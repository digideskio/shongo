package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.authorization.AclRecord;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.executor.Executor;

/**
 * This class represents a common lock used in {@link WorkerThread} and {@link Executor} to not execute theirs
 * {@link WorkerThread#work} and {@link Executor#execute} at the same time.
 * <p/>
 * It is required because we don't want to execute some {@link Executable} for which hasn't been
 * created all {@link AclRecord}s yet. {@link AclRecord}s are created in the end of
 * {@link cz.cesnet.shongo.controller.scheduler.Scheduler#run} which is executed at the end of {@link WorkerThread#work}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ThreadLock
{
    /**
     * No instance can be created.
     */
    private ThreadLock()
    {
    }
}
