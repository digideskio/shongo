package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractDatabaseTest;
import cz.cesnet.shongo.controller.ControllerClient;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestType;
import cz.cesnet.shongo.fault.CommonFault;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import org.apache.xmlrpc.XmlRpcException;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.*;

/**
 * Tests for using the implementation of {@link ReservationService} through XML-RPC.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationServiceImplTest extends AbstractDatabaseTest
{
    cz.cesnet.shongo.controller.Controller controller;

    ControllerClient controllerClient;

    ReservationService reservationService;

    @Override
    public void before() throws Exception
    {
        super.before();

        // Start controller
        controller = new cz.cesnet.shongo.controller.Controller();
        controller.setDomain("cz.cesnet", "CESNET, z.s.p.o.");
        controller.setEntityManagerFactory(getEntityManagerFactory());
        controller.addService(new ReservationServiceImpl());
        controller.start();
        controller.startRpc();

        // Start client
        controllerClient = new ControllerClient(controller.getRpcHost(), controller.getRpcPort());

        // Get reservation service from client
        reservationService = controllerClient.getService(ReservationService.class);
    }

    @Override
    public void after()
    {
        controller.stop();

        super.after();
    }

    @Test
    public void testCreateReservationRequest() throws Exception
    {
        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.setType(ReservationRequestType.NORMAL);
        reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSet.addSlot(DateTime.parse("2012-06-01T15:00"), Period.parse("PT2H"));
        reservationRequestSet.addSlot(new PeriodicDateTime(DateTime.parse("2012-07-01T14:00"), Period.parse("P1W")),
                Period.parse("PT2H"));
        CompartmentSpecification compartment = reservationRequestSet.addSpecification(new CompartmentSpecification());
        compartment.addSpecification(new PersonSpecification("Martin Srom", "srom@cesnet.cz"));
        compartment.addSpecification(new ExternalEndpointSpecification(Technology.H323, 2));

        String identifier = reservationService.createReservationRequest(new SecurityToken(), reservationRequestSet);
        assertEquals("shongo:cz.cesnet:1", identifier);
    }

    @Test
    public void testCreateReservationRequestByRawRpcXml() throws Exception
    {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("type", "NORMAL");
        attributes.put("purpose", "SCIENCE");
        attributes.put("slots", new ArrayList<Object>()
        {{
                add(new HashMap<String, Object>()
                {{
                        put("start", "2012-06-01T15:00");
                        put("duration", "PT2H");
                    }});
                add(new HashMap<String, Object>()
                {{
                        put("start", new HashMap<String, Object>()
                        {{
                                put("start", "2012-07-01T14:00");
                                put("period", "P1W");
                            }});
                        put("duration", "PT2H");
                    }});
            }});
        attributes.put("compartments", new ArrayList<Object>()
        {{
                add(new HashMap<String, Object>()
                {{
                        put("persons", new ArrayList<Object>()
                        {{
                                add(new HashMap<String, Object>()
                                {{
                                        put("name", "Martin Srom");
                                        put("email", "srom@cesnet.cz");
                                    }});
                            }});
                        put("resources", new ArrayList<Object>()
                        {{
                                add(new HashMap<String, Object>()
                                {{
                                        put("class", "ExternalEndpointSpecification");
                                        put("technology", "H323");
                                        put("count", 2);
                                        put("persons", new ArrayList<Object>()
                                        {{
                                                add(new HashMap<String, Object>()
                                                {{
                                                        put("name", "Ondrej Bouda");
                                                        put("email", "bouda@cesnet.cz");
                                                    }});
                                                add(new HashMap<String, Object>()
                                                {{
                                                        put("name", "Petr Holub");
                                                        put("email", "hopet@cesnet.cz");
                                                    }});
                                            }});
                                    }});

                            }});
                    }});
            }});

        List<Object> params = new ArrayList<Object>();
        params.add(new HashMap<String, Object>());
        params.add(attributes);

        String identifier = (String) controllerClient.execute("Reservation.createReservationRequest", params);
        assertEquals("shongo:cz.cesnet:1", identifier);
    }

    @Test
    public void testModifyAndDeleteReservationRequest() throws Exception
    {
        SecurityToken securityToken = new SecurityToken();
        String identifier = null;

        // ---------------------------
        // Create reservation request
        // ---------------------------
        {
            ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
            reservationRequestSet.setType(ReservationRequestType.NORMAL);
            reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
            reservationRequestSet.addSlot(DateTime.parse("2012-06-01T15:00"), Period.parse("PT2H"));
            reservationRequestSet.addSlot(new PeriodicDateTime(DateTime.parse("2012-07-01T14:00"), Period.parse("P1W")),
                    Period.parse("PT2H"));
            CompartmentSpecification compartmentSpecification =
                    reservationRequestSet.addSpecification(new CompartmentSpecification());
            compartmentSpecification.addSpecification(new PersonSpecification("Martin Srom", "srom@cesnet.cz"));

            identifier = reservationService.createReservationRequest(securityToken, reservationRequestSet);
            assertNotNull(identifier);

            reservationRequestSet = (ReservationRequestSet) reservationService.getReservationRequest(securityToken,
                    identifier);
            assertNotNull(reservationRequestSet);
            assertEquals(ReservationRequestType.NORMAL, reservationRequestSet.getType());
            assertEquals(ReservationRequestPurpose.SCIENCE, reservationRequestSet.getPurpose());
            assertEquals(2, reservationRequestSet.getSlots().size());
            assertEquals(1, reservationRequestSet.getSpecifications().size());
        }

        // ---------------------------
        // Modify reservation request
        // ---------------------------
        {
            ReservationRequestSet reservationRequestSet =
                    (ReservationRequestSet) reservationService.getReservationRequest(securityToken, identifier);
            reservationRequestSet.setType(ReservationRequestType.PERMANENT);
            reservationRequestSet.setPurpose(null);
            reservationRequestSet.removeSlot(reservationRequestSet.getSlots().iterator().next());
            CompartmentSpecification compartmentSpecification =
                    reservationRequestSet.addSpecification(new CompartmentSpecification());
            reservationRequestSet.removeSpecification(compartmentSpecification);

            reservationService.modifyReservationRequest(securityToken, reservationRequestSet);

            reservationRequestSet = (ReservationRequestSet) reservationService.getReservationRequest(securityToken,
                    identifier);
            assertNotNull(reservationRequestSet);
            assertEquals(ReservationRequestType.PERMANENT, reservationRequestSet.getType());
            assertEquals(null, reservationRequestSet.getPurpose());
            assertEquals(1, reservationRequestSet.getSlots().size());
            assertEquals(1, reservationRequestSet.getSpecifications().size());
        }

        // ---------------------------
        // Delete reservation request
        // ---------------------------
        {
            ReservationRequestSet reservationRequestSet =
                    (ReservationRequestSet) reservationService.getReservationRequest(securityToken, identifier);
            assertNotNull(reservationRequestSet);

            reservationService.deleteReservationRequest(securityToken, identifier);

            try {
                reservationRequestSet = (ReservationRequestSet) reservationService.getReservationRequest(securityToken,
                        identifier);
                fail("Exception that record doesn't exists should be thrown.");
            }
            catch (EntityNotFoundException exception) {
            }
        }
    }

    @Test
    public void testExceptions() throws Exception
    {
        Map<String, Object> reservationRequest = null;

        reservationRequest = new HashMap<String, Object>();
        reservationRequest.put("slots", new ArrayList<Object>()
        {{
                add(new HashMap<String, Object>());
            }});
        try {
            controllerClient.execute("Reservation.createReservationRequest",
                    new Object[]{new HashMap(), reservationRequest});
            fail("Exception that collection cannot contain null should be thrown.");
        }
        catch (XmlRpcException exception) {
            assertEquals(CommonFault.COLLECTION_ITEM_NULL.getCode(), exception.code);
        }

        reservationRequest = new HashMap<String, Object>();
        reservationRequest.put("slots", new ArrayList<Object>()
        {{
                add(new HashMap<String, Object>()
                {{
                        put("start", "xxx");
                    }});
            }});
        try {
            controllerClient.execute("Reservation.createReservationRequest",
                    new Object[]{new HashMap(), reservationRequest});
            fail("Exception that attribute has wrong type should be thrown.");
        }
        catch (XmlRpcException exception) {
            assertEquals(CommonFault.CLASS_ATTRIBUTE_TYPE_MISMATCH.getCode(), exception.code);
        }

        reservationRequest = new HashMap<String, Object>();
        reservationRequest.put("requests", new ArrayList<Object>());
        try {
            controllerClient.execute("Reservation.createReservationRequest",
                    new Object[]{new HashMap(), reservationRequest});
            fail("Exception that attribute is read only should be thrown.");
        }
        catch (XmlRpcException exception) {
            assertEquals(CommonFault.CLASS_ATTRIBUTE_READ_ONLY.getCode(), exception.code);
        }
    }
}
