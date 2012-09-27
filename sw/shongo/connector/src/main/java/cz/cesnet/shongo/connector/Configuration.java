package cz.cesnet.shongo.connector;

import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.tree.NodeCombiner;
import org.apache.commons.configuration.tree.UnionCombiner;
import org.joda.time.Duration;
import org.joda.time.Period;

/**
 * Configuration for the {@link Connector}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Configuration extends CombinedConfiguration
{
    /**
     * Configuration parameters names.
     */
    public static final String CONTROLLER_HOST = "controller.host";
    public static final String CONTROLLER_PORT = "controller.port";
    public static final String CONTROLLER_CONNECTION_CHECK_PERIOD = "controller.connection-check-period";
    public static final String JADE_HOST = "jade.host";
    public static final String JADE_PORT = "jade.port";

    /**
     * Constructor.
     */
    public Configuration()
    {
        NodeCombiner nodeCombiner = new UnionCombiner();
        nodeCombiner.addListNode("instance");
        setNodeCombiner(nodeCombiner);
    }

    /**
     * @see {@link #getString(String)}
     */
    public Duration getDuration(String key)
    {
        String value = getString(key);
        if (value == null) {
            return null;
        }
        return Period.parse(value).toStandardDuration();
    }
}