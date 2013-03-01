package cz.cesnet.shongo.connector.api.jade.multipoint.rooms;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ModifyRoom extends ConnectorCommand
{
    private Room room;

    public ModifyRoom()
    {
    }

    public ModifyRoom(Room room)
    {
        this.room = room;
    }

    public Room getRoom()
    {
        return room;
    }

    public void setRoom(Room room)
    {
        this.room = room;
    }

    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Modifying room {}", room);
        return getMultipoint(connector).modifyRoom(room);
    }

    public String toString()
    {
        return String.format(ModifyRoom.class.getSimpleName() + " (room: %s)", room.toString());
    }
}