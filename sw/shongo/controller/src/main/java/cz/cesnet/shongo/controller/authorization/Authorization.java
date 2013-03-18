package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.CommonFaultSet;
import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.EntityType;
import cz.cesnet.shongo.controller.Permission;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.common.UserPerson;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.FaultRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * Provides methods for performing authentication and authorization.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class Authorization
{
    private static Logger logger = LoggerFactory.getLogger(Authorization.class);

    /**
     * Root user-id.
     */
    public static final String ROOT_USER_ID = "0";

    /**
     * Root user {@link cz.cesnet.shongo.controller.common.Person}.
     */
    public static final UserInformation ROOT_USER_INFORMATION;

    /**
     * Static initialization.
     */
    static {
        ROOT_USER_INFORMATION = new UserInformation();
        ROOT_USER_INFORMATION.setUserId(ROOT_USER_ID);
        ROOT_USER_INFORMATION.setFirstName("root");
    }

    /**
     * @see EntityManagerFactory
     */
    protected EntityManagerFactory entityManagerFactory;

    /**
     * @see AuthorizationCache
     */
    protected AuthorizationCache cache = new AuthorizationCache();

    /**
     * Constructor.
     *
     * @param config to load authorization configuration from
     */
    protected Authorization(Configuration config)
    {
        cache.setUserIdExpiration(config.getDuration(Configuration.SECURITY_EXPIRATION_USER_ID));
        cache.setUserInformationExpiration(config.getDuration(Configuration.SECURITY_EXPIRATION_USER_INFORMATION));
        cache.setAclExpiration(config.getDuration(Configuration.SECURITY_EXPIRATION_ACL));
    }

    /**
     * Destroy this {@link Authorization} (and you be able to create another {@link Authorization} instance again)
     */
    public void destroy()
    {
        authorization = null;
    }

    /**
     * @param entityManagerFactory sets the {@link #entityManagerFactory}
     */
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * Validate given {@code securityToken}.
     *
     * @param securityToken to be validated
     * @return user-id
     * @throws FaultException when the validation fails
     */
    public final String validate(SecurityToken securityToken) throws FaultException
    {
        // Check not empty
        if (securityToken == null || securityToken.getAccessToken() == null) {
            throw new SecurityException(
                    SecurityToken.class.getSimpleName() + " should not be empty.");
        }
        return onValidate(securityToken);
    }

    /**
     * Retrieve {@link UserInformation} for given {@code securityToken}.
     *
     * @param securityToken of an user
     * @return {@link UserInformation} for the user with given {@code securityToken}
     * @throws FaultException when the {@link UserInformation} cannot be retrieved
     */
    public final UserInformation getUserInformation(SecurityToken securityToken) throws FaultException
    {
        UserInformation userInformation = securityToken.getCachedUserInformation();
        if (userInformation != null) {
            return userInformation;
        }
        String accessToken = securityToken.getAccessToken();

        // Try to use the user-id from access token cache to get the user information
        String userId = cache.getUserIdByAccessToken(accessToken);
        if (userId != null) {
            logger.debug("Using cached user-id '{}' for access token '{}'...", userId, accessToken);
            userInformation = getUserInformation(userId);

            // Store the user information inside the security token
            securityToken.setCachedUserInformation(userInformation);

            return userInformation;
        }
        else {
            logger.debug("Retrieving user information by access token '{}'...", accessToken);

            userInformation = onGetUserInformationByAccessToken(accessToken);
            userId = userInformation.getUserId();
            cache.putUserIdByAccessToken(accessToken, userId);
            cache.putUserInformationByUserId(userId, userInformation);

            // Store the user information inside the security token
            securityToken.setCachedUserInformation(userInformation);

            return userInformation;
        }
    }

    /**
     * Retrieve {@link UserInformation} for given {@code userId}.
     *
     * @param userId of an user
     * @return {@link UserInformation} for the user with given {@code userId}
     * @throws FaultRuntimeException when the {@link UserInformation} cannot be retrieved
     */
    public final UserInformation getUserInformation(String userId)
    {
        // Root user
        if (userId.equals(ROOT_USER_ID)) {
            return ROOT_USER_INFORMATION;
        }

        // Try to use the user information from the cache
        UserInformation userInformation = cache.getUserInformationByUserId(userId);
        if (userInformation != null) {
            logger.debug("Using cached user information for user-id '{}'...", userId);
            return userInformation;
        }
        else {
            logger.debug("Retrieving user information by user-id '{}'...", userId);

            try {
                userInformation = onGetUserInformationByUserId(userId);
            }
            catch (FaultException exception) {
                throw new FaultRuntimeException(exception.getFault());
            }
            cache.putUserInformationByUserId(userId, userInformation);
            return userInformation;
        }
    }

    /**
     * Retrieve a {@link UserPerson} by given {@code userId}.
     *
     * @param userId of an user
     * @return {@link UserInformation} for the user with given {@code userId}
     * @throws FaultRuntimeException when the {@link UserInformation} cannot be retrieved
     */
    public final UserPerson getUserPerson(String userId)
    {
        return new UserPerson(userId, getUserInformation(userId));
    }

    /**
     * Retrieve all {@link UserInformation}s.
     *
     * @return collection of {@link UserInformation}s
     * @throws FaultException when the {@link UserInformation}s cannot be retrieved
     */
    public final Collection<UserInformation> listUserInformation() throws FaultException
    {
        logger.debug("Retrieving list of user information...");

        return onListUserInformation();
    }


    protected abstract AclRecord onCreateAclRecord(String userId, EntityIdentifier entityId, Role role)
            throws FaultException;

    protected abstract void onDeleteAclRecord(AclRecord aclRecord)
            throws FaultException;

    protected abstract AclRecord onGetAclRecord(String aclRecordId) throws FaultException;

    protected abstract Collection<AclRecord> onListAclRecords(String userId, EntityIdentifier entityId, Role role)
            throws FaultException;

    public AclRecord createAclRecord(String userId, EntityIdentifier entityId, Role role) throws FaultException
    {
        EntityType entityType = entityId.getEntityType();
        if (!entityType.allowsRole(role)) {
            CommonFaultSet.throwSecurityErrorFault("Role is not allowed to specified entity");
        }
        AclRecord newAclRecord = onCreateAclRecord(userId, entityId, role);

        // Update AclUserState cache
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState == null) {
            aclUserState = onFetchAclUserState(userId);
            cache.putAclUserStateByUserId(userId, aclUserState);
        }
        AclRecord addedAclRecord = aclUserState.addAclRecord(newAclRecord);
        // ACL already exists so return it
        if (addedAclRecord != newAclRecord) {
            return addedAclRecord;
        }

        // Update AclRecord cache
        cache.putAclRecordById(newAclRecord);

        // Update AclEntityState cache
        AclEntityState aclEntityState = cache.getAclEntityStateByEntityId(entityId);
        if (aclEntityState == null) {
            aclEntityState = onFetchAclEntityState(entityId);
            cache.putAclEntityStateByEntityId(entityId, aclEntityState);
        }
        aclEntityState.addAclRecord(newAclRecord);

        return newAclRecord;
    }

    public void deleteAclRecord(AclRecord aclRecord) throws FaultException
    {
        onDeleteAclRecord(aclRecord);

        // Update AclRecord cache
        cache.removeAclRecordById(aclRecord);

        // Update AclUserState cache
        String userId = aclRecord.getUserId();
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState == null) {
            aclUserState = onFetchAclUserState(userId);
            cache.putAclUserStateByUserId(userId, aclUserState);
        }
        aclUserState.removeAclRecord(aclRecord);

        // Update AclEntityState cache
        EntityIdentifier entityId = aclRecord.getEntityId();
        AclEntityState aclEntityState = cache.getAclEntityStateByEntityId(entityId);
        if (aclEntityState == null) {
            aclEntityState = onFetchAclEntityState(entityId);
            cache.putAclEntityStateByEntityId(entityId, aclEntityState);
        }
        aclEntityState.removeAclRecord(aclRecord);
    }

    public AclRecord getAclRecord(String aclRecordId) throws FaultException
    {
        AclRecord aclRecord = cache.getAclRecordById(aclRecordId);
        if (aclRecord == null) {
            aclRecord = onGetAclRecord(aclRecordId);
            cache.putAclRecordById(aclRecord);
        }
        return aclRecord;
    }

    public Collection<AclRecord> getAclRecords(String userId, EntityIdentifier entityId) throws FaultException
    {
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState == null) {
            aclUserState = onFetchAclUserState(userId);
            cache.putAclUserStateByUserId(userId, aclUserState);
        }
        Set<AclRecord> aclRecords = aclUserState.getAclRecords(entityId);
        if (aclRecords == null) {
            return Collections.emptySet();
        }
        return aclRecords;
    }

    public Collection<AclRecord> getAclRecords(EntityIdentifier entityId) throws FaultException
    {
        AclEntityState aclEntityState = cache.getAclEntityStateByEntityId(entityId);
        if (aclEntityState == null) {
            aclEntityState = onFetchAclEntityState(entityId);
            cache.putAclEntityStateByEntityId(entityId, aclEntityState);
        }
        Set<AclRecord> aclRecords = aclEntityState.getAclRecords();
        if (aclRecords == null) {
            return Collections.emptySet();
        }
        return aclRecords;
    }

    public final Collection<AclRecord> getAclRecords(String userId, EntityIdentifier entityId, Role role)
            throws FaultException
    {
        return onListAclRecords(userId, entityId, role);
    }

    public void checkPermission(String userId, EntityIdentifier entityId, Permission permission)
            throws FaultException
    {
        Set<Permission> permissions = getPermissions(userId, entityId);
        if (!permissions.contains(permission)) {
            CommonFaultSet.throwSecurityErrorFault(
                    String.format("User with id '%s' doesn't have '%s' permission for the '%s'",
                            userId, permission.getCode(), entityId));
        }
    }

    public Set<Permission> getPermissions(String userId, EntityIdentifier entityId) throws FaultException
    {
        if (userId.equals(ROOT_USER_ID)) {
            // Root user has all possible permissions
            EntityType entityType = entityId.getEntityType();
            return entityType.getPermissions();
        }
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState == null) {
            aclUserState = onFetchAclUserState(userId);
            cache.putAclUserStateByUserId(userId, aclUserState);
        }
        Set<Permission> permissions = aclUserState.getPermissions(entityId);
        if (permissions == null) {
            return Collections.emptySet();
        }
        return permissions;
    }

    public Set<Long> getEntitiesWithPermission(String userId, EntityType entityType, Permission permission)
            throws FaultException
    {
        if (userId.equals(ROOT_USER_ID)) {
            return null;
        }
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState == null) {
            aclUserState = onFetchAclUserState(userId);
            cache.putAclUserStateByUserId(userId, aclUserState);
        }
        Set<Long> entities = aclUserState.getEntitiesByPermission(entityType, permission);
        if (entities == null) {
            return Collections.emptySet();
        }
        return entities;
    }

    public Set<String> getUserIdsWithRole(PersistentObject persistentObject, Role role)
    {
        EntityIdentifier entityId = new EntityIdentifier(persistentObject);
        AclEntityState aclEntityState = cache.getAclEntityStateByEntityId(entityId);
        if (aclEntityState == null) {
            try {
                aclEntityState = onFetchAclEntityState(entityId);
            }
            catch (FaultException exception) {
                throw new FaultRuntimeException(exception.getFault());
            }
            cache.putAclEntityStateByEntityId(entityId, aclEntityState);
        }
        Set<String> userIds = aclEntityState.getUserIdsByRole(role);
        if (userIds == null) {
            return Collections.emptySet();
        }
        return userIds;
    }

    public Collection<UserInformation> getUsersWithRole(PersistentObject persistentObject, Role role)
    {
        List<UserInformation> users = new LinkedList<UserInformation>();
        for (String userId : getUserIdsWithRole(persistentObject, role)) {
            users.add(getUserInformation(userId));
        }
        return users;
    }

    /**
     * Validate given {@code securityToken}.
     *
     * @param securityToken to be validated
     * @return user-id
     * @throws FaultException when the validation fails
     */
    protected String onValidate(SecurityToken securityToken) throws FaultException
    {
        // Validate access token by getting user info
        try {
            UserInformation userInformation = getUserInformation(securityToken);
            logger.debug("Access token '{}' is valid for {} (id: {}).",
                    new Object[]{securityToken.getAccessToken(), userInformation.getFullName(),
                            userInformation.getUserId()
                    });
            return userInformation.getUserId();
        }
        catch (IllegalStateException exception) {
            return CommonFaultSet.throwSecurityErrorFault(String.format("Access token '%s' cannot be validated. %s",
                    securityToken.getAccessToken(), exception.getMessage()));
        }
    }

    /**
     * Fetch {@link AclUserState} for given {@code userId}.
     *
     * @param userId of user for which the ACL should be fetched
     * @return fetched {@link AclUserState} for given {@code userId}
     */
    protected AclUserState onFetchAclUserState(String userId) throws FaultException
    {
        AclUserState aclUserState = new AclUserState();
        for (AclRecord aclRecord : onListAclRecords(userId, null, null)) {
            aclUserState.addAclRecord(aclRecord);
            cache.putAclRecordById(aclRecord);
        }
        return aclUserState;
    }

    /**
     * Fetch {@link AclEntityState} for given {@code entityId}.
     *
     * @param entityId of entity for which the ACL should be fetched
     * @return fetched {@link AclEntityState} for given {@code entityId}
     */
    protected AclEntityState onFetchAclEntityState(EntityIdentifier entityId) throws FaultException
    {
        AclEntityState aclEntityState = new AclEntityState();
        for (AclRecord aclRecord : onListAclRecords(null, entityId, null)) {
            aclEntityState.addAclRecord(aclRecord);
            cache.putAclRecordById(aclRecord);
        }
        return aclEntityState;
    }

    /**
     * Retrieve {@link UserInformation} for given {@code accessToken}.
     *
     * @param accessToken of an user
     * @return {@link UserInformation} for the user with given {@code accessToken}
     * @throws FaultException when the {@link UserInformation} cannot be retrieved
     */
    protected abstract UserInformation onGetUserInformationByAccessToken(String accessToken) throws FaultException;

    /**
     * Retrieve {@link UserInformation} for given {@code userId}.
     *
     * @param userId of an user
     * @return {@link UserInformation} for the user with given {@code userId}
     * @throws FaultException when the {@link UserInformation} cannot be retrieved
     */
    protected abstract UserInformation onGetUserInformationByUserId(String userId) throws FaultException;

    /**
     * Retrieve all {@link UserInformation}s.
     *
     * @return collection of {@link UserInformation}s
     * @throws FaultException when the {@link UserInformation}s cannot be retrieved
     */
    protected abstract Collection<UserInformation> onListUserInformation() throws FaultException;

    /**
     * Single instance of {@link Authorization}.
     */
    protected static Authorization authorization;

    /**
     * @param authorization sets the {@link #authorization}
     * @throws IllegalStateException when the {@link #authorization} is already set
     */
    protected static void setInstance(Authorization authorization)
    {
        if (Authorization.authorization != null) {
            throw new IllegalStateException("Another instance of " + Authorization.class.getSimpleName()
                    + "has been created and wasn't destroyed.");
        }
        Authorization.authorization = authorization;
    }

    /**
     * @return {@link #authorization}
     * @throws IllegalStateException when the no {@link Authorization} has been created
     */
    public static Authorization getInstance() throws IllegalStateException
    {
        if (authorization == null) {
            throw new IllegalStateException("No instance of " + Authorization.class.getSimpleName()
                    + "has been created.");
        }
        return authorization;
    }
}
