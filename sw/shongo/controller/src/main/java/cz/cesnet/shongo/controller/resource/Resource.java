package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.common.DateTime;
import cz.cesnet.shongo.common.Identifier;
import cz.cesnet.shongo.common.PersistentObject;
import cz.cesnet.shongo.common.Person;

import javax.persistence.*;
import java.util.*;

/**
 * Represents an entity that can be scheduled.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Resource extends PersistentObject
{
    /**
     * Represents a type of a resource.
     */
    public static enum Type
    {
        /**
         * Video/Web Conferencing Equipment
         */
        DEVICE,
        /**
         * Component for Device (e.g., alias, virtual room, etc.)
         */
        DEVICE_COMPONENT,
        /**
         * Physical room (e.g., boardroom, classroom)
         */
        PHYSICAL_ROOM
    }

    /**
     * Unique identifier in whole Shongo.
     */
    private Identifier identifier;

    /**
     * Type of a resource.
     */
    private Type type;

    /**
     * Name of a resource that is visible to users.
     */
    private String name;

    /**
     * Description for a resource.
     */
    private String description;

    /**
     * List of capabilities.
     */
    private List<Capability> capabilities = new ArrayList<Capability>();

    /**
     * Parent resource in which is this resource contained (e.g., endpoint can be located
     * in parent physical room resource).
     */
    private Resource parentResource;

    /**
     * List of child resources (e.g., physical room can contain some videoconferencing equipment).
     */
    private List<Resource> childResources = new ArrayList<Resource>();

    /**
     * List of persons that are contacted when are encountered any technical issues.
     */
    private List<Person> administrators = new ArrayList<Person>();

    /**
     * Defines a maximum future to which the resource is schedulable (e.g., can be set as relative date/time which
     * means that resource can be always scheduled only e.g., to four month ahead).
     */
    private DateTime maximumFuture;

    /**
     * Constructor.
     */
    Resource()
    {
    }

    /**
     * @return {@link #identifier} as string
     */
    @Column(name = "identifier")
    public String getIdentifierAsString()
    {
        return (identifier != null ? identifier.toString() : null);
    }

    /**
     * @param identifier Sets the {@link #identifier} from string
     */
    private void setIdentifierAsString(String identifier)
    {
        if (identifier != null) {
            this.identifier = new Identifier(identifier);
        }
        else {
            this.identifier = null;
        }
    }

    /**
     * @return {@link #identifier} object (stored in db as string by IdentifierAsString methods)
     */
    @Transient
    public Identifier getIdentifier()
    {
        return identifier;
    }

    /**
     * Create a new identifier for the resource.
     *
     * @param domain domain to which the resource belongs.
     */
    public void createNewIdentifier(String domain)
    {
        if (identifier != null) {
            throw new IllegalStateException("Resource has already created identifier!");
        }
        identifier = new Identifier(Identifier.Type.RESOURCE, domain);
    }

    /**
     * Change the identifier for the resource.
     *
     * @param domain
     */
    public void changeIdentifier(String domain)
    {
        if (identifier == null) {
            throw new IllegalStateException("Resource hasn't created identifier yet!");
        }
        throw new RuntimeException("TODO: Implement identifier change!");
    }

    /**
     * @return {@link #type}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public Type getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(Type type)
    {
        this.type = type;
    }

    /**
     * @return {@link #name}
     */
    @Column
    public String getName()
    {
        return name;
    }

    /**
     * @param name sets the {@link #name}
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return {@link #description}
     */
    @Column
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description sets the {@link #description}
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return {@link #capabilities}
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "resource")
    @Access(AccessType.FIELD)
    public List<Capability> getCapabilities()
    {
        return Collections.unmodifiableList(capabilities);
    }

    /**
     * @param capability capability to be added to the {@link #capabilities}
     */
    public void addCapability(Capability capability)
    {
        // Manage bidirectional association
        if (capabilities.contains(capability) == false) {
            capabilities.add(capability);
            capability.setResource(this);
        }
    }

    /**
     * @param capability capability to be removed from the {@link #capabilities}
     */
    public void removeCapability(Capability capability)
    {
        // Manage bidirectional association
        if (capabilities.contains(capability)) {
            capabilities.remove(capability);
            capability.setResource(null);
        }
    }

    /**
     * @return {@link #parentResource}
     */
    @ManyToOne
    @Access(AccessType.FIELD)
    public Resource getParentResource()
    {
        return parentResource;
    }

    /**
     * @param parentResource sets the {@link #parentResource}
     */
    public void setParentResource(Resource parentResource)
    {
        // Manage bidirectional association
        if ( parentResource != this.parentResource) {
            if ( this.parentResource != null ) {
                Resource oldParentResource = this.parentResource;
                this.parentResource = null;
                oldParentResource.removeChildResource(this);
            }
            if ( parentResource != null ) {
                this.parentResource = parentResource;
                this.parentResource.addChildResource(this);
            }
        }
    }

    /**
     * @return {@link #childResources}
     */
    @OneToMany(mappedBy = "parentResource")
    @Access(AccessType.FIELD)
    public List<Resource> getChildResources()
    {
        return childResources;
    }

    /**
     * @param resource resource to be added to the {@link #childResources}
     */
    public void addChildResource(Resource resource)
    {
        // Manage bidirectional association
        if (childResources.contains(resource) == false) {
            childResources.add(resource);
            resource.setParentResource(this);
        }
    }

    /**
     * @param resource resource to be removed from the {@link #childResources}
     */
    public void removeChildResource(Resource resource)
    {
        // Manage bidirectional association
        if (childResources.contains(resource)) {
            childResources.remove(resource);
            resource.setParentResource(null);
        }
    }

    /**
     * @return {@link #administrators}
     */
    @OneToMany
    @Access(AccessType.FIELD)
    public List<Person> getAdministrators()
    {
        return administrators;
    }

    /**
     * @param person person to be added to the {@link #administrators}
     */
    public void addAdministrator(Person person)
    {
        administrators.add(person);
    }

    /**
     * @param person person to be removed from the {@link #administrators}
     */
    public void removeAdministrator(Person person)
    {
        administrators.remove(person);
    }

    /**
     * @return {@link #maximumFuture}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public DateTime getMaximumFuture()
    {
        return maximumFuture;
    }

    /**
     * @param maximumFuture sets the {@link #maximumFuture}
     */
    public void setMaximumFuture(DateTime maximumFuture)
    {
        this.maximumFuture = maximumFuture;
    }

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        map.put("identifier", getIdentifierAsString());
        map.put("type", getType().toString());
        map.put("name", getName());
        map.put("description", getDescription());
        addCollectionToMap(map, "capabilities", capabilities);
    }
}