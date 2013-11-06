package cz.cesnet.shongo.controller.booking.recording;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.booking.EntityIdentifier;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.executable.ExecutableService;
import cz.cesnet.shongo.controller.booking.reservation.ExecutableServiceReservation;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.room.*;
import cz.cesnet.shongo.controller.booking.specification.ExecutableServiceSpecification;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.scheduler.*;
import cz.cesnet.shongo.controller.util.RangeSet;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link cz.cesnet.shongo.controller.booking.specification.ExecutableServiceSpecification} for recording.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class RecordingServiceSpecification extends ExecutableServiceSpecification implements ReservationTaskProvider
{
    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return cz.cesnet.shongo.controller.api.ExecutableServiceSpecification.createRecording();
    }

    @Override
    public ReservationTask createReservationTask(SchedulerContext schedulerContext)
    {
        return new ReservationTask(schedulerContext)
        {
            @Override
            protected Reservation allocateReservation() throws SchedulerException
            {
                Interval interval = getInterval();
                Cache cache = getCache();
                ResourceCache resourceCache = cache.getResourceCache();
                Executable executable = getExecutable();
                Interval executableSlot = executable.getSlot();

                // Check interval
                if (!executableSlot.contains(interval)) {
                    throw new SchedulerReportSet.ExecutableServiceInvalidSlotException(executableSlot, interval);
                }

                // Check executable
                if (executable instanceof RoomEndpoint) {
                    RoomEndpoint roomEndpoint = (RoomEndpoint) executable;
                    RoomProviderCapability roomProviderCapability = getRoomProvider(roomEndpoint);
                    DeviceResource deviceResource = roomProviderCapability.getDeviceResource();
                    RecordingCapability recordingCapability = deviceResource.getCapability(RecordingCapability.class);

                    // If room is not automatically recordable
                    if (recordingCapability == null) {
                        // Allocate room license for recording
                        RoomReservationTask roomReservationTask = new RoomReservationTask(schedulerContext, 1, false);
                        roomReservationTask.setRoomProviderCapability(roomProviderCapability);
                        addChildReservation(roomReservationTask);
                    }
                    else {
                        // Check whether licenses aren't unlimited (otherwise the rooms are always recordable
                        // and we shouldn't allocate the recording service for it)
                        if (recordingCapability.getLicenseCount() == null) {
                            throw new SchedulerReportSet.RoomEndpointAlwaysRecordableException(
                                    EntityIdentifier.formatId(executable));
                        }
                    }
                }
                else {
                    throw new TodoImplementException(executable.getClass());
                }

                // Find matching recorders
                List<AvailableRecorder> availableRecorders = new LinkedList<AvailableRecorder>();
                beginReport(new SchedulerReportSet.FindingAvailableResourceReport());
                for (RecordingCapability recordingCapability : cache.getRecorders()) {
                    DeviceResource deviceResource = recordingCapability.getDeviceResource();
                    if (technologies.size() > 0 && !deviceResource.hasTechnologies(technologies)) {
                        continue;
                    }

                    // Get available recorder
                    EntityManager entityManager = schedulerContext.getEntityManager();
                    ReservationManager reservationManager = new ReservationManager(entityManager);
                    List<RecordingServiceReservation> roomReservations =
                            reservationManager.getRecordingServiceReservations(recordingCapability, interval);
                    schedulerContext.applyReservations(recordingCapability.getId(),
                            roomReservations, RecordingServiceReservation.class);
                    RangeSet<RecordingServiceReservation, DateTime> rangeSet =
                            new RangeSet<RecordingServiceReservation, DateTime>();
                    for (RecordingServiceReservation roomReservation : roomReservations) {
                        rangeSet.add(roomReservation, roomReservation.getSlotStart(), roomReservation.getSlotEnd());
                    }
                    List<RangeSet.Bucket> roomBuckets = new LinkedList<RangeSet.Bucket>();
                    roomBuckets.addAll(rangeSet.getBuckets(interval.getStart(), interval.getEnd()));
                    Collections.sort(roomBuckets, new Comparator<RangeSet.Bucket>()
                    {
                        @Override
                        public int compare(RangeSet.Bucket roomBucket1, RangeSet.Bucket roomBucket2)
                        {
                            return -Double.compare(roomBucket1.size(), roomBucket2.size());
                        }
                    });
                    int usedLicenseCount = 0;
                    if (roomBuckets.size() > 0) {
                        RangeSet.Bucket roomBucket = roomBuckets.get(0);
                        usedLicenseCount = roomBucket.size();
                    }
                    AvailableRecorder availableRecorder = new AvailableRecorder(recordingCapability, usedLicenseCount);
                    if (availableRecorder.getAvailableLicenseCount() == 0) {
                        addReport(new SchedulerReportSet.ResourceRecordingCapacityExceededReport(deviceResource));
                        continue;
                    }
                    availableRecorders.add(availableRecorder);
                    addReport(new SchedulerReportSet.ResourceReport(deviceResource));
                }
                if (availableRecorders.size() == 0) {
                    throw new SchedulerException(getCurrentReport());
                }
                endReport();

                // Sort recorders
                addReport(new SchedulerReportSet.SortingResourcesReport());
                Collections.sort(availableRecorders, new Comparator<AvailableRecorder>()
                {
                    @Override
                    public int compare(AvailableRecorder first, AvailableRecorder second)
                    {
                        int result = -Double.compare(first.getFullnessRatio(), second.getFullnessRatio());
                        if (result != 0) {
                            return result;
                        }
                        return 0;
                    }
                });

                // Allocate reservation in some matching recorder
                for (AvailableRecorder availableRecorder : availableRecorders) {
                    RecordingCapability recordingCapability = availableRecorder.getRecordingCapability();
                    DeviceResource deviceResource = availableRecorder.getDeviceResource();
                    beginReport(new SchedulerReportSet.AllocatingResourceReport(deviceResource));

                    // Check whether alias provider can be allocated
                    try {
                        resourceCache.checkCapabilityAvailable(recordingCapability, schedulerContext);
                    }
                    catch (SchedulerException exception) {
                        endReportError(exception.getReport());
                        continue;
                    }

                    // Allocate recording service
                    RecordingService recordingService = new RecordingService();
                    recordingService.setRecordingCapability(recordingCapability);
                    recordingService.setExecutable(executable);
                    recordingService.setSlot(interval);
                    if (isEnabled()) {
                        recordingService.setState(ExecutableService.State.PREPARED);
                    }
                    else {
                        recordingService.setState(ExecutableService.State.NOT_ACTIVE);
                    }

                    // Allocate recording reservation
                    RecordingServiceReservation recordingServiceReservation = new RecordingServiceReservation();
                    recordingServiceReservation.setRecordingCapability(recordingCapability);
                    recordingServiceReservation.setSlot(interval);
                    recordingServiceReservation.setExecutableService(recordingService);
                    return recordingServiceReservation;
                }
                throw new SchedulerException(getCurrentReport());
            }
        };
    }

    /**
     * @param roomEndpoint
     * @return {@link RoomProviderCapability} for given {@code roomEndpoint}
     */
    private RoomProviderCapability getRoomProvider(RoomEndpoint roomEndpoint)
    {
        if (roomEndpoint instanceof ResourceRoomEndpoint) {
            ResourceRoomEndpoint resourceRoomEndpoint = (ResourceRoomEndpoint) roomEndpoint;
            return resourceRoomEndpoint.getRoomProviderCapability();
        }
        else if (roomEndpoint instanceof UsedRoomEndpoint) {
            UsedRoomEndpoint usedRoomEndpoint = (UsedRoomEndpoint) roomEndpoint;
            return getRoomProvider(usedRoomEndpoint.getRoomEndpoint());
        }
        else {
            throw new TodoImplementException(roomEndpoint.getClass());
        }
    }

    private static class AvailableRecorder
    {
        /**
         * {@link DeviceResource} with the {@link RecordingCapability}.
         */
        private final RecordingCapability recordingCapability;

        /**
         * Number of available {@link RecordingCapability#licenseCount}.
         */
        private final int availableLicenseCount;

        /**
         * Constructor.
         *
         * @param recordingCapability sets the {@link #recordingCapability}
         * @param usedLicenseCount to be used for computing {@link #availableLicenseCount}
         */
        private AvailableRecorder(RecordingCapability recordingCapability, int usedLicenseCount)
        {
            this.recordingCapability = recordingCapability;
            this.availableLicenseCount = recordingCapability.getLicenseCount() - usedLicenseCount;
            if (this.availableLicenseCount < 0) {
                throw new IllegalStateException("Available license count can't be negative.");
            }
        }

        /**
         * @return {@link #recordingCapability}
         */
        public RecordingCapability getRecordingCapability()
        {
            return recordingCapability;
        }

        /**
         * @return {@link DeviceResource} of the {@link #recordingCapability}
         */
        public DeviceResource getDeviceResource()
        {
            return recordingCapability.getDeviceResource();
        }

        /**
         * @return {@link #availableLicenseCount}
         */
        public int getAvailableLicenseCount()
        {
            return availableLicenseCount;
        }

        /**
         * @return maximum {@link RecordingCapability#licenseCount} for {@link #recordingCapability}
         */
        public Integer getMaximumLicenseCount()
        {
            return recordingCapability.getLicenseCount();
        }

        /**
         * @return ratio of fullness for the device (0.0 - 1.0)
         */
        public Double getFullnessRatio()
        {
            return 1.0 - (double) getAvailableLicenseCount() / (double) getMaximumLicenseCount();
        }
    }
}
