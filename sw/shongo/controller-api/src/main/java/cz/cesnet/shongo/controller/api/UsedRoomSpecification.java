package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;

import java.util.LinkedList;
import java.util.List;

/**
 * {@link cz.cesnet.shongo.controller.api.Specification} for a one-time room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UsedRoomSpecification extends AbstractRoomSpecification
{
    /**
     * Shongo-id for reused {@link RoomExecutable} or {@link ReservationRequest} that allocates the {@link RoomExecutable}.
     */
    private String reusedRoomExecutableId;

    /**
     * Number of ports which must be allocated for the virtual room.
     */
    private Integer participantCount;

    /**
     * {@link ExecutableServiceSpecification}s for the virtual room.
     */
    private List<ExecutableServiceSpecification> serviceSpecifications = new LinkedList<ExecutableServiceSpecification>();

    /**
     * Constructor.
     */
    public UsedRoomSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param reusedRoomExecutableId sets the {@link #reusedRoomExecutableId}
     * @param participantCount sets the {@link #participantCount}
     */
    public UsedRoomSpecification(String reusedRoomExecutableId, int participantCount)
    {
        setReusedRoomExecutableId(reusedRoomExecutableId);
        setParticipantCount(participantCount);
    }

    /**
     * @return {@link #reusedRoomExecutableId}
     */
    public String getReusedRoomExecutableId()
    {
        return reusedRoomExecutableId;
    }

    /**
     * @param reusedRoomExecutableId sets the {@link #reusedRoomExecutableId}
     */
    public void setReusedRoomExecutableId(String reusedRoomExecutableId)
    {
        this.reusedRoomExecutableId = reusedRoomExecutableId;
    }

    /**
     * @return {@link #participantCount}
     */
    public Integer getParticipantCount()
    {
        return participantCount;
    }

    /**
     * @param participantCount sets the {@link #participantCount}
     */
    public void setParticipantCount(Integer participantCount)
    {
        this.participantCount = participantCount;
    }

    /**
     * @return {@link #serviceSpecifications}
     */
    public List<ExecutableServiceSpecification> getServiceSpecifications()
    {
        return serviceSpecifications;
    }

    /**
     * @param serviceSpecification to be added to the {@link #serviceSpecifications}
     */
    public void addServiceSpecification(ExecutableServiceSpecification serviceSpecification)
    {
        serviceSpecifications.add(serviceSpecification);
    }

    public static final String REUSED_ROOM_EXECUTABLE_ID = "reusedRoomExecutableId";
    public static final String PARTICIPANT_COUNT = "participantCount";
    public static final String SERVICE_SPECIFICATIONS = "serviceSpecifications";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(REUSED_ROOM_EXECUTABLE_ID, reusedRoomExecutableId);
        dataMap.set(PARTICIPANT_COUNT, participantCount);
        dataMap.set(SERVICE_SPECIFICATIONS, serviceSpecifications);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        reusedRoomExecutableId = dataMap.getStringRequired(REUSED_ROOM_EXECUTABLE_ID);
        participantCount = dataMap.getIntegerRequired(PARTICIPANT_COUNT);
        serviceSpecifications = dataMap.getList(SERVICE_SPECIFICATIONS, ExecutableServiceSpecification.class);
    }
}