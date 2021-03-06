package cz.cesnet.shongo.controller.booking;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.ObjectType;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.resource.Resource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an object identifier.
 * The class also contains methods for parsing and formatting global identifiers.
 * <p/>
 * Identifiers are of following format:
 * <p/>
 * {@code shongo:<domain>:<object-type>:<object-id>}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ObjectIdentifier
{
    /**
     * {@link cz.cesnet.shongo.controller.ObjectType} of the identifier.
     */
    private ObjectType objectType;

    /**
     * Identifier value.
     */
    private Long persistenceId;

    /**
     * Constructor.
     */
    public ObjectIdentifier()
    {
        this.objectType = null;
        this.persistenceId = null;
    }

    /**
     * Constructor.
     *
     * @param objectType sets the {@link #objectType}
     */
    public ObjectIdentifier(ObjectType objectType)
    {
        this.objectType = objectType;
        this.persistenceId = null;
    }

    /**
     * Constructor.
     *
     * @param objectType    sets the {@link #objectType}
     * @param persistenceId sets the {@link #persistenceId}
     */
    public ObjectIdentifier(ObjectType objectType, Long persistenceId)
    {
        this.objectType = objectType;
        this.persistenceId = persistenceId;
    }

    /**
     * Constructor.
     *
     * @param persistentObject sets the {@link #objectType} and the {@link #persistenceId}
     */
    public ObjectIdentifier(PersistentObject persistentObject)
    {
        this.objectType = ObjectTypeResolver.getObjectType(persistentObject.getClass());
        this.persistenceId = persistentObject.getId();
    }

    /**
     * @return {@link #objectType}
     */
    public ObjectType getObjectType()
    {
        return objectType;
    }

    /**
     * @param objectType sets the {@link #objectType}
     */
    public void setObjectType(ObjectType objectType)
    {
        this.objectType = objectType;
    }

    /**
     * @return true whether {@link #objectType} is empty or {@link #persistenceId} is empty,
     *         false otherwise
     */
    public boolean isGroup()
    {
        return objectType == null || persistenceId == null;
    }

    /**
     * @return {@link #persistenceId}
     */
    public Long getPersistenceId()
    {
        return persistenceId;
    }

    /**
     * @param persistenceId sets the {@link #persistenceId}
     */
    public void setPersistenceId(Long persistenceId)
    {
        this.persistenceId = persistenceId;
    }

    /**
     * @return class for the {@link #objectType}
     */
    public Class<? extends PersistentObject> getObjectClass()
    {
        return ObjectTypeResolver.getObjectTypeClass(objectType);
    }

    /**
     * @return global identifier
     */
    public String toId()
    {
        return formatId(objectType, persistenceId);
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        ObjectIdentifier that = (ObjectIdentifier) object;

        if (objectType != that.objectType) {
            return false;
        }
        if (persistenceId != null ? !persistenceId.equals(that.persistenceId) : that.persistenceId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = objectType != null ? objectType.hashCode() : 0;
        result = 31 * result + (persistenceId != null ? persistenceId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return toId();
    }

    /**
     * @param objectClass to be checked
     * @return true if given {@code objectClass} is bound to any object type,
     *         false otherwise
     */
    public static boolean isAvailableForObjectType(Class<? extends PersistentObject> objectClass)
    {
        try {
            ObjectTypeResolver.getObjectType(objectClass);
            return true;
        }
        catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * @param objectId object identifier
     * @return parsed {@link ObjectIdentifier}
     */
    public static ObjectIdentifier parse(String objectId)
    {
        if (objectId == null) {
            return null;
        }
        Matcher matcher = LOCAL_TYPE_IDENTIFIER_PATTERN.matcher(objectId);
        if (matcher.matches()) {
            return new ObjectIdentifier(ObjectType.getByCode(matcher.group(1)), parsePersistenceId(matcher.group(2)));
        }
        return parse(Domain.getLocalDomainName(), objectId);
    }

    /**
     * @param objectId   object identifier
     * @param objectType
     * @return parsed {@link ObjectIdentifier}
     */
    public static ObjectIdentifier parse(String objectId, ObjectType objectType)
    {
        return parse(Domain.getLocalDomainName(), objectType, objectId);
    }

    /**
     * @param objectClass   object type for the identifier
     * @param objectLocalId object local id for the identifier
     * @return object global identifier
     */
    public static String formatId(Class<? extends PersistentObject> objectClass, Long objectLocalId)
    {
        return formatId(Domain.getLocalDomainName(), objectClass, objectLocalId);
    }

    /**
     * @param objectType    object type for the identifier
     * @param objectLocalId object local id for the identifier
     * @return object global identifier
     */
    public static String formatId(ObjectType objectType, Long objectLocalId)
    {
        return formatId(Domain.getLocalDomainName(), objectType, objectLocalId);
    }

    /**
     * @param objectType    object type for the identifier
     * @param objectLocalId object local id for the identifier
     * @return object global identifier
     */
    public static String formatId(ObjectType objectType, String objectLocalId)
    {
        return formatId(Domain.getLocalDomainName(), objectType, objectLocalId);
    }

    /**
     * @param objectClass   object type for the identifier
     * @param objectLocalId object local id for the identifier
     * @return object global identifier
     */
    public static String formatId(Class<? extends PersistentObject> objectClass, String objectLocalId)
    {
        return formatId(Domain.getLocalDomainName(), objectClass, objectLocalId);
    }

    /**
     * @param object for which the global identifier should be formatted
     * @return given {@code object} global identifier.
     */
    public static String formatId(PersistentObject object)
    {
        return formatId(object.getClass(), object.getId());
    }

    /**
     * @param resource for which the global identifier should be formatted
     * @return given {@code resource} global identifier.
     */
    public static String formatId(Resource resource)
    {
        resource.checkPersisted();
        return formatId(Domain.getLocalDomainName(), Resource.class, resource.getId());
    }

    /**
     * @param reservationRequest for which the global identifier should be formatted
     * @return given {@code resource} global identifier.
     */
    public static String formatId(AbstractReservationRequest reservationRequest)
    {
        reservationRequest.checkPersisted();
        return formatId(Domain.getLocalDomainName(), AbstractReservationRequest.class,
                reservationRequest.getId());
    }

    /**
     * @param reservation for which the global identifier should be formatted
     * @return given {@code resource} global identifier.
     */
    public static String formatId(Reservation reservation)
    {
        reservation.checkPersisted();
        return formatId(Domain.getLocalDomainName(), Reservation.class, reservation.getId());
    }

    /**
     * @param executable for which the global identifier should be formatted
     * @return given {@code resource} global identifier.
     */
    public static String formatId(Executable executable)
    {
        executable.checkPersisted();
        return formatId(Domain.getLocalDomainName(), Executable.class, executable.getId());
    }

    /**
     * @param objectId   object local id for the identifier
     * @param objectType object type for the identifier
     * @return parsed local identifier from given global or local identifier
     */
    public static Long parseId(String objectId, ObjectType objectType)
    {
        return parseId(Domain.getLocalDomainName(), objectType, objectId);
    }

    /**
     * @param objectClass object type for the identifier
     * @param objectId    object local id for the identifier
     * @return parsed local identifier from given global or local identifier
     */
    public static Long parseId(String objectId, Class<? extends PersistentObject> objectClass)
    {
        ObjectType objectType = ObjectTypeResolver.getObjectType(objectClass);
        return parseId(Domain.getLocalDomainName(), objectType, objectId);
    }

    /**
     * Local identifier pattern.
     */
    private static Pattern LOCAL_IDENTIFIER_PATTERN = Pattern.compile("\\d+|\\*");

    /**
     * Local identifier pattern with type.
     */
    private static Pattern LOCAL_TYPE_IDENTIFIER_PATTERN = Pattern.compile("([a-z]+|\\*):(\\d+|\\*)");

    /**
     * Global identifier pattern.
     */
    private static Pattern GLOBAL_IDENTIFIER_PATTERN = Pattern.compile("shongo:(.+):([a-z]+|\\*):(\\d+|\\*)");

    /**
     * @param persistenceId to be parsed
     * @return parsed persistence id
     */
    private static Long parsePersistenceId(String persistenceId)
    {
        if (persistenceId.equals("*")) {
            return null;
        }
        return Long.parseLong(persistenceId);
    }

    /**
     * @param domain   required domain
     * @param objectId
     * @return {@link ObjectIdentifier} parsed from given {@code objectId}
     */
    private static ObjectIdentifier parse(String domain, String objectId)
            throws ControllerReportSet.IdentifierInvalidException,
                   ControllerReportSet.IdentifierInvalidDomainException
    {
        Matcher matcher = GLOBAL_IDENTIFIER_PATTERN.matcher(objectId);
        if (!matcher.matches()) {
            throw new ControllerReportSet.IdentifierInvalidException(objectId);
        }
        if (!domain.equals(matcher.group(1))) {
            throw new ControllerReportSet.IdentifierInvalidDomainException(objectId, domain);
        }
        ObjectType objectType = ObjectType.getByCode(matcher.group(2));
        return new ObjectIdentifier(objectType, parsePersistenceId(matcher.group(3)));
    }

    /**
     * @param domain     required domain
     * @param objectType
     * @param objectId
     * @return {@link ObjectIdentifier} parsed from given {@code objectId}
     */
    private static ObjectIdentifier parse(String domain, ObjectType objectType, String objectId)
            throws ControllerReportSet.IdentifierInvalidException,
                   ControllerReportSet.IdentifierInvalidDomainException,
                   ControllerReportSet.IdentifierInvalidTypeException
    {
        if (LOCAL_IDENTIFIER_PATTERN.matcher(objectId).matches()) {
            return new ObjectIdentifier(objectType, parsePersistenceId(objectId));
        }
        ObjectIdentifier objectIdentifier = parse(domain, objectId);
        if (objectIdentifier.objectType != objectType) {
            throw new ControllerReportSet.IdentifierInvalidTypeException(objectId, objectType.getCode());
        }
        return objectIdentifier;
    }

    /**
     * @param domain      domain name for the identifier
     * @param objectType  object type for the identifier
     * @param objectId    object local id for the identifier
     * @return parsed local identifier from given global or local identifier
     */
    private static Long parseId(String domain, ObjectType objectType, String objectId)
            throws ControllerReportSet.IdentifierInvalidException,
                   ControllerReportSet.IdentifierInvalidDomainException,
                   ControllerReportSet.IdentifierInvalidTypeException
    {
        if (objectId == null) {
            return null;
        }
        if (LOCAL_IDENTIFIER_PATTERN.matcher(objectId).matches()) {
            return parsePersistenceId(objectId);
        }
        ObjectIdentifier objectIdentifier = parse(domain, objectId);
        if (objectIdentifier.objectType != objectType) {
            throw new ControllerReportSet.IdentifierInvalidTypeException(objectId, objectType.getCode());
        }
        return objectIdentifier.persistenceId;
    }

    /**
     * @param domain        domain name for the identifier
     * @param objectClass   object type for the identifier
     * @param objectLocalId object local id for the identifier
     * @return object global identifier.
     */
    private static String formatId(String domain, Class<? extends PersistentObject> objectClass, Long objectLocalId)
    {
        return formatId(domain, objectClass, (objectLocalId == null ? null : objectLocalId.toString()));
    }

    /**
     * @param domain        domain name for the identifier
     * @param objectType    object type for the identifier
     * @param objectLocalId object local id for the identifier
     * @return object global identifier.
     */
    private static String formatId(String domain, ObjectType objectType, Long objectLocalId)
    {
        return formatId(domain, objectType, (objectLocalId == null ? null : objectLocalId.toString()));
    }

    /**
     * @param domain        domain name for the identifier
     * @param objectClass   object type for the identifier
     * @param objectLocalId object local id for the identifier
     * @return object global identifier.
     */
    private static String formatId(String domain, Class<? extends PersistentObject> objectClass, String objectLocalId)
    {
        return formatId(domain, ObjectTypeResolver.getObjectType(objectClass), objectLocalId);
    }

    /**
     * @param domain        domain name for the identifier
     * @param objectType    object type for the identifier
     * @param objectLocalId object local id for the identifier
     * @return object global identifier.
     */
    private static String formatId(String domain, ObjectType objectType, String objectLocalId)
    {
        return String.format("shongo:%s:%s:%s", domain,
                (objectType == null ? "*" : objectType.getCode()),
                (objectLocalId == null ? "*" : objectLocalId));
    }
}

