package cz.cesnet.shongo.controller.api;

import java.util.Map;

import static cz.cesnet.shongo.controller.api.ComplexType.Required;

/**
 * Interface to the service handling operations on reservations.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface ReservationService extends Service
{
    /**
     * Creates a new reservation request.
     * <p/>
     * The user with the given {@code token} will be the resource owner.
     *
     * @param token              token of the user requesting the operation
     * @param reservationRequest reservation request; should contains all atributes marked as {@link Required}
     *                           in {@link ReservationRequest}
     * @return the created reservation request identifier
     */
    public String createReservationRequest(SecurityToken token, ReservationRequest reservationRequest)
            throws FaultException;

    /**
     * Modifies a given reservation.
     *
     * @param token         token of the user requesting the operation
     * @param reservationId Shongo identifier of the reservation to modify
     * @param attributes    map of reservation attributes to change
     */
    public void modifyReservationRequest(SecurityToken token, String reservationId, Map attributes)
            throws FaultException;

    /**
     * Deletes a given reservation.
     *
     * @param token         token of the user requesting the operation
     * @param reservationId Shongo identifier of the reservation to modify
     */
    public void deleteReservationRequest(SecurityToken token, String reservationId) throws FaultException;

    /**
     * Gets the complete Reservation object.
     *
     * @param token                token of the user requesting the operation
     * @param reservationRequestId identifier of the reservation request to get
     */
    public ReservationRequest getReservationRequest(SecurityToken token, String reservationRequestId);

    /**
     * Lists all the time slots with assigned resources that were allocated by the scheduler for the reservation.
     *
     * @param token         token of the user requesting the operation
     * @param reservationId Shongo identifier of the reservation to get
     * @return
     */
    //public ReservationAllocation getReservationAllocation(SecurityToken token, String reservationId);

    /**
     * Lists resources allocated by a given reservation in a given time slot, matching a filter.
     *
     * @param token         token of the user requesting the operation
     * @param reservationId Shongo identifier of the reservation to get
     * @param slot
     * @param filter
     * @return
     */
    //public ResourceSummary[] listReservationResources(SecurityToken token, String reservationId, DateTimeSlot slot,
    //        Map filter);

    /**
     * Lists all the reservations matching a filter.
     *
     * @param token  token of the user requesting the operation
     * @param filter
     * @return
     */
    //public ReservationSummary[] listReservations(SecurityToken token, Map filter);

    /**
     * Looks up available time slots for a given reservation duration and resources.
     *
     * @param token
     * @param duration
     * @param resources
     * @param interDomain specification whether inter-domain lookup should be performed
     * @return
     */
    //public DateTimeSlot[] findReservationAvailableTime(SecurityToken token, Period duration, Resource[] resources,
    //        boolean interDomain);
}