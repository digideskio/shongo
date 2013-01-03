package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.executor.Compartment;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.executor.ExecutableManager;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.List;

/**
 * Manager for {@link Reservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationManager extends AbstractManager
{
    /**
     * @param entityManager sets the {@link #entityManager}
     */
    public ReservationManager(EntityManager entityManager)
    {
        super(entityManager);
    }

    /**
     * @param reservation to be created in the database
     */
    public void create(Reservation reservation)
    {
        super.create(reservation);
    }

    /**
     * @param reservation to be updated in the database
     */
    public void update(Reservation reservation)
    {
        super.update(reservation);
    }

    /**
     * @param reservation to be deleted in the database
     */
    public void delete(Reservation reservation, Cache cache)
    {
        Executable executable = reservation.getExecutable();
        if (executable != null) {
            ExecutableManager executableManager = new ExecutableManager(entityManager);
            if (executable.getState().equals(Compartment.State.STARTED)) {
                if (executable.getSlotEnd().isAfter(DateTime.now())) {
                    DateTime newSlotEnd = DateTime.now().withField(DateTimeFieldType.millisOfSecond(), 0);
                    if (newSlotEnd.isBefore(executable.getSlotStart())) {
                        newSlotEnd = executable.getSlotStart();
                    }
                    executable.setSlotEnd(newSlotEnd);
                    executableManager.update(executable);
                }
            }
        }
        // Remove reservation from cache
        cache.removeReservation(reservation);
        // Remove also all child reservations
        List<Reservation> childReservations = reservation.getChildReservations();
        for (Reservation childReservation : childReservations) {
            cache.removeReservation(childReservation);
        }
        super.delete(reservation);
    }

    /**
     * @param reservationId of the {@link Reservation}
     * @return {@link Reservation} with given id
     * @throws EntityNotFoundException when the {@link Reservation} doesn't exist
     */
    public Reservation get(Long reservationId) throws EntityNotFoundException
    {
        try {
            Reservation reservation = entityManager.createQuery(
                    "SELECT reservation FROM Reservation reservation"
                            + " WHERE reservation.id = :id",
                    Reservation.class).setParameter("id", reservationId)
                    .getSingleResult();
            return reservation;
        } catch (NoResultException exception) {
            throw new EntityNotFoundException(Reservation.class, reservationId);
        }
    }

    /**
     * @param reservationRequest for which the {@link Reservation} should be returned
     * @return {@link Reservation} for the given {@link ReservationRequest} or null if doesn't exists
     */
    public Reservation getByReservationRequest(ReservationRequest reservationRequest)
    {
        return getByReservationRequest(reservationRequest.getId());
    }

    /**
     * @param reservationRequestId of the {@link ReservationRequest} for which the {@link Reservation} should be returned
     * @return {@link Reservation} for the given {@link ReservationRequest} or null if doesn't exists
     */
    public Reservation getByReservationRequest(Long reservationRequestId)
    {
        try {
            Reservation reservation = entityManager.createQuery(
                    "SELECT reservation FROM Reservation reservation WHERE reservation.id IN("
                            + " SELECT reservationRequest.reservation.id FROM ReservationRequest reservationRequest"
                            + " WHERE reservationRequest.id = :id)", Reservation.class)
                    .setParameter("id", reservationRequestId)
                    .getSingleResult();
            return reservation;
        } catch (NoResultException exception) {
            return null;
        }
    }

    /**
     * @return list of {@link Reservation}s
     */
    public List<Reservation> list()
    {
        List<Reservation> reservations = entityManager.createQuery(
                "SELECT reservation FROM Reservation reservation", Reservation.class)
                .getResultList();
        return reservations;
    }

    /**
     * @param reservationRequest from which the {@link Reservation}s should be
     *                           returned.
     * @return list of {@link Reservation}s from given {@code reservationRequest}
     */
    public List<Reservation> listByReservationRequest(AbstractReservationRequest reservationRequest)
    {
        return listByReservationRequest(reservationRequest.getId());
    }

    /**
     * @param reservationRequestId for {@link AbstractReservationRequest} from which the {@link Reservation}s should be
     *                             returned.
     * @return list of {@link Reservation}s from {@link AbstractReservationRequest} with
     *         given {@code reservationRequestId}
     */
    public List<Reservation> listByReservationRequest(Long reservationRequestId)
    {
        List<Reservation> reservations = entityManager.createQuery(
                "SELECT reservation FROM Reservation reservation"
                        + " WHERE reservation IN ("
                        + "   SELECT reservationRequest.reservation FROM ReservationRequestSet reservationRequestSet"
                        + "   LEFT JOIN reservationRequestSet.reservationRequests reservationRequest"
                        + "   WHERE reservationRequestSet.id = :id"
                        + " ) OR reservation IN ("
                        + "   SELECT reservationRequest.reservation FROM ReservationRequest reservationRequest"
                        + "   WHERE reservationRequest.id = :id"
                        + " ) OR reservation IN ("
                        + "   SELECT reservation FROM PermanentReservationRequest reservationRequest"
                        + "   LEFT JOIN reservationRequest.resourceReservations reservation"
                        + "   WHERE reservationRequest.id = :id"
                        + " ) ",
                Reservation.class)
                .setParameter("id", reservationRequestId)
                .getResultList();
        return reservations;
    }

    /**
     * @param interval        in which the requested {@link Reservation}s should start
     * @param reservationType type of requested {@link Reservation}s
     * @return list of {@link Reservation}s starting in given {@code interval}
     */
    public <R extends Reservation> List<R> listByInterval(Interval interval, Class<R> reservationType)
    {
        List<R> reservations = entityManager.createQuery(
                "SELECT reservation FROM " + reservationType.getSimpleName() + " reservation"
                        + " WHERE reservation.slotStart BETWEEN :start AND :end",
                reservationType)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return reservations;
    }

    /**
     * Get list of reused {@link Reservation}s. Reused {@link Reservation} is a {@link Reservation} which is referenced
     * by at least one {@link ExistingReservation} in the {@link ExistingReservation#reservation} attribute.
     *
     * @return list of reused {@link Reservation}.
     */
    public List<Reservation> getReusedReservations()
    {
        List<Reservation> reservations = entityManager.createQuery(
                "SELECT DISTINCT reservation.reservation FROM ExistingReservation reservation", Reservation.class)
                .getResultList();
        return reservations;
    }

    /**
     * Get list of {@link ExistingReservation} which reuse the given {@code reusedReservation}.
     *
     * @param reusedReservation which must be referenced in the {@link ExistingReservation#reservation}
     * @return list of {@link ExistingReservation} which reuse the given {@code reusedReservation}
     */
    public List<ExistingReservation> getExistingReservations(Reservation reusedReservation)
    {
        List<ExistingReservation> reservations = entityManager.createQuery(
                "SELECT reservation FROM ExistingReservation reservation"
                        + " WHERE reservation.reservation = :reusedReservation",
                ExistingReservation.class)
                .setParameter("reusedReservation", reusedReservation)
                .getResultList();
        return reservations;
    }

    /**
     * Delete {@link Reservation}s which aren't allocated for any {@link ReservationRequest}.
     *
     * @return list of deleted {@link Reservation}
     */
    public List<Reservation> getReservationsForDeletion()
    {
        List<Reservation> reservations = entityManager.createQuery(
                "SELECT reservation FROM Reservation reservation"
                        + " WHERE reservation.createdBy = :createdBy"
                        + " AND reservation.parentReservation IS NULL"
                        + " AND reservation NOT IN("
                        + "   SELECT reservationRequest.reservation FROM ReservationRequest reservationRequest"
                        + "   WHERE reservationRequest.state = :state"
                        + " ) AND reservation NOT IN("
                        + "   SELECT reservation FROM PermanentReservationRequest reservationRequest"
                        + "   INNER JOIN reservationRequest.resourceReservations reservation"
                        + ")",
                Reservation.class)
                .setParameter("createdBy", Reservation.CreatedBy.CONTROLLER)
                .setParameter("state", ReservationRequest.State.ALLOCATED)
                .getResultList();
        return reservations;
    }

    /**
     * @param reservation to be checked if it is provided to any {@link ReservationRequest} or {@link Reservation}
     * @return true if given {@code reservation} is provided to any other {@link ReservationRequest},
     *         false otherwise
     */
    public boolean isProvided(Reservation reservation)
    {
        // Checks whether reservation isn't referenced in existing reservations
        List reservations = entityManager.createQuery(
                "SELECT reservation.id FROM ExistingReservation reservation"
                        + " WHERE reservation.reservation = :reservation")
                .setParameter("reservation", reservation)
                .getResultList();
        if (reservations.size() > 0) {
            return false;
        }
        // Checks whether reservation isn't referenced in existing reservation requests
        List reservationRequests = entityManager.createQuery(
                "SELECT reservationRequest.id FROM AbstractReservationRequest reservationRequest"
                        + " WHERE :reservation MEMBER OF reservationRequest.providedReservations")
                .setParameter("reservation", reservation)
                .getResultList();
        if (reservationRequests.size() > 0) {
            return false;
        }
        return true;
    }
}
