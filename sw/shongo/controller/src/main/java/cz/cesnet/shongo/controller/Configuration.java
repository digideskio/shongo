package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.executor.Executable;
import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.tree.NodeCombiner;
import org.apache.commons.configuration.tree.UnionCombiner;
import org.joda.time.Duration;
import org.joda.time.Period;

/**
 * Configuration for the {@link Controller}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Configuration extends CombinedConfiguration
{
    /**
     * Time-zone in which the controller works and which is considered as default for date/times without specific zone.
     */
    public static final String TIMEZONE = "timezone";

    /**
     * Domain configuration.
     */
    public static final String DOMAIN_NAME = "domain.name";
    public static final String DOMAIN_ORGANIZATION = "domain.organization";

    /**
     * Database configuration.
     */
    public static final String DATABASE_DRIVER = "database.driver";
    public static final String DATABASE_URL = "database.url";
    public static final String DATABASE_USERNAME = "database.username";
    public static final String DATABASE_PASSWORD = "database.password";

    /**
     * API XML-RPC configuration.
     */
    public static final String RPC_HOST = "rpc.host";
    public static final String RPC_PORT = "rpc.port";

    /**
     * Jade configuration.
     */
    public static final String JADE_HOST = "jade.host";
    public static final String JADE_PORT = "jade.port";
    public static final String JADE_AGENT_NAME = "jade.agent-name";
    public static final String JADE_PLATFORM_ID = "jade.platform-id";

    /**
     * Worker configuration (it runs scheduler and executor).
     */
    public static final String WORKER_PERIOD = "worker.period";
    public static final String WORKER_INTERVAL = "worker.interval";

    /**
     * Maximum duration of reservations.
     */
    public static final String RESERVATION_RESOURCE_MAX_DURATION = "reservation.resource.max-duration";
    public static final String RESERVATION_VALUE_MAX_DURATION = "reservation.value.max-duration";

    /**
     * SMTP configuration.
     */
    public static final String SMTP_SENDER = "smtp.sender";
    public static final String SMTP_HOST = "smtp.host";
    public static final String SMTP_PORT = "smtp.port";
    public static final String SMTP_USERNAME = "smtp.username";
    public static final String SMTP_PASSWORD = "smtp.password";
    public static final String SMTP_SUBJECT_PREFIX = "smtp.subject-prefix";

    /**
     * Period in which the executor works.
     */
    public static final String EXECUTOR_PERIOD = "executor.period";

    /**
     * Duration to modify {@link Executable} starting date/time.
     */
    public static final String EXECUTOR_EXECUTABLE_START = "executor.executable.start";

    /**
     * Duration to modify {@link Executable} ending date/time.
     */
    public static final String EXECUTOR_EXECUTABLE_END = "executor.executable.end";

    /**
     * Period in which {@link cz.cesnet.shongo.controller.Executor} try to perform failed action again.
     */
    public static final String EXECUTOR_EXECUTABLE_NEXT_ATTEMPT = "executor.executable.next-attempt";

    /**
     * Maximum count of attempts for {@link cz.cesnet.shongo.controller.Executor} to try to perform action.
     */
    public static final String EXECUTOR_EXECUTABLE_MAX_ATTEMPT_COUNT = "executor.executable.max-attempt-count";

    /**
     * Duration which {@link cz.cesnet.shongo.controller.Executor} waits for virtual rooms to be created.
     */
    public static final String EXECUTOR_STARTING_DURATION_ROOM = "executor.starting-duration.room";

    /**
     * Security server.
     */
    public static final String SECURITY_SERVER = "security.server";

    /**
     * Specifies expiration of cache for user-id by access-token.
     */
    public static final String SECURITY_EXPIRATION_USER_ID = "security.expiration.user-id";

    /**
     * Specifies expiration of cache for user information by user-id.
     */
    public static final String SECURITY_EXPIRATION_USER_INFORMATION = "security.expiration.user-information";

    /**
     * Specifies expiration of cache for user ACL by user-id.
     */
    public static final String SECURITY_EXPIRATION_ACL = "security.expiration.acl";

    /**
     * Specifies access token which won't be verified and can be used for testing purposes.
     */
    public static final String SECURITY_ROOT_ACCESS_TOKEN = "security.root-access-token";

    /**
     * SSL host verification mappings.
     */
    public static final String SSL_HOST_VERIFICATION_MAPPINGS = "ssl.host-verification-mapping";

    /**
     * Administrator emails to which error are reported.
     */
    public static final String ADMINISTRATOR_EMAIL = "administrator.email";

    /**
     * Constructor.
     */
    public Configuration()
    {
        NodeCombiner nodeCombiner = new UnionCombiner();
        nodeCombiner.addListNode("host-verification-mapping");
        nodeCombiner.addListNode("email");
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

    /**
     * @see {@link #getString(String)}
     */
    public Period getPeriod(String key)
    {
        String value = getString(key);
        if (value == null) {
            return null;
        }
        return Period.parse(value);
    }
}
