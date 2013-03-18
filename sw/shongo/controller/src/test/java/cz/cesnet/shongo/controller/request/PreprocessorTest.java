package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractDatabaseTest;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.Preprocessor;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.common.OtherPerson;
import cz.cesnet.shongo.controller.common.PeriodicDateTime;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

/**
 * Tests for using {@link Preprocessor} that synchronizes {@link ReservationRequestSet}(s)
 * with {@link ReservationRequest}(s).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PreprocessorTest extends AbstractDatabaseTest
{
    @Test
    public void test() throws Exception
    {
        EntityManager entityManager = getEntityManager();

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        // Create reservation request
        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.setUserId(Authorization.ROOT_USER_ID);
        reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSet.addSlot("2012-06-01T15", "PT1H");
        reservationRequestSet.addSlot(new PeriodicDateTime(
                DateTime.parse("2012-07-01T14:00"), Period.parse("P1W"), LocalDate.parse("2012-07-15")), "PT2H");
        MultiCompartmentSpecification multiCompartmentSpecification = new MultiCompartmentSpecification();
        reservationRequestSet.setSpecification(multiCompartmentSpecification);
        // First compartment
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addChildSpecification(new ExternalEndpointSpecification(Technology.SIP));
        EndpointSpecification endpointSpecification = new ExternalEndpointSpecification(Technology.H323);
        endpointSpecification.addPerson(new OtherPerson("Martin Srom", "martin.srom@cesnet.cz"));
        compartmentSpecification.addChildSpecification(endpointSpecification);
        multiCompartmentSpecification.addSpecification(compartmentSpecification);
        // Second compartment
        compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addChildSpecification(
                new ExternalEndpointSetSpecification(Technology.ADOBE_CONNECT, 2));
        multiCompartmentSpecification.addSpecification(compartmentSpecification);

        // Save it
        reservationRequestManager.create(reservationRequestSet);

        // Run preprocessor
        Preprocessor.createAndRun(new Interval(
                DateTime.parse("2012-06-01T00:00:00"), DateTime.parse("2012-06-01T23:59:59")), entityManager);
        assertEquals(1, reservationRequestManager.listReservationRequestsBySet(reservationRequestSet).size());
        Preprocessor.createAndRun(new Interval(
                DateTime.parse("2012-07-02T00:00:00"), DateTime.parse("2012-07-08T23:59:59")), entityManager);
        assertEquals(2, reservationRequestManager.listReservationRequestsBySet(reservationRequestSet).size());
        Preprocessor.createAndRun(new Interval(
                DateTime.parse("2012-06-01T00:00:00"), DateTime.parse("2012-07-08T23:59:59")), entityManager);
        assertEquals(3, reservationRequestManager.listReservationRequestsBySet(reservationRequestSet).size());

        // Check created reservation requests
        List<ReservationRequest> reservationRequests =
                reservationRequestManager.listReservationRequestsBySet(reservationRequestSet);
        assertEquals(3, reservationRequests.size());
        assertEquals(new Interval(DateTime.parse("2012-06-01T15:00"), Period.parse("PT1H")),
                reservationRequests.get(0).getSlot());
        assertEquals(new Interval(DateTime.parse("2012-07-01T14:00"), Period.parse("PT2H")),
                reservationRequests.get(1).getSlot());
        assertEquals(new Interval(DateTime.parse("2012-07-08T14:00"), Period.parse("PT2H")),
                reservationRequests.get(2).getSlot());

        // Modify reservation request
        reservationRequestSet.setPurpose(ReservationRequestPurpose.EDUCATION);
        reservationRequestSet.removeSlot(reservationRequestSet.getSlots().get(0));
        multiCompartmentSpecification = (MultiCompartmentSpecification) reservationRequestSet.getSpecification();
        multiCompartmentSpecification.removeSpecification(multiCompartmentSpecification.getSpecifications().get(1));

        // Update request
        reservationRequestManager.update(reservationRequestSet);

        // Run preprocessor
        Preprocessor.createAndRun(new Interval(
                DateTime.parse("2012-06-01T00:00:00"), DateTime.parse("2012-07-08T23:59:59")), entityManager);

        // Check modified compartments
        reservationRequests = reservationRequestManager.listReservationRequestsBySet(reservationRequestSet);
        assertEquals(2, reservationRequests.size());
        assertEquals(new Interval(DateTime.parse("2012-07-01T14:00"), Period.parse("PT2H")),
                reservationRequests.get(0).getSlot());
        assertEquals(new Interval(DateTime.parse("2012-07-08T14:00"), Period.parse("PT2H")),
                reservationRequests.get(1).getSlot());

        entityManager.close();
    }

    @Test
    public void testClonedSpecifications() throws Exception
    {
        EntityManager entityManager = getEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.setUserId(Authorization.ROOT_USER_ID);
        reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSet.addSlot("2012-01-01", "PT1H");
        reservationRequestSet.addSlot("2012-01-02", "PT1H");
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addChildSpecification(new ExternalEndpointSetSpecification(Technology.H323, 2));
        compartmentSpecification.addChildSpecification(
                new PersonSpecification(new OtherPerson("Martin Srom", "srom@cesnet.cz")));
        reservationRequestSet.setSpecification(compartmentSpecification);
        reservationRequestManager.create(reservationRequestSet);

        Preprocessor.createAndRun(new Interval(
                DateTime.parse("2012-01-01"), DateTime.parse("2012-01-03")), entityManager);

        List<ReservationRequest> reservationRequests =
                reservationRequestManager.listReservationRequestsBySet(reservationRequestSet);
        assertEquals(2, reservationRequests.size());
        assertThat("Compartment specifications in reservation requests created from single reservation request set"
                + " should be different database instances.",
                reservationRequests.get(0).getSpecification().getId(),
                is(not(reservationRequests.get(1).getSpecification().getId())));
        CompartmentSpecification compartmentSpecification1 =
                (CompartmentSpecification) reservationRequests.get(0).getSpecification();
        CompartmentSpecification compartmentSpecification2 =
                (CompartmentSpecification) reservationRequests.get(1).getSpecification();
        assertThat("External endpoint specifications in reservation requests created from single"
                + " reservation request set should be different database instances.",
                compartmentSpecification1.getSpecifications().get(0).getId(),
                is(not(compartmentSpecification2.getSpecifications().get(0).getId())));
        assertThat("Person specifications in reservation requests created from single reservation request set"
                + " should be different database instances.",
                compartmentSpecification1.getSpecifications().get(1).getId(),
                is(not(compartmentSpecification2.getSpecifications().get(1).getId())));

        entityManager.close();
    }

    @Test
    public void testModification() throws Exception
    {
        Interval preprocessorInterval = new Interval(DateTime.parse("2012-01-01"), DateTime.parse("2012-01-03"));
        List<ReservationRequest> reservationRequests = null;
        CompartmentSpecification createdCompartmentSpecification = null;

        EntityManager entityManager = getEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        // -------------------------------
        // Create reservation request set
        // -------------------------------
        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.setUserId(Authorization.ROOT_USER_ID);
        reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSet.addSlot("2012-01-01", "PT1H");
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addChildSpecification(new ExternalEndpointSetSpecification(Technology.H323, 2));
        compartmentSpecification.addChildSpecification(
                new PersonSpecification(new OtherPerson("Martin Srom", "srom@cesnet.cz")));
        reservationRequestSet.setSpecification(compartmentSpecification);
        reservationRequestManager.create(reservationRequestSet);

        Preprocessor.createAndRun(preprocessorInterval, entityManager);

        reservationRequests = reservationRequestManager.listReservationRequestsBySet(reservationRequestSet);
        assertEquals(1, reservationRequests.size());
        createdCompartmentSpecification = (CompartmentSpecification) reservationRequests.get(0).getSpecification();
        assertEquals(2, createdCompartmentSpecification.getChildSpecifications().size());

        // -------------------------------
        // Modify reservation request set
        // -------------------------------
        compartmentSpecification.removeSpecification(compartmentSpecification.getSpecifications().get(1));
        reservationRequestManager.update(reservationRequestSet);

        Preprocessor.createAndRun(preprocessorInterval, entityManager);

        reservationRequests = reservationRequestManager.listReservationRequestsBySet(reservationRequestSet);
        assertEquals(1, reservationRequests.size());
        createdCompartmentSpecification = (CompartmentSpecification) reservationRequests.get(0).getSpecification();
        assertEquals("Specification should be deleted from the created reservation request",
                1, createdCompartmentSpecification.getChildSpecifications().size());

        compartmentSpecification.addChildSpecification(
                new PersonSpecification(new OtherPerson("Martin Srom", "srom@cesnet.cz")));
        reservationRequestManager.update(reservationRequestSet);

        Preprocessor.createAndRun(preprocessorInterval, entityManager);

        reservationRequests = reservationRequestManager.listReservationRequestsBySet(reservationRequestSet);
        assertEquals(1, reservationRequests.size());
        createdCompartmentSpecification = (CompartmentSpecification) reservationRequests.get(0).getSpecification();
        assertEquals("Specification should be added to the created reservation request",
                2, createdCompartmentSpecification.getChildSpecifications().size());

        ((PersonSpecification) compartmentSpecification.getSpecifications().get(1)).setPerson(
                new OtherPerson("Ondrej Bouda", "bouda@cesnet.cz"));
        reservationRequestManager.update(reservationRequestSet);

        Preprocessor.createAndRun(preprocessorInterval, entityManager);

        reservationRequests = reservationRequestManager.listReservationRequestsBySet(reservationRequestSet);
        assertEquals(1, reservationRequests.size());
        createdCompartmentSpecification = (CompartmentSpecification) reservationRequests.get(0).getSpecification();
        assertEquals("Specification should be updated to the created reservation request",
                ((PersonSpecification) compartmentSpecification.getSpecifications().get(1)).getPerson(),
                ((PersonSpecification) createdCompartmentSpecification.getSpecifications().get(1)).getPerson());

        entityManager.close();
    }
}
