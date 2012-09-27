package cz.cesnet.shongo.controller.scheduler;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.graph.JGraphSimpleLayout;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.CallInitiation;
import cz.cesnet.shongo.controller.cache.AvailableVirtualRoom;
import cz.cesnet.shongo.controller.compartment.*;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.*;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.CompartmentReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.VirtualRoomReservation;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.scheduler.report.*;
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
public class CompartmentReservationTask extends ReservationTask<CompartmentReservation>
{
    private static Logger logger = LoggerFactory.getLogger(CompartmentReservationTask.class);

    /**
     * {@see {@link CompartmentSpecification}}.
     */
    private CompartmentSpecification compartmentSpecification;

    /**
     * {@link Compartment} which is created by the {@link CompartmentReservationTask}.
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
            addEndpoint(endpointProvider.createEndpoint());
        }
        else {
            throw new IllegalArgumentException(String.format("%s is not supported by the %s.",
                    childSpecification.getClass().getSimpleName(), getClass().getSimpleName()));
        }
    }

    @Override
    public void addChildReservation(Reservation reservation)
    {
        super.addChildReservation(reservation);

        if (reservation instanceof EndpointProvider) {
            EndpointProvider endpointProvider = (EndpointProvider) reservation;
            addEndpoint(endpointProvider.createEndpoint());
        }
    }

    /**
     * @param endpoint to be added to the {@link #compartment}
     */
    private void addEndpoint(Endpoint endpoint)
    {
        compartment.addEndpoint(endpoint);

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
                if (!(endpointFrom instanceof VirtualRoom) && endpointTo instanceof VirtualRoom) {
                    Endpoint endpointTmp = endpointFrom;
                    endpointFrom = endpointTo;
                    endpointTo = endpointTmp;
                }
                break;
            case TERMINAL:
                // If the call should be initiated by a terminal and it is the second endpoint, exchange them
                if (endpointFrom instanceof VirtualRoom && !(endpointTo instanceof VirtualRoom)) {
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

        Report connection = new CreatingConnectionBetweenReport(endpointFrom, endpointTo, technology);
        try {
            addConnection(endpointFrom, endpointTo, technology);
        }
        catch (ReportException firstException) {
            connection.addChildMessage(firstException.getReport());
            try {
                addConnection(endpointTo, endpointFrom, technology);
            }
            catch (ReportException secondException) {
                connection.addChildMessage(secondException.getReport());
                Report connectionFailed = new CannotCreateConnectionBetweenReport(endpointFrom, endpointTo);
                connectionFailed.addChildMessage(connection);
                throw connectionFailed.exception();
            }
        }
        addReport(connection);
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
        // Created connection
        Connection connection = null;

        // TODO: implement connections to multiple endpoints
        if (endpointTo.getCount() > 1) {
            throw new CannotCreateConnectionFromToMultipleReport(endpointFrom, endpointTo).exception();
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
            ConnectionByAlias connectionByAlias = new ConnectionByAlias();
            connectionByAlias.setAlias(alias);
            connection = connectionByAlias;
        }
        // Create connection by address
        else if (technology.isAllowedConnectionByAddress() && endpointTo.getAddress() != null) {
            ConnectionByAddress connectionByAddress = new ConnectionByAddress();
            connectionByAddress.setAddress(endpointTo.getAddress());
            connection = connectionByAddress;
        }
        else {
            // Allocate alias for the target endpoint
            try {
                Resource resource = null;
                if (endpointTo instanceof ResourceEndpoint) {
                    ResourceEndpoint resourceEndpoint = (ResourceEndpoint) endpointTo;
                    resource = resourceEndpoint.getDeviceResource();
                }
                else if (endpointTo instanceof ResourceVirtualRoom) {
                    ResourceVirtualRoom resourceVirtualRoom = (ResourceVirtualRoom) endpointTo;
                    resource = resourceVirtualRoom.getDeviceResource();
                }
                AliasSpecification aliasSpecification = new AliasSpecification(technology, resource);
                AliasReservation aliasReservation = addChildReservation(aliasSpecification, AliasReservation.class);

                // Assign alias to endpoint
                endpointTo.addAlias(aliasReservation.getAlias());

                // Create connection by the created alias
                ConnectionByAlias connectionByAlias = new ConnectionByAlias();
                connectionByAlias.setAlias(aliasReservation.getAlias());
                connection = connectionByAlias;
            }
            catch (ReportException exception) {
                Report report = new CannotCreateConnectionFromToReport(endpointFrom, endpointTo);
                report.addChildMessage(exception.getReport());
                throw report.exception();
            }
        }

        if (connection == null) {
            throw new CannotCreateConnectionFromToReport(endpointFrom, endpointTo).exception();
        }

        connection.setEndpointFrom(endpointFrom);
        connection.setEndpointTo(endpointTo);
        compartment.addConnection(connection);
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
                if (!(endpointTo instanceof VirtualRoom) && callInitiationTo == CallInitiation.VIRTUAL_ROOM) {
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
     * @return plan if possible, null otherwise
     */
    private CompartmentReservation createNoVirtualRoomReservation() throws ReportException
    {
        List<Endpoint> endpoints = compartment.getEndpoints();
        // Maximal two endpoints may be connected without virtual room
        if (compartment.getTotalEndpointCount() > 2 || endpoints.size() > 2) {
            return null;
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
                return null;
            }

            // Check connectivity
            ConnectivityEdge connectivityEdge = connectivityGraph.getEdge(endpointFrom, endpointTo);
            if (connectivityEdge == null) {
                Endpoint endpointTemp = endpointFrom;
                endpointFrom = endpointTo;
                endpointTo = endpointTemp;
                connectivityEdge = connectivityGraph.getEdge(endpointFrom, endpointTo);
                if (connectivityEdge == null) {
                    return null;
                }
            }
        }
        else {
            // Only allocated resource is allowed
            Endpoint allocatedEndpoint = endpoints.get(0);
            if (!(allocatedEndpoint instanceof ResourceEndpoint)) {
                return null;
            }
        }

        // Create allocated compartment
        CompartmentReservation compartmentReservation = new CompartmentReservation();
        compartmentReservation.setSlot(getInterval());
        compartmentReservation.setCompartment(compartment);
        for (Reservation childReservation : getChildReservations()) {
            compartmentReservation.addChildReservation(childReservation);
        }
        // Add connection between two standalone endpoints
        if (endpointFrom != null && endpointTo != null) {
            addConnection(endpointFrom, endpointTo);
        }
        return compartmentReservation;
    }

    /**
     * @return collection of technology sets which interconnects all endpoints
     */
    private Collection<Set<Technology>> getSingleVirtualRoomPlanTechnologySets()
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
     * @return plan if possible, null otherwise
     */
    private CompartmentReservation createSingleVirtualRoomReservation() throws ReportException
    {
        Collection<Set<Technology>> technologySets = getSingleVirtualRoomPlanTechnologySets();

        // Get available virtual rooms
        List<AvailableVirtualRoom> availableVirtualRooms = getCache().findAvailableVirtualRoomsByVariants(
                getInterval(), compartment.getTotalEndpointCount(), technologySets);
        if (availableVirtualRooms.size() == 0) {

            return null;

        }
        // Sort virtual rooms from the most filled to the least filled
        Collections.sort(availableVirtualRooms, new Comparator<AvailableVirtualRoom>()
        {
            @Override
            public int compare(AvailableVirtualRoom first, AvailableVirtualRoom second)
            {
                return -Double.valueOf(first.getFullnessRatio()).compareTo(second.getFullnessRatio());
            }
        });
        // Get the first virtual room
        AvailableVirtualRoom availableVirtualRoom = availableVirtualRooms.get(0);

        // Create virtual room reservation
        VirtualRoomReservation virtualRoomReservation = new VirtualRoomReservation();
        virtualRoomReservation.setSlot(getInterval());
        virtualRoomReservation.setResource(availableVirtualRoom.getDeviceResource());
        virtualRoomReservation.setPortCount(compartment.getTotalEndpointCount());

        // Add virtual room to compartment
        VirtualRoom virtualRoom = new ResourceVirtualRoom(virtualRoomReservation);
        compartment.addVirtualRoom(virtualRoom);
        addReport(new AllocatingVirtualRoomReport(virtualRoom));
        for (Endpoint endpoint : compartment.getEndpoints()) {
            addConnection(virtualRoom, endpoint);
        }

        // Create compartment reservation
        CompartmentReservation compartmentReservation = new CompartmentReservation();
        compartmentReservation.setSlot(getInterval());
        compartmentReservation.setCompartment(compartment);
        compartmentReservation.addChildReservation(virtualRoomReservation);
        for (Reservation childReservation : getChildReservations()) {
            compartmentReservation.addChildReservation(childReservation);
        }

        return compartmentReservation;
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
    protected CompartmentReservation createReservation() throws ReportException
    {
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
            if (!personSpecifications.contains(endpointSpecification)) {
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
                if (endpoint instanceof ResourceEndpoint || endpoint instanceof ResourceVirtualRoom) {
                    resourceRequested = true;
                }
            }
            if (!resourceRequested) {
                throw new NotEnoughEndpointInCompartmentReport().exception();
            }
        }

        CompartmentReservation noVirtualRoomReservation = createNoVirtualRoomReservation();
        if (noVirtualRoomReservation != null) {
            return noVirtualRoomReservation;
        }

        CompartmentReservation singleVirtualRoomReservation = createSingleVirtualRoomReservation();
        if (singleVirtualRoomReservation != null) {
            return singleVirtualRoomReservation;
        }

        // TODO: Resolve multiple virtual rooms and/or gateways for connecting endpoints

        throw new NoAvailableVirtualRoomReport(getSingleVirtualRoomPlanTechnologySets(),
                compartment.getTotalEndpointCount()).exception();
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