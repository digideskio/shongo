package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.ObjectType;
import cz.cesnet.shongo.controller.acl.AclEntry;
import cz.cesnet.shongo.controller.acl.AclObjectClass;
import cz.cesnet.shongo.controller.acl.AclObjectIdentity;
import cz.cesnet.shongo.controller.booking.ObjectTypeResolver;

import java.util.*;

/**
 * Represents an user state in the {@link AuthorizationCache}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AclUserState
{
    /**
     * Set of {@link AclEntry}s for the user.
     */
    private Map<Long, AclEntry> aclEntries = new HashMap<Long, AclEntry>();

    /**
     * {@link cz.cesnet.shongo.controller.authorization.AclUserState.ObjectState} for the user.
     */
    private Map<AclObjectIdentity, ObjectState> objectStateByObjectIdentity =
            new HashMap<AclObjectIdentity, ObjectState>();

    /**
     * Map of objects which are accessible to the user (he has {@link ObjectPermission#READ} for them) by {@link AclObjectClass}.
     */
    private Map<AclObjectClass, Set<Long>> accessibleObjectsByClass =
            new HashMap<AclObjectClass, Set<Long>>();

    /**
     * Map of objects which are owned by the user (he has {@link ObjectRole#OWNER} for them) by {@link AclObjectClass}.
     */
    private Map<AclObjectClass, Set<Long>> ownedObjectsByClass =
            new HashMap<AclObjectClass, Set<Long>>();

    /**
     * @param aclEntry to be added to the {@link AclUserState}
     */
    public synchronized void addAclEntry(AclEntry aclEntry)
    {
        Long aclEntryId = aclEntry.getId();
        AclObjectIdentity objectIdentity = aclEntry.getObjectIdentity();
        if (aclEntries.put(aclEntryId, aclEntry) == null) {
            ObjectState objectState = objectStateByObjectIdentity.get(objectIdentity);
            if (objectState == null) {
                objectState = new ObjectState();
                objectStateByObjectIdentity.put(objectIdentity, objectState);
            }

            // Update records
            objectState.aclEntries.put(aclEntryId, aclEntry);

            // Update roles
            ObjectRole objectRole = ObjectRole.valueOf(aclEntry.getRole());
            objectState.roles.add(objectRole);

            // Update permissions
            AclObjectClass objectClass = objectIdentity.getObjectClass();
            ObjectType objectType = ObjectTypeResolver.getObjectType(objectClass);
            for (ObjectPermission objectPermission : objectType.getRolePermissions(objectRole)) {
                objectState.permissions.add(objectPermission);
            }

            // Update owned entities
            if (objectState.roles.contains(ObjectRole.OWNER)) {
                Set<Long> entities = ownedObjectsByClass.get(objectClass);
                if (entities == null) {
                    entities = new HashSet<Long>();
                    ownedObjectsByClass.put(objectClass, entities);
                }
                entities.add(objectIdentity.getObjectId());
            }
            // Update accessible entities
            if (objectState.permissions.contains(ObjectPermission.READ)) {
                Set<Long> entities = accessibleObjectsByClass.get(objectClass);
                if (entities == null) {
                    entities = new HashSet<Long>();
                    accessibleObjectsByClass.put(objectClass, entities);
                }
                entities.add(objectIdentity.getObjectId());
            }
        }
    }

    /**
     * @param aclEntry to be removed from the {@link AclUserState}
     */
    public synchronized void removeAclEntry(AclEntry aclEntry)
    {
        Long aclEntryId = aclEntry.getId();
        if (aclEntries.remove(aclEntryId) != null) {
            AclObjectIdentity objectIdentity = aclEntry.getObjectIdentity();
            ObjectState objectState = objectStateByObjectIdentity.get(objectIdentity);
            if (objectState == null) {
                return;
            }

            // Update records
            objectState.aclEntries.remove(aclEntryId);

            // Update roles and permissions
            objectState.roles.clear();
            objectState.permissions.clear();
            AclObjectClass objectClass = objectIdentity.getObjectClass();
            ObjectType objectType = ObjectTypeResolver.getObjectType(objectClass);
            for (AclEntry existingAclEntry : objectState.aclEntries.values()) {
                ObjectRole objectRole = ObjectRole.valueOf(existingAclEntry.getRole());
                objectState.roles.add(objectRole);
                for (ObjectPermission objectPermission : objectType.getRolePermissions(objectRole)) {
                    objectState.permissions.add(objectPermission);
                }
            }

            // Update owned entities
            if (!objectState.roles.contains(ObjectRole.OWNER)) {
                Set<Long> entities = ownedObjectsByClass.get(objectClass);
                if (entities != null) {
                    entities.remove(objectIdentity.getObjectId());
                    if (entities.size() == 0) {
                        ownedObjectsByClass.remove(objectClass);
                    }
                }
            }
            // Update accessible entities
            if (!objectState.permissions.contains(ObjectPermission.READ)) {
                Set<Long> entities = accessibleObjectsByClass.get(objectClass);
                if (entities != null) {
                    entities.remove(objectIdentity.getObjectId());
                    if (entities.size() == 0) {
                        accessibleObjectsByClass.remove(objectClass);
                    }
                }
            }

            // Remove objects states
            if (objectState.aclEntries.size() == 0) {
                objectStateByObjectIdentity.remove(objectIdentity);
            }
        }
    }

    /**
     * @param objectIdentity for which the {@link AclEntry}s should be returned
     * @return set of {@link AclEntry}s for given {@code objectIdentity}
     */
    public synchronized Collection<AclEntry> getAclEntries(AclObjectIdentity objectIdentity)
    {
        ObjectState objectState = objectStateByObjectIdentity.get(objectIdentity);
        if (objectState != null) {
            return objectState.aclEntries.values();
        }
        return null;
    }

    /**
     * @param objectIdentity for which the {@link ObjectRole}s should be returned
     * @return set of {@link ObjectRole} for given {@code objectIdentity}
     */
    public synchronized Set<ObjectRole> getObjectRoles(AclObjectIdentity objectIdentity)
    {
        ObjectState objectState = objectStateByObjectIdentity.get(objectIdentity);
        if (objectState != null) {
            return Collections.unmodifiableSet(objectState.roles);
        }
        return null;
    }

    /**
     * @param objectIdentity for which the {@link ObjectPermission}s should be returned
     * @return set of {@link ObjectPermission} for given {@code objectIdentity}
     */
    public synchronized Set<ObjectPermission> getObjectPermissions(AclObjectIdentity objectIdentity)
    {
        ObjectState objectState = objectStateByObjectIdentity.get(objectIdentity);
        if (objectState != null) {
            return Collections.unmodifiableSet(objectState.permissions);
        }
        return null;
    }

    /**
     * @param objectIdentity for which the should be checked
     * @return true if the user has given {@code objectRole} for the object,
     *         false otherwise
     */
    public synchronized boolean hasObjectRole(AclObjectIdentity objectIdentity, ObjectRole objectRole)
    {
        ObjectState objectState = objectStateByObjectIdentity.get(objectIdentity);
        return objectState != null && objectState.roles.contains(objectRole);
    }

    /**
     * @param objectIdentity for which the should be checked
     * @return true if the user has given {@code objectPermission} for the object,
     *         false otherwise
     */
    public synchronized boolean hasObjectPermission(AclObjectIdentity objectIdentity, ObjectPermission objectPermission)
    {
        ObjectState objectState = objectStateByObjectIdentity.get(objectIdentity);
        return objectState != null && objectState.permissions.contains(objectPermission);
    }

    /**
     * @param objectClass of which the {@link AclObjectIdentity#objectId}s should be returned
     * @param objectRole which the user must have for the returned {@link AclObjectIdentity#objectId}s
     * @return set of {@link AclObjectIdentity#objectId}s of given {@code objectClass} for which the user has given {@code objectRole}
     */
    public Set<Long> getObjectsByRole(AclObjectClass objectClass, ObjectRole objectRole)
    {
        if (!objectRole.equals(ObjectRole.OWNER)) {
            throw new TodoImplementException(objectRole);
        }
        Set<Long> entities = ownedObjectsByClass.get(objectClass);
        if (entities != null) {
            return Collections.unmodifiableSet(entities);
        }
        return null;
    }

    /**
     * @param objectClass of which the {@link AclObjectIdentity#objectId}s should be returned
     * @param objectPermission which the user must have for the returned {@link AclObjectIdentity#objectId}s
     * @return set of {@link AclObjectIdentity#objectId}s of given {@code objectClass} for which the user has given {@code objectPermission}
     */
    public synchronized Set<Long> getObjectsByPermission(AclObjectClass objectClass, ObjectPermission objectPermission)
    {
        if (!objectPermission.equals(ObjectPermission.READ)) {
            throw new TodoImplementException(objectPermission);
        }
        Set<Long> entities = accessibleObjectsByClass.get(objectClass);
        if (entities != null) {
            return Collections.unmodifiableSet(entities);
        }
        return null;
    }

    /**
     * Represents an object state for the user.
     */
    private static class ObjectState
    {
        /**
         * Set of {@link AclEntry}s for the object.
         */
        private Map<Long, AclEntry> aclEntries = new HashMap<Long, AclEntry>();

        /**
         * Set of {@link ObjectRole}s for the object.
         */
        private Set<ObjectRole> roles = new HashSet<ObjectRole>();

        /**
         * Set of {@link ObjectPermission}s for the object.
         */
        private Set<ObjectPermission> permissions = new HashSet<ObjectPermission>();
    }
}
