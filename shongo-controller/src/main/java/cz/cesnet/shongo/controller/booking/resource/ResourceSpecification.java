package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.controller.ObjectType;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.specification.Specification;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.scheduler.*;
import cz.cesnet.shongo.util.ObjectHelper;
import org.joda.time.Interval;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.OneToOne;

/**
 * Represents a specific existing resource in the compartment.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ResourceSpecification extends Specification implements ReservationTaskProvider
{
    /**
     * Specific resource.
     */
    private Resource resource;

    /**
     * Constructor.
     */
    public ResourceSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param resource sets the {@link #resource}
     */
    public ResourceSpecification(Resource resource)
    {
        this.resource = resource;
    }

    /**
     * @return {@link #resource}
     */
    @OneToOne
    public Resource getResource()
    {
        return resource;
    }

    /**
     * @param resource sets the {@link #resource}
     */
    public void setResource(Resource resource)
    {
        this.resource = resource;
    }

    @Override
    public boolean synchronizeFrom(Specification specification, EntityManager entityManager)
    {
        ResourceSpecification resourceSpecification = (ResourceSpecification) specification;

        boolean modified = super.synchronizeFrom(specification, entityManager);
        modified |= !ObjectHelper.isSamePersistent(getResource(), resourceSpecification.getResource());

        setResource(resourceSpecification.getResource());

        return modified;
    }

    @Override
    public ReservationTask createReservationTask(SchedulerContext schedulerContext, Interval slot)
            throws SchedulerException
    {
        return new ReservationTask(schedulerContext, slot)
        {
            @Override
            protected Reservation allocateReservation() throws SchedulerException
            {
                ResourceReservationTask reservationTask = new ResourceReservationTask(schedulerContext, slot, resource);
                Reservation reservation = reservationTask.perform();
                addReports(reservationTask);
                return reservation;
            }
        };
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return new cz.cesnet.shongo.controller.api.ResourceSpecification();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi)
    {
        cz.cesnet.shongo.controller.api.ResourceSpecification resourceSpecificationApi =
                (cz.cesnet.shongo.controller.api.ResourceSpecification) specificationApi;
        resourceSpecificationApi.setResourceId(ObjectIdentifier.formatId(resource));
        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.ResourceSpecification resourceSpecificationApi =
                (cz.cesnet.shongo.controller.api.ResourceSpecification) specificationApi;

        if (resourceSpecificationApi.getResourceId() == null) {
            setResource(null);
        }
        else {
            Long resourceId = ObjectIdentifier.parseId(resourceSpecificationApi.getResourceId(), ObjectType.RESOURCE);
            ResourceManager resourceManager = new ResourceManager(entityManager);
            setResource(resourceManager.get(resourceId));
        }

        super.fromApi(specificationApi, entityManager);
    }
}
