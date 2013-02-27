package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.connector.api.jade.ConnectorOntology;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.ModifyRoom;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlService;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlServiceImpl;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import cz.cesnet.shongo.jade.Agent;
import jade.core.AID;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for serializing API classes for JADE.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class JadeTest extends AbstractControllerTest
{
    private static Logger logger = LoggerFactory.getLogger(JadeTest.class);

    /**
     * @return {@link cz.cesnet.shongo.controller.api.rpc.ResourceControlService} from the {@link #controllerClient}
     */
    public ResourceControlService getResourceControlService()
    {
        return getControllerClient().getService(ResourceControlService.class);
    }

    @Override
    protected void onInit()
    {
        super.onInit();

        getController().addRpcService(new ResourceControlServiceImpl()
        {
            @Override
            public Room getRoom(SecurityToken token, String deviceResourceId, String roomId)
                    throws FaultException
            {
                Assert.assertEquals("1", roomId);
                Room room = new Room();
                room.setId("1");
                room.setName("room");
                room.setLicenseCount(5);
                room.addAlias(new Alias(AliasType.H323_E164, "9501"));
                return room;
            }
        });
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        getController().startJade();
    }

    /**
     * Test serialization of {@link Room} in {@link ResourceControlService#modifyRoom(SecurityToken, String, Room)}.
     *
     * @throws Exception
     */
    @Test
    public void testCreateRoom() throws Exception
    {
        ConnectorAgent mcuAgent = getController().addJadeAgent("mcu", new ConnectorAgent());
        getController().waitForJadeAgentsToStart();

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        Room room = new Room();
        room.setName("room");
        room.setOption(Room.Option.PIN, "1234");
        room.addAlias(new Alias(AliasType.ROOM_NAME, "test"));
        getResourceControlService().createRoom(SECURITY_TOKEN, mcuId, room);
    }

    /**
     * Test serialization of {@link Room} in {@link ResourceControlService#modifyRoom(SecurityToken, String, Room)}.
     *
     * @throws Exception
     */
    @Test
    public void testModifyRoom() throws Exception
    {
        ConnectorAgent mcuAgent = getController().addJadeAgent("mcu", new ConnectorAgent());
        getController().waitForJadeAgentsToStart();

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        Room room = getResourceControlService().getRoom(SECURITY_TOKEN, mcuId, "1");
        room.setName("room");
        room.setOption(Room.Option.PIN, "1234");
        getResourceControlService().modifyRoom(SECURITY_TOKEN, mcuId, room);
    }

    /**
     * Testing connector agent.
     */
    public class ConnectorAgent extends Agent
    {
        @Override
        protected void setup()
        {
            addOntology(ConnectorOntology.getInstance());
            super.setup();
        }

        @Override
        public Object handleCommand(Command command, AID sender) throws CommandException, CommandUnsupportedException
        {
            if (command instanceof CreateRoom) {
                CreateRoom createRoom = (CreateRoom) command;
                Room room = createRoom.getRoom();
                try {
                    room.setupNewEntity();
                }
                catch (FaultException exception) {
                    throw new IllegalStateException(exception);
                }
                Assert.assertTrue(room.isPropertyItemMarkedAsNew(room.ALIASES, room.getAliases().get(0)));
            }
            else if (command instanceof ModifyRoom) {
                ModifyRoom modifyRoom = (ModifyRoom) command;
                Room room = modifyRoom.getRoom();
                Assert.assertEquals("1", room.getId());
                Assert.assertEquals("room", room.getName());
                Assert.assertTrue(room.isPropertyItemMarkedAsNew(room.OPTIONS, Room.Option.PIN));
                Assert.assertEquals("1234", room.getOption(Room.Option.PIN));
            }
            else {
                throw new TodoImplementException(command.getClass().getName());
            }
            return null;
        }
    }
}
