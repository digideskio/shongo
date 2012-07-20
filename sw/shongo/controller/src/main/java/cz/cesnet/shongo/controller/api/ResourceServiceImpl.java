package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Fault;
import cz.cesnet.shongo.api.FaultException;
import cz.cesnet.shongo.api.Technology;
import cz.cesnet.shongo.api.util.Serializer;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.ResourceDatabase;
import cz.cesnet.shongo.controller.resource.ResourceManager;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Resource service implementation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceServiceImpl extends Component implements ResourceService
{
    /**
     * @see Domain
     */
    private Domain domain;

    /**
     * @see ResourceDatabase
     */
    private ResourceDatabase resourceDatabase;

    /**
     * Constructor.
     */
    public ResourceServiceImpl()
    {
    }

    /**
     * Constructor.
     *
     * @param domain sets the {@link #domain}
     */
    public ResourceServiceImpl(Domain domain)
    {
        setDomain(domain);
    }

    /**
     * @param domain sets the {@link #domain}
     */
    public void setDomain(Domain domain)
    {
        this.domain = domain;
    }

    /**
     * @param resourceDatabase sets the {@link #resourceDatabase}
     */
    public void setResourceDatabase(ResourceDatabase resourceDatabase)
    {
        this.resourceDatabase = resourceDatabase;
    }

    @Override
    public void init()
    {
        super.init();
        if (domain == null) {
            throw new IllegalStateException(getClass().getName() + " doesn't have the domain set!");
        }
    }


    @Override
    public String getServiceName()
    {
        return "Resource";
    }

    @Override
    public String createResource(SecurityToken token, Resource resource) throws FaultException
    {
        resource.checkRequiredPropertiesFilled();

        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        // Create reservation request
        cz.cesnet.shongo.controller.resource.DeviceResource resourceImpl =
                new cz.cesnet.shongo.controller.resource.DeviceResource();

        // Fill common attributes
        resourceImpl.setName(resource.getName());

        // Fill technologies
        for (Technology technology : resource.getTechnologies()) {
            resourceImpl.addTechnology(technology);
        }

        // Fill capabilities
        for (Capability capability : resource.getCapabilities()) {
            if (capability instanceof VirtualRoomsCapability) {
                VirtualRoomsCapability virtualRoomsCapability = (VirtualRoomsCapability) capability;
                cz.cesnet.shongo.controller.resource.VirtualRoomsCapability virtualRoomsCapabilityImpl =
                        new cz.cesnet.shongo.controller.resource.VirtualRoomsCapability();
                virtualRoomsCapabilityImpl.setPortCount(virtualRoomsCapability.getPortCount());
                resourceImpl.addCapability(virtualRoomsCapabilityImpl);
            }
            else {
                throw new FaultException(Fault.Common.TODO_IMPLEMENT);
            }
        }

        // Save it
        ResourceManager resourceManager = new ResourceManager(entityManager);
        resourceManager.create(resourceImpl);

        entityManager.getTransaction().commit();
        entityManager.close();

        // Add resource to resource database
        if (resourceDatabase != null) {
            resourceDatabase.addResource(resourceImpl);
        }

        // Return resource identifier
        return domain.formatIdentifier(resourceImpl.getId());
    }

    @Override
    public void modifyResource(SecurityToken token, Resource resource) throws FaultException
    {
        Long resourceId = domain.parseIdentifier(resource.getIdentifier());

        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        ResourceManager resourceManager = new ResourceManager(entityManager);

        // Get reservation request
        cz.cesnet.shongo.controller.resource.Resource resourceImpl =
                resourceManager.get(resourceId);
        if (resourceImpl == null) {
            throw new FaultException(Fault.Common.RECORD_NOT_EXIST, ReservationRequest.class, resourceId);
        }

        if (true) {
            throw new RuntimeException("TODO: Implement ResourceServiceImpl.modifyResource");
        }

        // Modify attributes
        if (resource.isPropertyFilled(Resource.NAME)) {
            resourceImpl.setName(resource.getName());
        }

        // Update resource in resource database
        if (resourceDatabase != null) {
            resourceDatabase.removeResource(resourceImpl);
        }
    }

    @Override
    public void deleteResource(SecurityToken token, String resourceIdentifier) throws FaultException
    {
        Long resource = domain.parseIdentifier(resourceIdentifier);

        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        ResourceManager resourceManager = new ResourceManager(entityManager);

        // Get the resource
        cz.cesnet.shongo.controller.resource.Resource resourceImpl = resourceManager.get(resource);
        if (resourceImpl == null) {
            throw new FaultException(Fault.Common.RECORD_NOT_EXIST, ReservationRequest.class, resource);
        }

        // Delete the resource
        resourceManager.delete(resourceImpl);

        // Remove resource from resource database
        if (resourceDatabase != null) {
            resourceDatabase.removeResource(resourceImpl);
        }

        entityManager.getTransaction().commit();
        entityManager.close();
    }

    @Override
    public ResourceSummary[] listResources(SecurityToken token)
    {
        EntityManager entityManager = getEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);

        List<cz.cesnet.shongo.controller.resource.Resource> list = resourceManager.list();
        List<ResourceSummary> summaryList = new ArrayList<ResourceSummary>();
        for (cz.cesnet.shongo.controller.resource.Resource resource : list) {
            ResourceSummary summary = new ResourceSummary();
            summary.setIdentifier(domain.formatIdentifier(resource.getId()));
            summary.setName(resource.getName());
            summaryList.add(summary);
        }

        entityManager.close();

        return summaryList.toArray(new ResourceSummary[summaryList.size()]);
    }

    @Override
    public Resource getResource(SecurityToken token, String resourceIdentifier) throws FaultException
    {
        Long resourceId = domain.parseIdentifier(resourceIdentifier);

        EntityManager entityManager = getEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);

        cz.cesnet.shongo.controller.resource.Resource resourceImpl = resourceManager.get(resourceId);
        if (resourceImpl == null) {
            throw new FaultException(Fault.Common.RECORD_NOT_EXIST, Resource.class, resourceId);
        }

        Resource resource = Serializer.toApi(resourceImpl, Resource.class);
        resource.setIdentifier(domain.formatIdentifier(resourceImpl.getId()));

        entityManager.close();

        return resource;
    }
}
