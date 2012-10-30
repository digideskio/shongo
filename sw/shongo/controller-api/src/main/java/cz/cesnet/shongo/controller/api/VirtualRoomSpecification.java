package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.annotation.Required;

import java.util.Set;

/**
 * {@link Specification} virtual room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class VirtualRoomSpecification extends Specification
{
    /**
     * Set of technologies which the virtual rooms must support.
     */
    public static final String TECHNOLOGIES = "technologies";

    /**
     * Number of ports which must be allocated for the virtual room.
     */
    public static final String PORT_COUNT = "portCount";

    /**
     * Preferred {@link Resource} identifier with {@link AliasProviderCapability}.
     */
    public static final String RESOURCE_IDENTIFIER = "resourceIdentifier";

    /**
     * Constructor.
     */
    public VirtualRoomSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param resourceIdentifier sets the {@link #RESOURCE_IDENTIFIER}
     */
    public VirtualRoomSpecification(String resourceIdentifier)
    {
        setResourceIdentifier(resourceIdentifier);
    }

    /**
     * @return {@link #TECHNOLOGIES}
     */
    public Set<Technology> getTechnologies()
    {
        return getPropertyStorage().getCollection(TECHNOLOGIES, Set.class);
    }

    /**
     * @param technologies sets the {@link #TECHNOLOGIES}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        getPropertyStorage().setCollection(TECHNOLOGIES, technologies);
    }

    /**
     * @param technology technology to be added to the {@link #TECHNOLOGIES}
     */
    public void addTechnology(Technology technology)
    {
        getPropertyStorage().addCollectionItem(TECHNOLOGIES, technology, Set.class);
    }

    /**
     * @param technology technology to be removed from the {@link #TECHNOLOGIES}
     */
    public void removeTechnology(Technology technology)
    {
        getPropertyStorage().removeCollectionItem(TECHNOLOGIES, technology);
    }

    /**
     * @return {@link #RESOURCE_IDENTIFIER}
     */
    @Required
    public Integer getPortCount()
    {
        return getPropertyStorage().getValue(PORT_COUNT);
    }

    /**
     * @param portCount sets the {@link #PORT_COUNT}
     */
    public void setPortCount(Integer portCount)
    {
        getPropertyStorage().setValue(PORT_COUNT, portCount);
    }

    /**
     * @return {@link #RESOURCE_IDENTIFIER}
     */
    public String getResourceIdentifier()
    {
        return getPropertyStorage().getValue(RESOURCE_IDENTIFIER);
    }

    /**
     * @param resourceIdentifier sets the {@link #RESOURCE_IDENTIFIER}
     */
    public void setResourceIdentifier(String resourceIdentifier)
    {
        getPropertyStorage().setValue(RESOURCE_IDENTIFIER, resourceIdentifier);
    }
}
