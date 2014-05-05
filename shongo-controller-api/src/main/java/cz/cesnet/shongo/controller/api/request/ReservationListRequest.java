package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.api.Reservation;
import cz.cesnet.shongo.controller.api.ReservationSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;

import java.util.*;

/**
 * {@link ListRequest} for reservations.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationListRequest extends SortableListRequest<ReservationListRequest.Sort>
{
    /**
     * {@link ReservationSummary.Type}s which should be returned.
     */
    private Set<ReservationSummary.Type> reservationTypes = new HashSet<ReservationSummary.Type>();

    /**
     * Resource-id of resources which must be allocated by returned {@link Reservation}s.
     */
    private String resourceId;

    /**
     * Constructor.
     */
    public ReservationListRequest()
    {
        super(Sort.class);
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     */
    public ReservationListRequest(SecurityToken securityToken)
    {
        super(Sort.class, securityToken);
    }

    /**
     * @return {@link #reservationTypes}
     */
    public Set<ReservationSummary.Type> getReservationTypes()
    {
        return Collections.unmodifiableSet(reservationTypes);
    }

    /**
     * @param reservationType to be added to the {@link #reservationTypes}
     */
    public void addReservationClass(ReservationSummary.Type reservationType)
    {
        this.reservationTypes.add(reservationType);
    }

    /**
     * @return {@link #resourceId}
     */
    public String getResourceId()
    {
        return resourceId;
    }

    /**
     * @param resourceId sets the {@link #resourceId}
     */
    public void setResourceId(String resourceId)
    {
        this.resourceId = resourceId;
    }

    /**
     * Field by which the result should be sorted.
     */
    public static enum Sort
    {
        SLOT
    }

    private static final String RESERVATION_TYPES = "reservationTypes";
    private static final String RESOURCE_ID = "resourceId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RESERVATION_TYPES, reservationTypes);
        dataMap.set(RESOURCE_ID, resourceId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        reservationTypes = (Set) dataMap.getSet(RESERVATION_TYPES, Class.class);
        resourceId = dataMap.getString(RESOURCE_ID);
    }
}