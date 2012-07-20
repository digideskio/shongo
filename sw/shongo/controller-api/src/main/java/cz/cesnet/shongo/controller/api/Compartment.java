package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Technology;

import java.util.HashMap;
import java.util.List;

/**
 * Represents a requested compartment in reservation request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Compartment extends IdentifiedChangeableObject
{
    /**
     * Map that represents a resource specification.
     */
    public static class ResourceSpecificationMap extends HashMap<String, Object>
    {
    }

    /**
     * Collection of requested persons for the compartment.
     */
    public final String PERSONS = "persons";

    /**
     * Collection of requested reosurces for the compartment.
     */
    public final String RESOURCES = "resources";

    /**
     * @return {@link #PERSONS}
     */
    public List<Person> getPersons()
    {
        return getPropertyStorage().getCollection(PERSONS, List.class);
    }

    /**
     * @param persons sets the {@link #PERSONS}
     */
    public void setPersons(List<Person> persons)
    {
        getPropertyStorage().setCollection(PERSONS, persons);
    }

    /**
     * Adds new person to the {@link #PERSONS}.
     *
     * @param name
     * @param email
     */
    public void addPerson(String name, String email)
    {
        getPropertyStorage().addCollectionItem(PERSONS, new Person(name, email), List.class);
    }

    /**
     * @return {@link #RESOURCES}
     */
    public List<ResourceSpecificationMap> getResources()
    {
        return getPropertyStorage().getCollection(RESOURCES, List.class);
    }

    /**
     * @param resources {@link #RESOURCES}
     */
    public void setResources(List<ResourceSpecificationMap> resources)
    {
        getPropertyStorage().setCollection(RESOURCES, resources);
    }

    /**
     * Adds new external resources definition.
     *
     * @param technology
     * @param count
     * @param persons
     */
    public void addResource(Technology technology, int count, Person[] persons)
    {
        ResourceSpecificationMap resourceSpecificationMap = new ResourceSpecificationMap();
        resourceSpecificationMap.put("technology", technology);
        resourceSpecificationMap.put("count", count);
        resourceSpecificationMap.put("persons", persons);
        getPropertyStorage().addCollectionItem(RESOURCES, resourceSpecificationMap, List.class);
    }
}
