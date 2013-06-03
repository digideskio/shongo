package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.AliasSetSpecification;
import cz.cesnet.shongo.controller.api.AliasSpecification;
import cz.cesnet.shongo.controller.api.CompartmentSpecification;
import cz.cesnet.shongo.controller.api.ExternalEndpointSetSpecification;
import cz.cesnet.shongo.controller.api.ReservationRequest;
import cz.cesnet.shongo.controller.api.ReservationRequestSet;
import cz.cesnet.shongo.controller.api.ResourceSpecification;
import cz.cesnet.shongo.controller.api.RoomSpecification;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

/**
 * Tests for creating, updating and deleting {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestManagementTest extends AbstractControllerTest
{
    /**
     * Test single reservation request.
     *
     * @throws Exception
     */
    @Test
    public void testReservationRequest() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        cz.cesnet.shongo.controller.api.ReservationRequest reservationRequest = new cz.cesnet.shongo.controller.api.ReservationRequest();
        reservationRequest.setDescription("request");
        reservationRequest.setSlot("2012-01-01T12:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new cz.cesnet.shongo.controller.api.ResourceSpecification(resourceId));
        String id1 = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        Assert.assertEquals("shongo:cz.cesnet:req:1", id1);

        Collection<ReservationRequestSummary> reservationRequests;

        // Check created reservation request
        reservationRequests = getReservationService().listReservationRequests(SECURITY_TOKEN, null);
        Assert.assertEquals("One reservation request should exist.", 1 , reservationRequests.size());
        Assert.assertEquals(id1 , reservationRequests.iterator().next().getId());
        reservationRequest = (cz.cesnet.shongo.controller.api.ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN, id1);
        Assert.assertEquals("request", reservationRequest.getDescription());
        Assert.assertEquals(ReservationRequestState.NOT_ALLOCATED, reservationRequest.getState());

        // Modify reservation request by retrieved instance of reservation request
        reservationRequest.setDescription("requestModified");
        String id2 = getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        Assert.assertEquals("shongo:cz.cesnet:req:2", id2);

        // Check already modified reservation request
        try {
            getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
            Assert.fail("Exception that reservation request has already been modified should be thrown.");
        }
        catch (ControllerReportSet.ReservationRequestAlreadyModifiedException exception) {
            Assert.assertEquals(id1, exception.getId());
        }

        // Check modified
        reservationRequests = getReservationService().listReservationRequests(SECURITY_TOKEN, null);
        Assert.assertEquals("One reservation request should exist.", 1 , reservationRequests.size());
        Assert.assertEquals(id2 , reservationRequests.iterator().next().getId());
        reservationRequest = (cz.cesnet.shongo.controller.api.ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN, id2);
        Assert.assertEquals("requestModified", reservationRequest.getDescription());

        // Modify reservation request by new instance of reservation request
        reservationRequest = new cz.cesnet.shongo.controller.api.ReservationRequest();
        reservationRequest.setId(id2);
        reservationRequest.setPurpose(ReservationRequestPurpose.EDUCATION);
        String id3 = getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        Assert.assertEquals("shongo:cz.cesnet:req:3", id3);

        // Check modified reservation request
        reservationRequests = getReservationService().listReservationRequests(SECURITY_TOKEN, null);
        Assert.assertEquals("One reservation request should exist.", 1 , reservationRequests.size());
        Assert.assertEquals(id3 , reservationRequests.iterator().next().getId());
        reservationRequest = (cz.cesnet.shongo.controller.api.ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN, id3);
        Assert.assertEquals(ReservationRequestPurpose.EDUCATION, reservationRequest.getPurpose());

        // Delete reservation request
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, id1);

        // Check deleted reservation request
        reservationRequests = getReservationService().listReservationRequests(SECURITY_TOKEN, null);
        Assert.assertEquals("No reservation request should exist.", 0 , reservationRequests.size());
        try {
            getReservationService().getReservationRequest(SECURITY_TOKEN, id1);
            Assert.fail("Reservation request should not exist.");
        }
        catch (ControllerReportSet.ReservationRequestDeletedException exception) {
            Assert.assertEquals(id1, exception.getId());
        }
    }

    /**
     * Test set of reservation requests.
     *
     * @throws Exception
     */
    @Test
    public void testReservationRequestSet() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        cz.cesnet.shongo.controller.api.ReservationRequestSet reservationRequest = new cz.cesnet.shongo.controller.api.ReservationRequestSet();
        reservationRequest.setDescription("request");
        reservationRequest.addSlot("2012-01-01T12:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new cz.cesnet.shongo.controller.api.ResourceSpecification(resourceId));
        String id1 = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        Assert.assertEquals("shongo:cz.cesnet:req:1", id1);

        Collection<ReservationRequestSummary> reservationRequests;

        // Check created reservation request
        reservationRequests = getReservationService().listReservationRequests(SECURITY_TOKEN, null);
        Assert.assertEquals("One reservation request should exist.", 1 , reservationRequests.size());
        Assert.assertEquals(id1 , reservationRequests.iterator().next().getId());
        reservationRequest = (cz.cesnet.shongo.controller.api.ReservationRequestSet) getReservationService().getReservationRequest(SECURITY_TOKEN, id1);
        Assert.assertEquals("request", reservationRequest.getDescription());

        // Modify reservation request by retrieved instance of reservation request
        reservationRequest.setDescription("requestModified");
        String id2 = getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        Assert.assertEquals("shongo:cz.cesnet:req:2", id2);

        // Check already modified reservation request
        try {
            getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
            Assert.fail("Exception that reservation request has already been modified should be thrown.");
        }
        catch (ControllerReportSet.ReservationRequestAlreadyModifiedException exception) {
            Assert.assertEquals(id1, exception.getId());
        }

        // Check modified
        reservationRequests = getReservationService().listReservationRequests(SECURITY_TOKEN, null);
        Assert.assertEquals("One reservation request should exist.", 1 , reservationRequests.size());
        Assert.assertEquals(id2 , reservationRequests.iterator().next().getId());
        reservationRequest = (cz.cesnet.shongo.controller.api.ReservationRequestSet) getReservationService().getReservationRequest(SECURITY_TOKEN, id2);
        Assert.assertEquals("requestModified", reservationRequest.getDescription());

        // Modify reservation request by new instance of reservation request
        reservationRequest = new cz.cesnet.shongo.controller.api.ReservationRequestSet();
        reservationRequest.setId(id2);
        reservationRequest.setPurpose(ReservationRequestPurpose.EDUCATION);
        String id3 = getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        Assert.assertEquals("shongo:cz.cesnet:req:3", id3);

        // Check modified reservation request
        reservationRequests = getReservationService().listReservationRequests(SECURITY_TOKEN, null);
        Assert.assertEquals("One reservation request should exist.", 1 , reservationRequests.size());
        Assert.assertEquals(id3 , reservationRequests.iterator().next().getId());
        reservationRequest = (cz.cesnet.shongo.controller.api.ReservationRequestSet) getReservationService().getReservationRequest(SECURITY_TOKEN, id3);
        Assert.assertEquals(ReservationRequestPurpose.EDUCATION, reservationRequest.getPurpose());

        // Delete reservation request
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, id1);

        // Check deleted reservation request
        reservationRequests = getReservationService().listReservationRequests(SECURITY_TOKEN, null);
        Assert.assertEquals("No reservation request should exist.", 0 , reservationRequests.size());
        try {
            getReservationService().getReservationRequest(SECURITY_TOKEN, id1);
            Assert.fail("Reservation request should not exist.");
        }
        catch (ControllerReportSet.ReservationRequestDeletedException exception) {
            Assert.assertEquals(id1, exception.getId());
        }
    }

    /**
     * Test modify {@link cz.cesnet.shongo.controller.api.CompartmentSpecification} to {@link cz.cesnet.shongo.controller.api.RoomSpecification} and delete the request).
     *
     * @throws Exception
     */
    @Test
    public void testReservationRequestSetModification() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("firstMcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.addCapability(new AliasProviderCapability("95{digit:1}", AliasType.H323_E164));
        mcu.setAllocatable(true);
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        cz.cesnet.shongo.controller.api.ReservationRequestSet reservationRequest = new cz.cesnet.shongo.controller.api.ReservationRequestSet();
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.addSlot(new PeriodicDateTimeSlot("2012-01-01T00:00", "PT1H", "P1W", "2012-01-01"));
        cz.cesnet.shongo.controller.api.CompartmentSpecification compartmentSpecification = new cz.cesnet.shongo.controller.api.CompartmentSpecification();
        compartmentSpecification.addSpecification(new cz.cesnet.shongo.controller.api.ExternalEndpointSetSpecification(Technology.H323, 3));
        reservationRequest.setSpecification(compartmentSpecification);

        String id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        runPreprocessor();
        runScheduler();
        checkAllocated(id);

        reservationRequest = (cz.cesnet.shongo.controller.api.ReservationRequestSet) getReservationService().getReservationRequest(SECURITY_TOKEN, id);
        cz.cesnet.shongo.controller.api.RoomSpecification roomSpecification = new cz.cesnet.shongo.controller.api.RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.setParticipantCount(5);
        reservationRequest.setSpecification(roomSpecification);
        getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, id);
    }

    /**
     * Test listing reservation requests based on {@link Technology} of
     * {@link cz.cesnet.shongo.controller.api.AliasSpecification},
     * {@link cz.cesnet.shongo.controller.api.AliasSetSpecification},
     * {@link cz.cesnet.shongo.controller.api.RoomSpecification} or
     * {@link cz.cesnet.shongo.controller.api.CompartmentSpecification}.
     *
     * @throws Exception
     */
    @Test
    public void testListReservationRequestsByTechnology() throws Exception
    {
        cz.cesnet.shongo.controller.api.ReservationRequest reservationRequest1 = new cz.cesnet.shongo.controller.api.ReservationRequest();
        reservationRequest1.setSlot("2012-01-01T12:00", "PT2H");
        reservationRequest1.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest1.setSpecification(new cz.cesnet.shongo.controller.api.AliasSpecification(AliasType.H323_E164).withValue("001"));
        getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest1);

        cz.cesnet.shongo.controller.api.ReservationRequest reservationRequest2 = new cz.cesnet.shongo.controller.api.ReservationRequest();
        reservationRequest2.setSlot("2012-01-01T12:00", "PT2H");
        reservationRequest2.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest2.setSpecification(new AliasSetSpecification(AliasType.SIP_URI));
        getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest2);

        cz.cesnet.shongo.controller.api.ReservationRequest reservationRequest3 = new cz.cesnet.shongo.controller.api.ReservationRequest();
        reservationRequest3.setSlot("2012-01-01T12:00", "PT2H");
        reservationRequest3.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest3.setSpecification(
                new cz.cesnet.shongo.controller.api.RoomSpecification(5, new Technology[]{Technology.ADOBE_CONNECT}));
        getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest3);

        cz.cesnet.shongo.controller.api.ReservationRequestSet reservationRequest4 = new cz.cesnet.shongo.controller.api.ReservationRequestSet();
        reservationRequest4.addSlot("2012-01-01T12:00", "PT2H");
        reservationRequest4.setPurpose(ReservationRequestPurpose.SCIENCE);
        cz.cesnet.shongo.controller.api.CompartmentSpecification compartmentSpecification3 = new CompartmentSpecification();
        compartmentSpecification3.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 5));
        reservationRequest4.setSpecification(compartmentSpecification3);
        getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest4);

        cz.cesnet.shongo.controller.api.ReservationRequest reservationRequest5 = new cz.cesnet.shongo.controller.api.ReservationRequest();
        reservationRequest5.setSlot("2012-01-01T12:00", "PT2H");
        reservationRequest5.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest5.setSpecification(
                new cz.cesnet.shongo.controller.api.RoomSpecification(5, new Technology[]{Technology.H323, Technology.SIP}));
        getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest5);

        cz.cesnet.shongo.controller.api.ReservationRequestSet reservationRequest6 = new ReservationRequestSet();
        reservationRequest6.addSlot("2012-01-01T12:00", "PT2H");
        reservationRequest6.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest6.setSpecification(
                new cz.cesnet.shongo.controller.api.RoomSpecification(5, new Technology[]{Technology.H323, Technology.SIP}));
        getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest6);

        Collection<ReservationRequestSummary> reservationRequests =
                getReservationService().listReservationRequests(SECURITY_TOKEN, null);
        Assert.assertEquals(6, reservationRequests.size());

        Assert.assertEquals(4, getReservationService().listReservationRequests(SECURITY_TOKEN,
                buildTechnologyFilter(new Technology[]{Technology.H323})).size());
        Assert.assertEquals(3, getReservationService().listReservationRequests(SECURITY_TOKEN,
                buildTechnologyFilter(new Technology[]{Technology.SIP})).size());
        Assert.assertEquals(5, getReservationService().listReservationRequests(SECURITY_TOKEN,
                buildTechnologyFilter(new Technology[]{Technology.H323, Technology.SIP})).size());
        Assert.assertEquals(1, getReservationService().listReservationRequests(SECURITY_TOKEN,
                buildTechnologyFilter(new Technology[]{Technology.ADOBE_CONNECT})).size());
    }

    /**
     * @param technologies
     * @return built filter for {@link ReservationService#listReservationRequests(SecurityToken, java.util.Map)}
     */
    private static Map<String, Object> buildTechnologyFilter(Technology[] technologies)
    {
        Map<String, Object> filter = new HashMap<String, Object>();
        Set<Technology> filterTechnologies = null;
        if (technologies != null) {
            filterTechnologies = new HashSet<Technology>();
            for (Technology technology : technologies) {
                filterTechnologies.add(technology);
            }
        }
        filter.put("technology", filterTechnologies);
        return filter;
    }

    /**
     * Test reservation request for infinite start/end/whole interval
     *
     * @throws Exception
     */
    @Test
    public void testSlotDuration() throws Exception
    {
        try {
            cz.cesnet.shongo.controller.api.ReservationRequest reservationRequest = new cz.cesnet.shongo.controller.api.ReservationRequest();
            reservationRequest.setDescription("request");
            reservationRequest.setSlot("2012-01-01T12:00", "PT0S");
            reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
            reservationRequest.setSpecification(new cz.cesnet.shongo.controller.api.AliasSpecification(AliasType.ROOM_NAME));
            getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
            Assert.fail("Exception of empty duration should has been thrown.");
        }
        catch (ControllerReportSet.ReservationRequestEmptyDurationException exception) {
        }
    }

    /**
     * Test reservation request for infinite start/end/whole interval
     *
     * @throws Exception
     */
    @Test
    public void testInfiniteReservationRequest() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        cz.cesnet.shongo.controller.api.ReservationRequest reservationRequest1 = new cz.cesnet.shongo.controller.api.ReservationRequest();
        reservationRequest1.setSlot(Temporal.INTERVAL_INFINITE);
        reservationRequest1.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest1.setSpecification(new cz.cesnet.shongo.controller.api.ResourceSpecification(resourceId));
        getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest1);

        cz.cesnet.shongo.controller.api.ReservationRequest reservationRequest2 = new cz.cesnet.shongo.controller.api.ReservationRequest();
        reservationRequest2.setSlot(Temporal.DATETIME_INFINITY_START, DateTime.parse("2012-01-01"));
        reservationRequest2.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest2.setSpecification(new cz.cesnet.shongo.controller.api.ResourceSpecification(resourceId));
        getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest2);

        cz.cesnet.shongo.controller.api.ReservationRequest reservationRequest3 = new cz.cesnet.shongo.controller.api.ReservationRequest();
        reservationRequest3.setSlot(DateTime.parse("2012-01-01"), Temporal.DATETIME_INFINITY_END);
        reservationRequest3.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest3.setSpecification(new ResourceSpecification(resourceId));
        getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest3);

        List<Object> params = new ArrayList<Object>();
        params.add(SECURITY_TOKEN.getAccessToken());
        params.add(null);

        Object[] result = (Object[]) getControllerClient().execute("Reservation.listReservationRequests", params);
        Interval slot1 = ((ReservationRequestSummary) result[0]).getEarliestSlot();
        Assert.assertEquals(Temporal.DATETIME_INFINITY_START, slot1.getStart());
        Assert.assertEquals(Temporal.DATETIME_INFINITY_END, slot1.getEnd());
        Interval slot2 = ((ReservationRequestSummary) result[1]).getEarliestSlot();
        Assert.assertEquals(Temporal.DATETIME_INFINITY_START, slot2.getStart());
        Assert.assertThat(Temporal.DATETIME_INFINITY_END, is(not(slot2.getEnd())));
        Interval slot3 = ((ReservationRequestSummary) result[2]).getEarliestSlot();
        Assert.assertThat(Temporal.DATETIME_INFINITY_START, is(not(slot3.getStart())));
        Assert.assertEquals(Temporal.DATETIME_INFINITY_END, slot3.getEnd());
    }

    @Test
    public void testCheckSpecificationAvailability() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.addCapability(new AliasProviderCapability("test", AliasType.ROOM_NAME));
        resource.setAllocatable(true);
        getResourceService().createResource(SECURITY_TOKEN, resource);

        Interval interval = Interval.parse("2012-01-01/2012-12-31");
        Object result;

        cz.cesnet.shongo.controller.api.AliasSpecification aliasSpecification = new AliasSpecification();
        aliasSpecification.addAliasType(AliasType.ROOM_NAME);
        aliasSpecification.setValue("test");

        result = getReservationService().checkSpecificationAvailability(SECURITY_TOKEN, aliasSpecification, interval);
        Assert.assertEquals(Boolean.TRUE, result);

        cz.cesnet.shongo.controller.api.ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(interval);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(aliasSpecification);
        allocateAndCheck(reservationRequest);

        result = getReservationService().checkSpecificationAvailability(SECURITY_TOKEN, aliasSpecification, interval);
        Assert.assertEquals(String.class, result.getClass());

        try {
            getReservationService().checkSpecificationAvailability(SECURITY_TOKEN,
                    new RoomSpecification(1, Technology.H323), interval);
            Assert.fail("Room specification should not be able to be checked for availability for now.");
        }
        catch (RuntimeException exception) {
            Assert.assertTrue(exception.getMessage().contains(
                    "Specification 'RoomSpecification' cannot be checked for availability"));
        }
    }
}