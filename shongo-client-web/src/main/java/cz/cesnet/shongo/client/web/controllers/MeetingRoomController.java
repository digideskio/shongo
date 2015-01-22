package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.ReservationRequestState;
import cz.cesnet.shongo.client.web.models.SpecificationType;
import cz.cesnet.shongo.client.web.models.TechnologyModel;
import cz.cesnet.shongo.client.web.support.editors.DateTimeEditor;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.*;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import cz.cesnet.shongo.util.DateTimeFormatter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * Controller for listing reservation requests of meeting rooms.
 *
 * @author: Ondřej Pavelka <pavelka@cesnet.cz>
 */
@Controller
public class MeetingRoomController {
    @Resource
    private ReservationService reservationService;

    @Resource
    private ResourceService resourceService;

    @Resource
    private Cache cache;

    @Resource
    private MessageSource messageSource;

    /**
     * Initialize model editors for additional types.
     *
     * @param binder to be initialized
     */
    @InitBinder
    public void initBinder(WebDataBinder binder, DateTimeZone timeZone)
    {
        binder.registerCustomEditor(DateTime.class, new DateTimeEditor(timeZone));
    }

    /**
     * Handle data request for list of reservation requests.
     */
    @RequestMapping(value = ClientWebUrl.MEETING_ROOM_RESERVATION_REQUEST_LIST_DATA, method = RequestMethod.GET)
    @ResponseBody
    public Map handleReservationRequestListData(
            Locale locale,
            DateTimeZone timeZone,
            SecurityToken securityToken,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "sort", required = false,
                    defaultValue = "SLOT") ReservationRequestListRequest.Sort sort,
            @RequestParam(value = "sort-desc", required = false, defaultValue = "true") boolean sortDescending,
            @RequestParam(value = "allocation-state", required = false) AllocationState allocationState,
            @RequestParam(value = "permanent-room-id", required = false) String permanentRoomId,
            @RequestParam(value = "specification-technology", required = false) TechnologyModel specificationTechnology,
            @RequestParam(value = "interval-from", required = false) DateTime intervalFrom,
            @RequestParam(value = "interval-to", required = false) DateTime intervalTo,
            @RequestParam(value = "user-id", required = false) String userId,
            @RequestParam(value = "participant-user-id", required = false) String participantUserId,
            @RequestParam(value = "search", required = false) String search)
    {
        // List reservation requests
        ReservationRequestListRequest request = new ReservationRequestListRequest();
        request.setSecurityToken(securityToken);
        request.setStart(start);
        request.setCount(count);
        request.setSort(sort);
        request.setSortDescending(sortDescending);
        request.setAllocationState(allocationState);

        // Get only resource reservation requests (meeting rooms) TODO: MR: change model
        request.addSpecificationType(ReservationRequestSummary.SpecificationType.RESOURCE);

        if (intervalFrom != null || intervalTo != null) {
            if (intervalFrom == null) {
                intervalFrom = Temporal.DATETIME_INFINITY_START;
            }
            if (intervalTo == null) {
                intervalTo = Temporal.DATETIME_INFINITY_END;
            }
            if (!intervalFrom.isAfter(intervalTo)) {
                request.setInterval(new Interval(intervalFrom, intervalTo));
            }
        }
        if (userId != null) {
            request.setUserId(userId);
        }
        if (participantUserId != null) {
            request.setParticipantUserId(participantUserId);
        }
        if (search != null) {
            request.setSearch(search);
        }
        ListResponse<ReservationRequestSummary> response = reservationService.listReservationRequests(request);

        Set<String> userIds = new HashSet<String>();
        Set<String> reusedReservationRequestIds = new HashSet<String>();
        for (ReservationRequestSummary reservationRequest : response.getItems()) {
            userIds.add(reservationRequest.getUserId());
            String reusedReservationRequestId = reservationRequest.getReusedReservationRequestId();
            if (reusedReservationRequestId != null) {
                reusedReservationRequestIds.add(reusedReservationRequestId);
            }
        }
        cache.fetchUserInformation(securityToken, userIds);
        cache.fetchReservationRequests(securityToken, reusedReservationRequestIds);

        // Get permissions for reservation requests
        Map<String, Set<ObjectPermission>> permissionsByReservationRequestId =
                cache.getReservationRequestsPermissions(securityToken, response.getItems());

        // Build response
        DateTimeFormatter formatter = DateTimeFormatter.getInstance(DateTimeFormatter.SHORT, locale, timeZone);
        List<Map<String, Object>> items = new LinkedList<Map<String, Object>>();
        for (ReservationRequestSummary reservationRequest : response.getItems()) {
            String reservationRequestId = reservationRequest.getId();
            SpecificationType specificationType = SpecificationType.fromReservationRequestSummary(reservationRequest);

            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", reservationRequestId);
            item.put("description", reservationRequest.getDescription());
            item.put("dateTime", formatter.formatDate(reservationRequest.getDateTime()));
            items.add(item);

            String reservationId = reservationRequest.getLastReservationId();
            if (reservationId != null) {
                item.put("reservationId", reservationId);
            }

            ReservationRequestState state = ReservationRequestState.fromApi(reservationRequest);
            if (state != null) {
                String lastReservationId = reservationRequest.getLastReservationId();
                item.put("state", state);
                item.put("stateMessage", state.getMessage(messageSource, locale, specificationType));
                item.put("stateHelp", state.getHelp(messageSource, locale, specificationType, lastReservationId));
            }

            Set<ObjectPermission> objectPermissions = permissionsByReservationRequestId.get(reservationRequestId);
            item.put("isWritable", objectPermissions.contains(ObjectPermission.WRITE));
            item.put("isProvidable", objectPermissions.contains(ObjectPermission.PROVIDE_RESERVATION_REQUEST));

            UserInformation user = cache.getUserInformation(securityToken, reservationRequest.getUserId());
            item.put("ownerName", user.getFullName());
            item.put("ownerEmail",user.getPrimaryEmail());

            Interval earliestSlot = reservationRequest.getEarliestSlot();
            if (earliestSlot != null) {
                item.put("earliestSlot", formatter.formatInterval(earliestSlot));
                item.put("earliestSlotMultiLine", formatter.formatIntervalMultiLine(earliestSlot, null, null));
            }
            Integer futureSlotCount = reservationRequest.getFutureSlotCount();
            if (futureSlotCount != null) {
                item.put("futureSlotCount", futureSlotCount);
            }
            boolean isDeprecated;
            switch (state != null ? state : ReservationRequestState.ALLOCATED) {
                case ALLOCATED_STARTED:
                case ALLOCATED_STARTED_AVAILABLE:
                    isDeprecated = false;
                    break;
                default:
                    isDeprecated = earliestSlot != null && earliestSlot.getEnd().isBeforeNow();
                    break;
            }
            item.put("isDeprecated", isDeprecated);

            Set<Technology> technologies = reservationRequest.getSpecificationTechnologies();
            TechnologyModel technology = TechnologyModel.find(technologies);
            item.put("type", specificationType);
            item.put("typeMessage", messageSource.getMessage(
                    "views.reservationRequest.specification." + specificationType, null, locale));

            String resourceId = reservationRequest.getResourceId();
            cz.cesnet.shongo.controller.api.Resource resource = resourceService.getResource(securityToken, resourceId);
            item.put("resourceName",resource.getName());
            item.put("resourceDescription",resource.getDescription());
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("start", response.getStart());
        data.put("count", response.getCount());
        data.put("sort", sort);
        data.put("sort-desc", sortDescending);
        data.put("items", items);
        return data;
    }

    /**
     * Handle data request for list of reservations.
     */
    @RequestMapping(value = ClientWebUrl.MEETING_ROOM_RESERVATION_LIST_DATA, method = RequestMethod.GET)
    @ResponseBody
    public Map handleReservationListData(
            Locale locale,
            DateTimeZone timeZone,
            SecurityToken securityToken,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "sort", required = false,
                    defaultValue = "SLOT") ReservationListRequest.Sort sort,
            @RequestParam(value = "sort-desc", required = false, defaultValue = "false") boolean sortDescending,
            @RequestParam(value = "allocation-state", required = false) AllocationState allocationState,
            @RequestParam(value = "resource-id", required = false) String resourceId,
            @RequestParam(value = "specification-technology", required = false) TechnologyModel specificationTechnology,
            @RequestParam(value = "interval-from", required = false) DateTime intervalFrom,
            @RequestParam(value = "interval-to", required = false) DateTime intervalTo,
            @RequestParam(value = "user-id", required = false) String userId,
            @RequestParam(value = "participant-user-id", required = false) String participantUserId,
            @RequestParam(value = "search", required = false) String search)
    {
        // List reservations
        ReservationListRequest request = new ReservationListRequest();
        request.setSecurityToken(securityToken);
        request.setStart(start);
        request.setCount(count);
        request.setSort(sort);
        request.setSortDescending(sortDescending);
        //request.addSpecificationType(ReservationRequestSummary.SpecificationType.RESOURCE);

        if (intervalFrom != null || intervalTo != null) {
            if (intervalFrom == null) {
                intervalFrom = Temporal.DATETIME_INFINITY_START;
            }
            if (intervalTo == null) {
                intervalTo = Temporal.DATETIME_INFINITY_END;
            }
            if (!intervalFrom.isAfter(intervalTo)) {
                request.setInterval(new Interval(intervalFrom, intervalTo));
            }
        }
        if (resourceId != null) {
            request.addResourceId(resourceId);
        }

        ListResponse<ReservationSummary> response = reservationService.listReservations(request);

        //TODO: MR: get permissions for reservations
        /*Set<String> userIds = new HashSet<String>();
        cache.fetchUserInformation(securityToken, userIds);
        cache.fetchReservationRequests(securityToken, reusedReservationRequestIds);

        // Get permissions for reservation requests
        for (ReservationSummary reservationSummary : response.getItems()) {
            reservationSummary.getReservationRequestId();
        }
        Map<String, Set<ObjectPermission>> permissionsByReservationRequestId =
                cache.getReservationRequestsPermissions(securityToken, );
        */

        // Build response
        DateTimeFormatter formatter = DateTimeFormatter.getInstance(DateTimeFormatter.SHORT, locale, timeZone);
        List<Map<String, Object>> items = new LinkedList<Map<String, Object>>();
        for (ReservationSummary reservation : response.getItems()) {
            Map<String, Object> item = new HashMap<String, Object>();
            String reservationId = reservation.getId();
            item.put("id", reservationId);
            item.put("description", reservation.getReservationRequestDescription());
            //item.put("dateTime", formatter.formatDate(reservationRequest.getDateTime()));
            items.add(item);

            if (reservationId != null) {
                item.put("reservationId", reservationId);
            }

            /*ReservationRequestState state = ReservationRequestState.fromApi(reservationRequest);
            if (state != null) {
                String lastReservationId = reservationRequest.getLastReservationId();
                item.put("state", state);
                item.put("stateMessage", state.getMessage(messageSource, locale, specificationType));
                item.put("stateHelp", state.getHelp(messageSource, locale, specificationType, lastReservationId));
            }*/

            /*Set<ObjectPermission> objectPermissions = permissionsByReservationRequestId.get(reservationRequestId);
            item.put("isWritable", objectPermissions.contains(ObjectPermission.WRITE));
            item.put("isProvidable", objectPermissions.contains(ObjectPermission.PROVIDE_RESERVATION_REQUEST));
            */
            UserInformation user = cache.getUserInformation(securityToken, reservation.getUserId());
            item.put("ownerName", user.getFullName());
            item.put("ownerEmail",user.getPrimaryEmail());

            Interval slot = reservation.getSlot();
            item.put("slot", formatter.formatInterval(slot));
            item.put("isDeprecated", slot != null && slot.getEnd().isBeforeNow());
            //item.put("type", specificationType);
            //item.put("typeMessage", messageSource.getMessage("views.reservationRequest.specification." + specificationType, null, locale));

            String reservationResourceId = reservation.getResourceId();
            cz.cesnet.shongo.controller.api.Resource resource = resourceService.getResource(securityToken, reservationResourceId);
            item.put("resourceName",resource.getName());
            item.put("resourceDescription",resource.getDescription());
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("start", response.getStart());
        data.put("count", response.getCount());
        data.put("sort", sort);
        data.put("sort-desc", sortDescending);
        data.put("items", items);
        return data;
    }
}