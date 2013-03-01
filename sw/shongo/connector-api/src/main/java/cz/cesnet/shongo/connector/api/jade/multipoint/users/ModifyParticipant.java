package cz.cesnet.shongo.connector.api.jade.multipoint.users;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

import java.util.Map;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ModifyParticipant extends ConnectorCommand
{
    private String roomId;
    private String roomUserId;
    private Map<String, Object> attributes;

    public ModifyParticipant()
    {
    }

    public ModifyParticipant(String roomId, String roomUserId, Map<String, Object> attributes)
    {
        this.roomId = roomId;
        this.roomUserId = roomUserId;
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes()
    {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes)
    {
        this.attributes = attributes;
    }

    public String getRoomId()
    {
        return roomId;
    }

    public void setRoomId(String roomId)
    {
        this.roomId = roomId;
    }

    public String getRoomUserId()
    {
        return roomUserId;
    }

    public void setRoomUserId(String roomUserId)
    {
        this.roomUserId = roomUserId;
    }

    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Modifying participant {} in room {}", roomUserId, roomId);
        if (attributes != null) {
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                logger.info("  - setting attribute '{}' to '{}'", entry.getKey(), entry.getValue());
            }
        }

        getMultipoint(connector).modifyParticipant(roomId, roomUserId, attributes);
        return null;
    }

    @Override
    public String toString()
    {
        return String.format(ModifyParticipant.class.getSimpleName() + " (roomId: %s, roomUserId: %s)",
                roomId, roomUserId);
    }
}