package cz.cesnet.shongo.controller.allocation;

import cz.cesnet.shongo.Technology;

import java.util.Set;

/**
 * Represents one or multiple endpoint(s) in a scheduler plan.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface AllocatedEndpoint
{
    /**
     * @return number of the endpoints
     */
    public int getCount();

    /**
     * @return set of technologies which are supported by the endpoint(s)
     */
    public abstract Set<Technology> getSupportedTechnologies();

    /**
     * @return true if device can participate in 2-point video conference without virtual room,
     *         false otherwise
     */
    public boolean isStandalone();
}