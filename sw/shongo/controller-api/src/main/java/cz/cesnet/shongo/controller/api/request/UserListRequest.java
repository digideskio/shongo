package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.api.SecurityToken;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link ListRequest} for reservations.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UserListRequest extends ListRequest
{
    /**
     * User-ids of users which should be returned.
     */
    private Set<String> userIds = new HashSet<String>();

    /**
     * Group-ids of groups from which the users will be returned.
     */
    private Set<String> groupIds = new HashSet<String>();

    /**
     * String for filtering users by name, email, etc.
     */
    private String search;

    /**
     * Constructor.
     */
    public UserListRequest()
    {
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     */
    public UserListRequest(SecurityToken securityToken)
    {
        super(securityToken);
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     * @param userId        to be added to the {@link #userIds}
     */
    public UserListRequest(SecurityToken securityToken, String userId)
    {
        super(securityToken);
        userIds.add(userId);
    }

    public Set<String> getUserIds()
    {
        return userIds;
    }

    public void setUserIds(Set<String> userIds)
    {
        this.userIds.clear();
        this.userIds.addAll(userIds);
    }

    public void addUserId(String userId)
    {
        userIds.add(userId);
    }

    public Set<String> getGroupIds()
    {
        return groupIds;
    }

    public void setGroupIds(Set<String> groupIds)
    {
        this.groupIds.clear();
        this.groupIds.addAll(groupIds);
    }

    public void addGroupId(String groupId)
    {
        groupIds.add(groupId);
    }

    public String getSearch()
    {
        return search;
    }

    public void setSearch(String search)
    {
        this.search = search;
    }

    private static final String USER_IDS = "userIds";
    private static final String GROUP_IDS = "groupIds";
    private static final String SEARCH = "search";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(USER_IDS, userIds);
        dataMap.set(GROUP_IDS, groupIds);
        dataMap.set(SEARCH, search);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        userIds = dataMap.getSet(USER_IDS, String.class);
        groupIds = dataMap.getSet(GROUP_IDS, String.class);
        search = dataMap.getString(SEARCH);
    }
}
