package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Technology;

import java.util.Set;

/**
 * Represents a capability of a resource.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Capability extends ComplexType
{
    /**
     * Type of capability.
     */
    public static enum Type
    {
        STANDALONE_TERMINAL,
        TERMINAL,
        VIRTUAL_ROOMS
    }

    public static final String TYPE = "type";

    /**
     * Set of technologies for which the resource supports capability.
     */
    public static final String TECHNOLOGIES = "technologies";

    /**
     * @return {@link #TYPE}
     */
    @Required
    public Type getType()
    {
        return propertyStore.getValue(TYPE);
    }

    /**
     * @param type sets the {@link #TYPE}
     */
    public void setType(Type type)
    {
        propertyStore.setValue(TYPE, type);
    }

    /**
     * @return {@link #TECHNOLOGIES}
     */
    @ComplexType.Required
    public Set<Technology> getTechnologies()
    {
        return propertyStore.getCollection(TECHNOLOGIES);
    }

    /**
     * @param technologies sets the {@link #TECHNOLOGIES}
     */
    private void setTechnologies(Set<Technology> technologies)
    {
        propertyStore.setCollection(TECHNOLOGIES, technologies);
    }

    /**
     * @param technology technology to be added to the {@link #TECHNOLOGIES}
     */
    public void addTechnology(Technology technology)
    {
        propertyStore.addCollectionItem(TECHNOLOGIES, technology);
    }

    /**
     * @param technology technology to be removed from the {@link #TECHNOLOGIES}
     */
    public void removeTechnology(Technology technology)
    {
        propertyStore.removeCollectionItem(TECHNOLOGIES, technology);
    }
}
