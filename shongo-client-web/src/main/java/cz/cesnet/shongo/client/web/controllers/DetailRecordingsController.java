package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ExecutableRecordingListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlService;
import cz.cesnet.shongo.util.DateTimeFormatter;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Controller for runtime management of room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class DetailRecordingsController extends AbstractDetailController
{
    private static Logger logger = LoggerFactory.getLogger(DetailRecordingsController.class);

    @Resource
    private ResourceControlService resourceControlService;

    /**
     * Handle detail recordings tab.
     */
    @RequestMapping(value = ClientWebUrl.DETAIL_RECORDINGS_TAB, method = RequestMethod.GET)
    public ModelAndView handleRecordingsTab(
            @PathVariable(value = "objectId") String objectId)
    {
        ModelAndView modelAndView = new ModelAndView("detailRecordings");
        return modelAndView;
    }

    @RequestMapping(value = ClientWebUrl.DETAIL_RECORDINGS_DATA, method = RequestMethod.GET)
    @ResponseBody
    public Map handleRecordingsData(
            Locale locale,
            DateTimeZone timeZone,
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "sort", required = false,
                    defaultValue = "START") ExecutableRecordingListRequest.Sort sort,
            @RequestParam(value = "sort-desc", required = false, defaultValue = "true") boolean sortDescending)
    {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.getInstance(
                DateTimeFormatter.Type.SHORT, locale, timeZone);

        String executableId = getExecutableId(securityToken, objectId);
        ExecutableRecordingListRequest request = new ExecutableRecordingListRequest();
        request.setSecurityToken(securityToken);
        request.setExecutableId(executableId);
        request.setStart(start);
        request.setCount(count);
        request.setSort(sort);
        request.setSortDescending(sortDescending);
        ListResponse<ResourceRecording> response = executableService.listExecutableRecordings(request);
        List<Map> items = new LinkedList<Map>();
        for (ResourceRecording recording : response.getItems()) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", recording.getId());
            item.put("resourceId", recording.getResourceId());
            item.put("name", recording.getName());
            item.put("description", recording.getDescription());
            item.put("beginDate", dateTimeFormatter.formatDateTime(recording.getBeginDate()));
            Duration duration = recording.getDuration();
            if (duration == null || duration.isShorterThan(Duration.standardMinutes(1))) {
                item.put("duration", messageSource.getMessage("views.room.recording.lessThanMinute", null, locale));
            }
            else {
                item.put("duration", dateTimeFormatter.formatRoundedDuration(duration.toPeriod()));
            }
            item.put("isPublic",recording.isPublic());
            item.put("downloadUrl", recording.getDownloadUrl());
            item.put("viewUrl", recording.getViewUrl());
            item.put("editUrl", recording.getEditUrl());
            item.put("filename",recording.getFileName());
            items.add(item);
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("start", response.getStart());
        data.put("count", response.getCount());
        data.put("sort", sort);
        data.put("sort-desc", sortDescending);
        data.put("items", items);
        return data;
    }

    @RequestMapping(value = ClientWebUrl.DETAIL_RECORDING_DELETE, method = RequestMethod.GET)
    public String handleRecordingDelete(
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId,
            @PathVariable(value = "resourceId") String resourceId,
            @PathVariable(value = "recordingId") String recordingId)
    {
        resourceControlService.deleteRecording(securityToken, resourceId, recordingId);
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_VIEW, objectId);
    }

    @RequestMapping(value = ClientWebUrl.DETAIL_RECORDING_DELETE, method = RequestMethod.POST)
    @ResponseBody
    public Map handleRoomManagementRecordingDeletePost(
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId,
            @PathVariable(value = "resourceId") String resourceId,
            @PathVariable(value = "recordingId") String recordingId)
    {
        handleRecordingDelete(securityToken, objectId, resourceId, recordingId);
        return null;
    }

    @RequestMapping(value = ClientWebUrl.DETAIL_RECORDING_MAKE_PUBLIC, method = RequestMethod.POST)
    @ResponseBody
    public Map handleMakeRecordingPublicPost(
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId,
            @PathVariable(value = "resourceId") String resourceId,
            @PathVariable(value = "recordingId") String recordingId)
    {
        resourceControlService.makeRecordingPublic(securityToken, resourceId, recordingId);
        return null;
    }

    @RequestMapping(value = ClientWebUrl.DETAIL_RECORDING_MAKE_PRIVATE, method = RequestMethod.POST)
    @ResponseBody
    public Map handleMakeRecordingPrivatePost(
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId,
            @PathVariable(value = "resourceId") String resourceId,
            @PathVariable(value = "recordingId") String recordingId)
    {
        resourceControlService.makeRecordingPrivate(securityToken, resourceId, recordingId);
        return null;
    }

    /**
     * Handle device command failed.
     */
    @ExceptionHandler(ControllerReportSet.DeviceCommandFailedException.class)
    public Object handleExceptions(Exception exception, HttpServletResponse response)
    {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return "errorRoomNotAvailable";
    }
}
