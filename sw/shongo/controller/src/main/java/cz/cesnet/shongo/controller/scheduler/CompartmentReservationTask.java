package cz.cesnet.shongo.controller.scheduler;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.graph.JGraphSimpleLayout;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.CallInitiation;
import cz.cesnet.shongo.controller.cache.CacheTransaction;
import cz.cesnet.shongo.controller.executor.*;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.CompartmentSpecification;
import cz.cesnet.shongo.controller.request.EndpointSpecification;
import cz.cesnet.shongo.controller.request.PersonSpecification;
import cz.cesnet.shongo.controller.request.Specification;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.RoomReservation;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.scheduler.report.*;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.jgraph.JGraph;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.SimpleGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

/**
 * Represents {@link ReservationTask} for a {@link CompartmentSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CompartmentReservationTask extends ReservationTask
{
    private static Logger logger = LoggerFactory.getLogger(CompartmentReservationTask.class);

    /**
     * {@see {@link CompartmentSpecification}}.
     */
    private CompartmentSpecification compartmentSpecification;

    /**
     * {@link cz.cesnet.shongo.controller.executor.Compartment} which is created by the {@link CompartmentReservationTask}.
     */
    private Compartment compartment = new Compartment();

    /**
     * Graph of connectivity between endpoints.
     */
    private UndirectedGraph<Endpoint, ConnectivityEdge> connectivityGraph =
            new SimpleGraph<Endpoint, ConnectivityEdge>(ConnectivityEdge.class);

    /**
     * Constructor.
     *
     * @param context sets the {@link #context}
     */
    public CompartmentReservationTask(Context context)
    {
        super(context);
        this.compartmentSpecification = new CompartmentSpecification();
        initCompartment();
    }

    /**
     * Constructor.
     *
     * @param context        sets the {@link #context}
     * @param callInitiation sets the default {@link cz.cesnet.shongo.controller.CallInitiation}
     */
    public CompartmentReservationTask(Context context, CallInitiation callInitiation)
    {
        super(context);
        this.compartmentSpecification = new CompartmentSpecification(callInitiation);
        initCompartment();
    }

    /**
     * Constructor.
     *
     * @param specification sets the {@link #compartmentSpecification}
     * @param context       sets the {@link #context}
     */
    public CompartmentReservationTask(CompartmentSpecification specification, Context context)
    {
        super(context);
        this.compartmentSpecification = specification;
        initCompartment();
    }

    /**
     * Initialize compartment
     */
    private void initCompartment()
    {
        // Initialize compartment
        compartment.setSlot(getInterval());
    }

    /**
     * @return default {@link CallInitiation}
     */
    public CallInitiation getCallInitiation()
    {
        CallInitiation callInitiation = compartmentSpecification.getCallInitiation();
        if (callInitiation == null) {
            callInitiation = CallInitiation.TERMINAL;
        }
        return callInitiation;
    }

    /**
     * Add child {@link Specification}.
     *
     * @param childSpecification
     * @throws ReportException
     */
    public void addChildSpecification(Specification childSpecification) throws ReportException
    {
        if (childSpecification instanceof ReservationTaskProvider) {
            ReservationTaskProvider reservationTaskProvider = (ReservationTaskProvider) childSpecification;
            addChildReservation(reservationTaskProvider);
        }
        else if (childSpecification instanceof EndpointProvider) {
            EndpointProvider endpointProvider = (EndpointProvider) childSpecification;
            addEndpoint(endpointProvider.getEndpoint());
        }
        else {
            throw new IllegalArgumentException(String.format("%s is not supported by the %s.",
                    childSpecification.getClass().getSimpleName(), getClass().getSimpleName()));
        }
    }

    @Override
    public Reservation addChildReservation(Reservation reservation)
    {
        reservation = super.addChildReservation(reservation);
        if (reservation instanceof EndpointProvider) {
            EndpointProvider endpointProvider = (EndpointProvider) reservation;
            addEndpoint(endpointProvider.getEndpoint());
        }
        return reservation;
    }

    /**
     * @param reservation child {@link cz.cesnet.shongo.controller.reservation.RoomReservation} to be added to the {@link CompartmentReservationTask}
     * @return allocated {@link cz.cesnet.shongo.controller.executor.RoomEndpoint}
     */
    public RoomEndpoint addChildRoomReservation(Reservation reservation)
    {
        addChildReservation(reservation);
        RoomReservation roomReservation = reservation.getTargetReservation(RoomReservation.class);
        return roomReservation.getEndpoint();
    }

    /**
     * @param endpoint to be added to the {@link #compartment}
     */
    private void addEndpoint(Endpoint endpoint)
    {
        endpoint.setSlot(getContext().getInterval());
        compartment.addChildExecutable(endpoint);

        if (!(endpoint instanceof RoomEndpoint)) {
            // Setup connectivity graph
            connectivityGraph.addVertex(endpoint);
            for (Endpoint existingEndpoint : connectivityGraph.vertexSet()) {
                if (existingEndpoint == endpoint) {
                    continue;
                }
                Set<Technology> technologies = new HashSet<Technology>(endpoint.getTechnologies());
                technologies.retainAll(existingEndpoint.getTechnologies());
                if (technologies.size() > 0) {
                    connectivityGraph.addEdge(endpoint, existingEndpoint, new ConnectivityEdge(technologies));
                }
            }
        }
    }

    /**
     * Add new connection to the {@link #compartment}.
     *
     * @param endpointFrom
     * @param endpointTo
     */
    private void addConnection(Endpoint endpointFrom, Endpoint endpointTo) throws ReportException
    {
        // Determine call initiation from given endpoints
        CallInitiation callInitiation = determineCallInitiation(endpointFrom, endpointTo);

        // Change preferred order of endpoints based on call initiation
        switch (callInitiation) {
            case VIRTUAL_ROOM:
                // If the call should be initiated by a virtual room and it is the second endpoint, exchange them
                if (!(endpointFrom instanceof RoomEndpoint) && endpointTo instanceof RoomEndpoint) {
                    Endpoint endpointTmp = endpointFrom;
                    endpointFrom = endpointTo;
                    endpointTo = endpointTmp;
                }
                break;
            case TERMINAL:
                // If the call should be initiated by a terminal and it is the second endpoint, exchange them
                if (endpointFrom instanceof RoomEndpoint && !(endpointTo instanceof RoomEndpoint)) {
                    Endpoint endpointTmp = endpointFrom;
                    endpointFrom = endpointTo;
                    endpointTo = endpointTmp;
                }
                break;
            default:
                throw new IllegalStateException("Unknown call initiation '" + callInitiation.toString() + "'.");
        }

        // Determine technology by which the resources will connect
        Technology technology = null;
        Set<Technology> technologies = new HashSet<Technology>(endpointFrom.getTechnologies());
        technologies.retainAll(endpointTo.getTechnologies());
        switch (technologies.size()) {
            case 0:
                // No common technology
                throw new IllegalArgumentException(
                        "Cannot connect endpoints because they doesn't have any common technology!");
            case 1:
                // One common technology
                technology = technologies.iterator().next();
                break;
            default:
                // Multiple common technologies, thus determine preferred technology
                Technology preferredTechnology = null;
                if (endpointFrom instanceof ResourceEndpoint) {
                    ResourceEndpoint deviceResourceEndpoint = (ResourceEndpoint) endpointFrom;
                    preferredTechnology = deviceResourceEndpoint.getDeviceResource().getPreferredTechnology();
                }
                if (preferredTechnology == null && endpointTo instanceof ResourceEndpoint) {
                    ResourceEndpoint deviceResourceEndpoint = (ResourceEndpoint) endpointTo;
                    preferredTechnology = deviceResourceEndpoint.getDeviceResource().getPreferredTechnology();
                }
                // Use preferred technology
                if (technologies.contains(preferredTechnology)) {
                    technology = preferredTechnology;
                }
                else {
                    technology = technologies.iterator().next();
                }
        }

        beginReport(new AllocatingConnectionBetweenReport(endpointFrom, endpointTo, technology), true);
        CacheTransaction.Savepoint cacheTransactionSavepoint = getContext().getCacheTransaction().createSavepoint();
        try {
            addConnection(endpointFrom, endpointTo, technology);
        }
        catch (ReportException firstException) {
            cacheTransactionSavepoint.revert();
            try {
                addConnection(endpointTo, endpointFrom, technology);
            }
            catch (ReportException secondException) {
                throw createReportFailureForThrowing().exception();
            }
        }
        finally {
            cacheTransactionSavepoint.destroy();
            endReport();
        }
    }

    /**
     * Add new connection to the given {@code reservation}.
     *
     * @param endpointFrom
     * @param endpointTo
     * @param technology
     * @throws ReportException
     */
    private void addConnection(Endpoint endpointFrom, Endpoint endpointTo, Technology technology)
            throws ReportException
    {
        // Allocate alias for the target endpoint
        beginReport(new AllocatingConnectionFromToReport(endpointFrom, endpointTo), true);
        try {
            // Created connection
            Connection connection = null;

            // TODO: implement connections to multiple endpoints
            if (endpointTo.getCount() > 1) {
                throwNewReportFailure(new CannotCreateConnectionToMultipleReport(endpointFrom, endpointTo));
            }

            // Find existing alias for connection
            Alias alias = null;
            List<Alias> aliases = endpointTo.getAliases();
            for (Alias possibleAlias : aliases) {
                if (possibleAlias.getTechnology().equals(technology)) {
                    alias = possibleAlias;
                    break;
                }
            }
            // Create connection by alias
            if (alias != null) {
                connection = new Connection();
                connection.setAlias(alias.clone());
            }
            else {
                DeviceResource deviceResource = null;
                if (endpointTo instanceof ResourceEndpoint) {
                    ResourceEndpoint resourceEndpoint = (ResourceEndpoint) endpointTo;
                    deviceResource = resourceEndpoint.getDeviceResource();
                }
                else if (endpointTo instanceof ResourceRoomEndpoint) {
                    ResourceRoomEndpoint resourceRoomEndpoint = (ResourceRoomEndpoint) endpointTo;
                    deviceResource = resourceRoomEndpoint.getDeviceResource();
                }
                AliasReservationTask aliasReservationTask = new AliasReservationTask(getContext());
                aliasReservationTask.addTechnology(technology);
                aliasReservationTask.setTargetResource(deviceResource);
                AliasReservation aliasReservation = addChildReservation(aliasReservationTask, AliasReservation.class);

                // Assign all usable aliases to endpoint, and find the one which will be used for the connection
                alias = null;
                Set<Technology> endpointToTechnologies = endpointTo.getTechnologies();
                for (Alias possibleAlias : aliasReservation.getAliases()) {
                    if (endpointToTechnologies.contains(possibleAlias.getTechnology())) {
                        endpointTo.addAssignedAlias(possibleAlias);
                        if (possibleAlias.getTechnology().equals(technology)) {
                            alias = possibleAlias;
                        }
                    }
                }
                if (alias == null) {
                    throw new IllegalStateException(
                            "Alias reservation doesn't contain alias for requested technology (should never happen).");
                }

                // Create connection by the created alias
                connection = new Connection();
                connection.setAlias(alias.clone());
            }

            connection.setSlot(getContext().getInterval());
            connection.setEndpointFrom(endpointFrom);
            connection.setEndpointTo(endpointTo);
            compartment.addChildExecutable(connection);
        }
        finally {
            endReport();
        }
    }

    /**
     * @param endpointFrom first {@link Endpoint}
     * @param endpointTo   second {@link Endpoint}
     * @return {@link CallInitiation} from given {@link Endpoint}s
     */

    private CallInitiation determineCallInitiation(Endpoint endpointFrom, Endpoint endpointTo)
    {
        CallInitiation callInitiation = null;
        CallInitiation callInitiationFrom = endpointFrom.getCallInitiation();
        CallInitiation callInitiationTo = endpointTo.getCallInitiation();
        if (callInitiationFrom != null) {
            callInitiation = callInitiationFrom;
        }
        if (callInitiationTo != null) {
            if (callInitiation == null) {
                callInitiation = callInitiationTo;
            }
            else if (callInitiation != callInitiationTo) {
                // Rewrite call initiation only when the second endpoint isn't virtual room and it want to be called
                // from the virtual room
                if (!(endpointTo instanceof RoomEndpoint) && callInitiationTo == CallInitiation.VIRTUAL_ROOM) {
                    callInitiation = callInitiationTo;
                }
            }
        }
        // If no call initiation was specified for the endpoints, use the default
        if (callInitiation == null) {
            callInitiation = this.getCallInitiation();
        }
        return callInitiation;
    }

    /**
     * Find plan for connecting endpoints without virtual room.
     *
     * @return true if succeeds, otherwise false
     */
    private boolean createNoRoomReservation() throws ReportException
    {
        List<Endpoint> endpoints = compartment.getEndpoints();
        // Maximal two endpoints may be connected without virtual room
        if (compartment.getTotalEndpointCount() > 2 || endpoints.size() > 2) {
            return false;
        }

        // Two endpoints must be standalone and interconnectable
        Endpoint endpointFrom = null;
        Endpoint endpointTo = null;
        if (endpoints.size() == 2) {
            if (compartment.getTotalEndpointCount() != 2) {
                throw new IllegalStateException();
            }
            endpointFrom = endpoints.get(0);
            endpointTo = endpoints.get(1);

            // Check if endpoints are standalone
            if (!endpointFrom.isStandalone() || !endpointTo.isStandalone()) {
                return false;
            }

            // Check connectivity
            ConnectivityEdge connectivityEdge = connectivityGraph.getEdge(endpointFrom, endpointTo);
            if (connectivityEdge == null) {
                Endpoint endpointTemp = endpointFrom;
                endpointFrom = endpointTo;
                endpointTo = endpointTemp;
                connectivityEdge = connectivityGraph.getEdge(endpointFrom, endpointTo);
                if (connectivityEdge == null) {
                    return false;
                }
            }
        }
        else {
            // Only allocated resource is allowed
            Endpoint allocatedEndpoint = endpoints.get(0);
            if (!(allocatedEndpoint instanceof ResourceEndpoint)) {
                return false;
            }
        }

        // Add connection between two standalone endpoints
        if (endpointFrom != null && endpointTo != null) {
            addConnection(endpointFrom, endpointTo);
        }
        return true;
    }

    /**
     * @return collection of technology sets which interconnects all endpoints
     */
    private Collection<Set<Technology>> getSingleRoomTechnologySets()
    {
        List<Set<Technology>> technologiesList = new ArrayList<Set<Technology>>();
        for (Endpoint endpoint : compartment.getEndpoints()) {
            technologiesList.add(endpoint.getTechnologies());
        }
        return Technology.interconnect(technologiesList);
    }

    /**
     * Find plan for connecting endpoints by a single virtual room
     *
     * @throws ReportException when the {@link cz.cesnet.shongo.controller.reservation.RoomReservation} cannot be created
     */
    private void createSingleRoomReservation() throws ReportException
    {
        RoomReservationTask roomReservationTask = new RoomReservationTask(getContext(),
                compartment.getTotalEndpointCount());
        for (Set<Technology> technologies : getSingleRoomTechnologySets()) {
            roomReservationTask.addTechnologyVariant(technologies);
        }
        Reservation reservation = roomReservationTask.perform();
        addReports(roomReservationTask);
        RoomEndpoint roomEndpoint = addChildRoomReservation(reservation);
        for (Endpoint endpoint : compartment.getEndpoints()) {
            addConnection(roomEndpoint, endpoint);
        }
    }

    /**
     * Show current connectivity graph in dialog
     */
    public void showConnectivityGraph()
    {
        JGraph graph = new JGraph(new JGraphModelAdapter<Endpoint, ConnectivityEdge>(connectivityGraph));

        JGraphFacade graphFacade = new JGraphFacade(graph, graph.getSelectionCells());
        graphFacade.setIgnoresUnconnectedCells(true);
        graphFacade.setIgnoresCellsInGroups(true);
        graphFacade.setIgnoresHiddenCells(true);
        graphFacade.setDirected(false);
        graphFacade.resetControlPoints();

        JGraphSimpleLayout graphLayout = new JGraphSimpleLayout(JGraphSimpleLayout.TYPE_CIRCLE);
        graphLayout.run(graphFacade);

        Dimension dimension = new Dimension(graphLayout.getMaxx(), graphLayout.getMaxy());
        Rectangle2D bounds = graphFacade.getCellBounds();
        dimension.setSize(bounds.getWidth(), bounds.getHeight());
        dimension.setSize(dimension.getWidth() + 50, dimension.getHeight() + 80);

        Map nested = graphFacade.createNestedMap(true, true);
        graph.getGraphLayoutCache().edit(nested);

        JDialog dialog = new JDialog();
        dialog.getContentPane().add(graph);
        dialog.setSize(dimension);
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    @Override
    protected Report createdMainReport()
    {
        return new AllocatingCompartmentReport();
    }

    @Override
    protected Reservation createReservation() throws ReportException
    {
        if (!getContext().isExecutableAllowed()) {
            throw new TodoImplementException("Allocating compartment without executable (does it make sense?).");
        }
        Set<Specification> specifications = new HashSet<Specification>();
        List<PersonSpecification> personSpecifications = new ArrayList<PersonSpecification>();
        for (Specification specification : compartmentSpecification.getReadySpecifications()) {
            if (specification instanceof PersonSpecification) {
                personSpecifications.add((PersonSpecification) specification);
            }
            else {
                specifications.add(specification);
            }
        }
        for (PersonSpecification personSpecification : personSpecifications) {
            EndpointSpecification endpointSpecification = personSpecification.getEndpointSpecification();
            if (!specifications.contains(endpointSpecification)) {
                specifications.add(endpointSpecification);
            }
            // TODO: Add persons for allocated devices
        }
        for (Specification specification : specifications) {
            addChildSpecification(specification);
        }

        if (compartment.getTotalEndpointCount() <= 1) {
            // Check whether an existing resource is requested
            boolean resourceRequested = false;
            for (Endpoint endpoint : compartment.getEndpoints()) {
                if (endpoint instanceof ResourceEndpoint || endpoint instanceof ResourceRoomEndpoint) {
                    resourceRequested = true;
                }
            }
            if (!resourceRequested) {
                throw new NotEnoughEndpointInCompartmentReport().exception();
            }
        }

        if (!createNoRoomReservation()) {
            try {
                createSingleRoomReservation();
            }
            catch (ReportException exception) {
                // TODO: Resolve multiple virtual rooms and/or gateways for connecting endpoints
                throw exception;
            }
        }

        // Initialize allocated compartment
        compartment.setState(Compartment.State.NOT_STARTED);

        // Create compartment reservation for allocated compartment
        Reservation compartmentReservation = new Reservation();
        compartmentReservation.setSlot(getInterval());
        compartmentReservation.setExecutable(compartment);
        return compartmentReservation;
    }

    /**
     * Represents an edge in the connectivity graph of endpoints.
     */
    private static class ConnectivityEdge
    {
        /**
         * Technologies by which two endpoints can be connected.
         */
        private Set<Technology> technologies;

        /**
         * @param technologies sets the {@link #technologies}
         */
        public ConnectivityEdge(Set<Technology> technologies)
        {
            this.technologies = technologies;
        }

        /**
         * @return {@link #technologies}
         */
        public Set<Technology> getTechnologies()
        {
            return technologies;
        }

        @Override
        public String toString()
        {
            return Technology.formatTechnologies(technologies);
        }
    }
}
