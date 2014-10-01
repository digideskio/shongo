<%--
  -- Meeting rooms tab in dashboard.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<security:authentication property="principal.userId" var="userId"/>
<tag:url var="meetingRoomListUrl" value="<%= ClientWebUrl.MEETING_ROOM_LIST_DATA %>">
    <tag:param name="specification-type" value="MEETING_ROOM"/>
</tag:url>
<tag:url var="meetingRoomDetailUrl" value="<%= ClientWebUrl.DETAIL_VIEW %>">
    <tag:param name="objectId" value="{{room.id}}" escape="false"/>
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="meetingRoomModifyUrl" value="<%= ClientWebUrl.WIZARD_MODIFY %>">
    <tag:param name="reservationRequestId" value="{{room.id}}" escape="false"/>
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="meetingRoomDeleteUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DELETE %>">
    <tag:param name="reservationRequestId" value="{{room.id}}" escape="false"/>
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>


<div ng-controller="PaginationController"
     ng-init="init('roomList', '${meetingRoomListUrl}', null, 'refresh-meetingRooms')">
    <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
    <spring:message code="views.button.refresh" var="paginationRefresh"/>
    <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}" refresh="${paginationRefresh}">
        <spring:message code="views.pagination.records"/>
    </pagination-page-size>
    <div class="alert alert-warning"><spring:message code="views.index.meetingRooms.description"/></div>
    <div class="spinner" ng-hide="ready || errorContent"></div>
    <span ng-controller="HtmlController" ng-show="errorContent" ng-bind-html="html(errorContent)"></span>
    <table class="table table-striped table-hover" ng-show="ready">
        <thead>
        <tr>
            <th>
                <pagination-sort column="ROOM_NAME"><spring:message code="views.reservationRequestList.resourceName"/></pagination-sort>
            </th>
            <th width="200px">
                <pagination-sort column="OWNER"><spring:message code="views.room.bookedBy"/></pagination-sort>
            </th>
            <th>
                <pagination-sort column="SLOT"><spring:message code="views.room.slot"/></pagination-sort>
            </th>
            <th width="200px">
                <pagination-sort column="STATE"><spring:message code="views.room.state"/></pagination-sort>
            </th>
            <th>
                <spring:message code="views.room.description"/>
            </th>
            <th style="min-width: 95px; width: 95px;">
                <spring:message code="views.list.action"/>
                <pagination-sort-default class="pull-right"><spring:message code="views.pagination.defaultSorting"/></pagination-sort-default>
            </th>
        </tr>
        </thead>
        <tbody>
        <tr ng-repeat="room in items" ng-class="{'deprecated': room.isDeprecated}">
            <td ng-controller="ParticipantRoomController">
                <spring:message code="views.room.name.adhoc" var="roomNameAdhoc"/>
                <tag:help label="{{room.resourceName}}" selectable="true">
                    <span>
                        <strong><spring:message code="views.room.roomDescription"/></strong>
                        <br />
                        {{room.resourceDescription}}
                    </span>
                </tag:help>
            </td>
            <td>
                <span>{{room.user}}</span>
            </td>
            <td>
                <span ng-bind-html="room.earliestSlot"></span>
                <span ng-show="room.futureSlotCount">
                    <spring:message code="views.reservationRequestList.slotMore" var="slotMore" arguments="{{room.futureSlotCount}}"/>
                    <tag:help label="(${slotMore})" cssClass="push-top">
                        <spring:message code="views.reservationRequestList.slotMoreHelp"/>
                    </tag:help>
                </span>
            </td>
            <td class="reservation-request-state">
                <tag:help label="{{room.stateMessage}}" cssClass="{{room.state}}">
                    <span>{{room.stateHelp}}</span>
                </tag:help>
            </td>
            <td>{{room.description}}</td>
            <td>
                <tag:listAction code="show" titleCode="views.index.reservations.showDetail" url="${meetingRoomDetailUrl}" tabindex="1"/>
                <span ng-show="room.isWritable">
                    <span ng-hide="room.state == 'ALLOCATED_FINISHED'">
                        | <tag:listAction code="modify" url="${meetingRoomModifyUrl}" tabindex="2"/>
                    </span>
                    | <tag:listAction code="delete" url="${meetingRoomDeleteUrl}" tabindex="3"/>
                </span>
            </td>
        </tr>
        </tbody>
        <tbody>
        <tr ng-hide="items.length">
            <td colspan="6" class="empty"><spring:message code="views.list.none"/></td>
        </tr>
        </tbody>
    </table>
    <pagination-pages ng-show="ready"><spring:message code="views.pagination.pages"/></pagination-pages>
</div>