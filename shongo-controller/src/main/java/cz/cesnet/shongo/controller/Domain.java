package cz.cesnet.shongo.controller;

/**
 * Holds information about domain for which the controller is running.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Domain
{
    /**
     * Represents an unique domain name (e.g., "cz.cesnet")
     */
    private String name;

    /**
     * Represents shorten version of {@link #name} (e.g., used in description of virtual rooms)
     */
    private String code;

    /**
     * Represents a user-visible domain organization (e.g., "CESNET, z.s.p.o.").
     */
    private String organization;

    /**
     * Constructor.
     */
    public Domain()
    {
    }

    /**
     * Constructor.
     *
     * @param name sets the {@link #name}
     */
    public Domain(String name)
    {
        setName(name);
    }

    /**
     * Constructor.
     *
     * @param name         sets the {@link #name}
     * @param organization sets the {@link #organization}
     */
    public Domain(String name, String organization)
    {
        setName(name);
        setOrganization(organization);
    }

    /**
     * @return {@link #name}
     */
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
     * @return {@link #code}
     */
    public String getCode()
    {
        if (code == null) {
            return name;
        }
        else {
            return code;
        }
    }

    /**
     * @param code sets the {@link #code}
     */
    public void setCode(String code)
    {
        this.code = code;
    }

    /**
     * @return {@link #organization}
     */
    public String getOrganization()
    {
        return organization;
    }

    /**
     * @param organization sets the {@link #organization}
     */
    public void setOrganization(String organization)
    {
        this.organization = organization;
    }

    /**
     * @return domain converted to API
     */
    public cz.cesnet.shongo.controller.api.Domain toApi()
    {
        cz.cesnet.shongo.controller.api.Domain apiDomain = new cz.cesnet.shongo.controller.api.Domain();
        apiDomain.setName(getName());
        apiDomain.setOrganization(getOrganization());
        apiDomain.setStatus(cz.cesnet.shongo.controller.api.Domain.Status.AVAILABLE);
        return apiDomain;
    }

    /**
     * Local domain of {@link Controller#instance}.
     */
    private static Domain localDomain = null;

    /**
     * @return {@link #localDomain}
     */
    public static void setLocalDomain(Domain domain)
    {
        if (domain != null && localDomain != null) {
            throw new IllegalStateException("Local domain is already defined.");
        }
        localDomain = domain;
    }

    /**
     * @return {@link #localDomain}
     */
    public static Domain getLocalDomain()
    {
        if (localDomain == null) {
            throw new IllegalStateException("No local domain is defined.");
        }
        return localDomain;
    }

    /**
     * @return {@link #localDomain#getName()}
     */
    public static String getLocalDomainName()
    {
        if (localDomain == null) {
            throw new IllegalStateException("No local domain is defined.");
        }
        return localDomain.getName();
    }

    /**
     * @return {@link #localDomain#getCode()} ()}
     */
    public static String getLocalDomainCode()
    {
        if (localDomain == null) {
            throw new IllegalStateException("No local domain is defined.");
        }
        return localDomain.getCode();
    }
}
