package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.resource.VirtualRoomsCapability;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.controller.scheduler.VirtualRoomReservationTask;
import cz.cesnet.shongo.fault.FaultException;
import org.apache.commons.lang.ObjectUtils;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a specification for virtual room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class VirtualRoomSpecification extends Specification implements ReservationTaskProvider
{
    /**
     * Set of technologies which the virtual rooms must support.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Number of ports which must be allocated for the virtual room.
     */
    private int portCount;

    /**
     * Preferred {@link Resource} with {@link VirtualRoomsCapability}.
     */
    private DeviceResource deviceResource;

    /**
     * Constructor.
     */
    public VirtualRoomSpecification()
    {
    }

    /**
     * @return {@link #technologies}
     */
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @Access(AccessType.FIELD)
    public Set<Technology> getTechnologies()
    {
        return Collections.unmodifiableSet(technologies);
    }

    /**
     * @param technology technology to be added to the set of technologies that the device support.
     */
    public void addTechnology(Technology technology)
    {
        technologies.add(technology);
    }

    /**
     * @param technology technology to be removed from the {@link #technologies}
     */
    public void removeTechnology(Technology technology)
    {
        technologies.remove(technology);
    }

    /**
     * @return {@link #portCount}
     */
    public int getPortCount()
    {
        return portCount;
    }

    /**
     * @param portCount sets the {@link #portCount}
     */
    public void setPortCount(int portCount)
    {
        this.portCount = portCount;
    }

    /**
     * @return {@link #deviceResource}
     */
    @OneToOne
    public DeviceResource getDeviceResource()
    {
        return deviceResource;
    }

    /**
     * @param deviceResource sets the {@link #deviceResource}
     */
    public void setDeviceResource(DeviceResource deviceResource)
    {
        this.deviceResource = deviceResource;
    }

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        VirtualRoomSpecification resourceSpecification = (VirtualRoomSpecification) specification;

        boolean modified = super.synchronizeFrom(specification);
        modified |= !ObjectUtils.equals(getDeviceResource(), resourceSpecification.getDeviceResource());

        setDeviceResource(resourceSpecification.getDeviceResource());

        return modified;
    }

    @Override
    public ReservationTask createReservationTask(ReservationTask.Context context)
    {
        return new VirtualRoomReservationTask(context, this);
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return new cz.cesnet.shongo.controller.api.VirtualRoomSpecification();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi, Domain domain)
    {
        cz.cesnet.shongo.controller.api.VirtualRoomSpecification virtualRoomSpecificationApi =
                (cz.cesnet.shongo.controller.api.VirtualRoomSpecification) specificationApi;
        for (Technology technology : technologies) {
            virtualRoomSpecificationApi.addTechnology(technology);
        }
        virtualRoomSpecificationApi.setPortCount(getPortCount());
        if (deviceResource != null) {
            virtualRoomSpecificationApi.setResourceIdentifier(domain.formatIdentifier(deviceResource.getId()));
        }
        super.toApi(specificationApi, domain);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager,
            Domain domain) throws FaultException
    {
        cz.cesnet.shongo.controller.api.VirtualRoomSpecification virtualRoomSpecificationApi =
                (cz.cesnet.shongo.controller.api.VirtualRoomSpecification) specificationApi;
        if (virtualRoomSpecificationApi.isPropertyFilled(virtualRoomSpecificationApi.PORT_COUNT)) {
            setPortCount(virtualRoomSpecificationApi.getPortCount());
        }
        if (virtualRoomSpecificationApi.isPropertyFilled(virtualRoomSpecificationApi.RESOURCE_IDENTIFIER)) {
            if (virtualRoomSpecificationApi.getResourceIdentifier() == null) {
                setDeviceResource(null);
            }
            else {
                Long resourceId = domain.parseIdentifier(virtualRoomSpecificationApi.getResourceIdentifier());
                ResourceManager resourceManager = new ResourceManager(entityManager);
                setDeviceResource(resourceManager.getDevice(resourceId));
            }
        }

        // Create technologies
        for (Technology technology : virtualRoomSpecificationApi.getTechnologies()) {
            if (specificationApi.isCollectionItemMarkedAsNew(
                    cz.cesnet.shongo.controller.api.DeviceResource.TECHNOLOGIES, technology)) {
                addTechnology(technology);
            }
        }
        // Delete technologies
        Set<Technology> technologies = specificationApi.getCollectionItemsMarkedAsDeleted(
                cz.cesnet.shongo.controller.api.DeviceResource.TECHNOLOGIES);
        for (Technology technology : technologies) {
            removeTechnology(technology);
        }

        super.fromApi(specificationApi, entityManager, domain);
    }
}
