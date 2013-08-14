package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.controller.api.AllocationState;
import cz.cesnet.shongo.controller.api.ExecutableState;
import cz.cesnet.shongo.controller.api.ReservationRequestType;

/**
 * Represents a reservation request state.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum ReservationRequestState
{
    /**
     * Reservation request has not been allocated by the scheduler yet.
     */
    NOT_ALLOCATED(false),

    /**
     * Reservation request is allocated by the scheduler but the allocated executable has not been started yet.
     */
    ALLOCATED(true),

    /**
     * Reservation request is allocated by the scheduler and the allocated executable is started.
     */
    STARTED(true),

    /**
     * Reservation request is allocated by the scheduler and the allocated executable has been started and stopped.
     */
    FINISHED(true),

    /**
     * Reservation request cannot be allocated by the scheduler or the starting of executable failed.
     */
    FAILED(false),

    /**
     * Modification of reservation request cannot be allocated by the scheduler
     * but some previous version of reservation request has been allocated and started.
     */
    MODIFICATION_FAILED(false);

    /**
     * Specifies whether reservation request is allocated.
     */
    private final boolean allocated;

    /**
     * Constructor.
     *
     * @param allocated sets the {@link #allocated}
     */
    private ReservationRequestState(boolean allocated)
    {
        this.allocated = allocated;
    }

    /**
     * @return {@link #allocated}
     */
    public boolean isAllocated()
    {
        return allocated;
    }

    /**
     * @param allocationState
     * @param executableState
     * @param reservationRequestType
     * @param lastReservationId
     * @return {@link ReservationRequestState}
     */
    public static ReservationRequestState fromApi(AllocationState allocationState, ExecutableState executableState,
            ReservationRequestType reservationRequestType, String lastReservationId)
    {
        switch (allocationState) {
            case ALLOCATED:
                switch (executableState) {
                    case STARTED:
                        return STARTED;
                    case STOPPED:
                    case STOPPING_FAILED:
                        return FINISHED;
                    case STARTING_FAILED:
                        return FAILED;
                    default:
                        return ALLOCATED;
                }
            case ALLOCATION_FAILED:
                if (reservationRequestType.equals(ReservationRequestType.MODIFIED) && lastReservationId != null) {
                    return MODIFICATION_FAILED;
                }
                else {
                    return FAILED;
                }
            default:
                return NOT_ALLOCATED;
        }
    }
}