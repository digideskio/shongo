package cz.cesnet.shongo.controller;

/**
 * Holds information about domain for which the controller is running.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Domain
{
    /**
     * Represents an unique code name (e.g., "cz.cesnet")
     */
    private String codeName;

    /**
     * Represents a user-visible name (e.g., "CESNET, z.s.p.o.").
     */
    private String name;

    /**
     * Constructor.
     */
    public Domain()
    {
    }

    /**
     * Constructor.
     * @param codeName sets the {@link #codeName}
     */
    public Domain(String codeName)
    {
        setCodeName(codeName);
    }

    /**
     * Constructor.
     * @param codeName sets the {@link #codeName}
     * @param name     sets the {@link #name}
     */
    public Domain(String codeName, String name)
    {
        setCodeName(codeName);
        setName(name);
    }

    /**
     * @return {@link #codeName}
     */
    public String getCodeName()
    {
        return codeName;
    }

    /**
     * @param codeName sets the {@link #codeName}
     */
    public void setCodeName(String codeName)
    {
        this.codeName = codeName;
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
}