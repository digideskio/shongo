package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.controller.ControllerImplFaultSet;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.util.DatabaseFilter;
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.DateTime;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Manager for {@link Executable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutableManager extends AbstractManager
{
    /**
     * @param entityManager sets the {@link #entityManager}
     */
    public ExecutableManager(EntityManager entityManager)
    {
        super(entityManager);
    }

    /**
     * @param executable to be created in the database
     */
    public void create(Executable executable)
    {
        super.create(executable);
    }

    /**
     * @param executable to be updated in the database
     */
    public void update(Executable executable)
    {
        super.update(executable);
    }

    /**
     * @param executable to be deleted in the database
     */
    public void delete(Executable executable)
    {
        super.delete(executable);
    }

    /**
     * @param executableId of the {@link Executable}
     * @return {@link Executable} with given id
     * @throws FaultException when the {@link Executable} doesn't exist
     */
    public Executable get(Long executableId)
            throws FaultException
    {
        try {
            Executable executable = entityManager.createQuery(
                    "SELECT executable FROM Executable executable"
                            + " WHERE executable.id = :id AND executable.state != :notAllocated",
                    Executable.class)
                    .setParameter("id", executableId)
                    .setParameter("notAllocated", Executable.State.NOT_ALLOCATED)
                    .getSingleResult();
            return executable;
        }
        catch (NoResultException exception) {
            return ControllerImplFaultSet.throwEntityNotFoundFault(Executable.class, executableId);
        }
    }

    /**
     * @param ids    requested identifiers
     * @return list of all allocated {@link Executable}s
     */
    public List<Executable> list(Set<Long> ids)
    {
        DatabaseFilter filter = new DatabaseFilter("executable");
        filter.addIds(ids);
        TypedQuery<Executable> query = entityManager.createQuery("SELECT executable FROM Executable executable"
                + " WHERE executable.state != :notAllocated"
                + " AND executable NOT IN("
                + "    SELECT childExecutable FROM Executable executable "
                + "   INNER JOIN executable.childExecutables childExecutable"
                + " )"
                + " AND " + filter.toQueryWhere(),
                Executable.class);
        query.setParameter("notAllocated", Executable.State.NOT_ALLOCATED);
        filter.fillQueryParameters(query);
        List<Executable> executables = query.getResultList();
        return executables;
    }

    /**
     * @param states in which the {@link Executable}s must be
     * @return list of {@link Executable}s which are in one of given {@code states}
     */
    public List<Executable> list(Collection<Executable.State> states)
    {
        List<Executable> executables = entityManager.createQuery(
                "SELECT executable FROM Executable executable"
                        + " WHERE executable NOT IN("
                        + "   SELECT childExecutable FROM Executable executable "
                        + "   INNER JOIN executable.childExecutables childExecutable"
                        + " ) AND executable.state IN(:states)",
                Executable.class)
                .setParameter("states", states)
                .getResultList();
        return executables;
    }

    /**
     * @param states   in which the {@link Executable}s must be
     * @param dateTime in which the {@link Executable}s must take place
     * @return list of {@link Executable}s which are in one of given {@code states}
     *         and take place at given {@code dateTime}
     */
    public List<Executable> listTakingPlace(Collection<Executable.State> states, DateTime dateTime)
    {
        List<Executable> executables = entityManager.createQuery(
                "SELECT executable FROM Executable executable"
                        + " WHERE executable.state IN(:states)"
                        + "   AND executable.slotStart <= :dateTime AND executable.slotEnd >= :dateTime",
                Executable.class)
                .setParameter("states", states)
                .setParameter("dateTime", dateTime)
                .getResultList();
        return executables;
    }

    /**
     * @param states   in which the {@link Executable}s must be
     * @param dateTime in which the {@link Executable}s must not take place
     * @return list of {@link Executable}s which are in one of given {@code states}
     *         and don't take place at given {@code dateTime}
     */
    public List<Executable> listNotTakingPlace(Collection<Executable.State> states, DateTime dateTime)
    {
        List<Executable> executables = entityManager.createQuery(
                "SELECT executable FROM Executable executable"
                        + " WHERE executable.state IN(:states)"
                        + "   AND (executable.slotStart > :dateTime OR executable.slotEnd <= :dateTime)",
                Executable.class)
                .setParameter("states", states)
                .setParameter("dateTime", dateTime)
                .getResultList();
        return executables;
    }

    /**
     * Delete all {@link Executable}s which are not placed inside another {@link Executable} and not referenced by
     * any {@link Reservation} and which should be automatically
     * deleted ({@link Executable.State#NOT_ALLOCATED} or {@link Executable.State#NOT_STARTED}).
     */
    public void deleteAllNotReferenced()
    {
        List<Executable> executables = entityManager
                .createQuery("SELECT executable FROM Executable executable"
                        + " WHERE executable NOT IN("
                        + "   SELECT childExecutable FROM Executable executable "
                        + "   INNER JOIN executable.childExecutables childExecutable "
                        + " ) AND ("
                        + "       executable.state = :toDelete "
                        + "   OR ("
                        + "       executable.state = :notStarted "
                        + "       AND executable NOT IN (SELECT reservation.executable FROM Reservation reservation))"
                        + " )",
                        Executable.class)
                .setParameter("notStarted", Executable.State.NOT_STARTED)
                .setParameter("toDelete", Executable.State.TO_DELETE)
                .getResultList();
        for (Executable executable : executables) {
            delete(executable);
        }
    }

    /**
     * @param deviceResourceId
     * @param roomId
     * @param referenceDateTime
     * @return {@link RoomEndpoint} in given {@code deviceResourceId} with given {@code roomId}
     *         and taking place in given {@code referenceDateTime}
     */
    public RoomEndpoint getRoomEndpoint(Long deviceResourceId, String roomId, DateTime referenceDateTime)
    {
        ResourceRoomEndpoint resourceRoomEndpoint;
        try {
            resourceRoomEndpoint = entityManager.createQuery(
                    "SELECT room FROM ResourceRoomEndpoint room"
                            + " WHERE room.roomProviderCapability.resource.id = :resourceId"
                            + " AND room.roomId = :roomId"
                            + " AND room.slotStart <= :dateTime AND room.slotEnd > :dateTime",
                    ResourceRoomEndpoint.class)
                    .setParameter("resourceId", deviceResourceId)
                    .setParameter("roomId", roomId)
                    .setParameter("dateTime", referenceDateTime)
                    .getSingleResult();
        }
        catch (NoResultException exception) {
            return null;
        }
        List<UsedRoomEndpoint> usedRoomEndpoints = entityManager.createQuery(
                "SELECT room FROM UsedRoomEndpoint room"
                        + " WHERE room.roomEndpoint = :room"
                        + " AND room.slotStart <= :dateTime AND room.slotEnd > :dateTime", UsedRoomEndpoint.class)
                .setParameter("room", resourceRoomEndpoint)
                .setParameter("dateTime", referenceDateTime)
                .getResultList();
        if (usedRoomEndpoints.size() == 0) {
            return resourceRoomEndpoint;
        }
        if (usedRoomEndpoints.size() == 1) {
            return usedRoomEndpoints.get(0);
        }
        throw new IllegalStateException("Found multiple " + UsedRoomEndpoint.class.getSimpleName()
                + "s taking place at " + referenceDateTime.toString() + ".");
    }
}
