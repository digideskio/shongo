<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%----%>
<%@ attribute name="code" required="true" type="java.lang.String" %>
<%@ attribute name="titleCode" required="false" type="java.lang.String" %>
<%@ attribute name="url" required="false" type="java.lang.String" %>
<%@ attribute name="ngClick" required="false" type="java.lang.String" %>
<%@ attribute name="target" required="false" type="java.lang.String" %>
<%@ attribute name="tabindex" required="false" type="java.lang.Integer" %>
<%----%>
<spring:message var="actionIcon" code="views.list.action.${code}.iconClass"/>
<c:choose>
    <c:when test="${titleCode != null}">
        <spring:message var="actionTitle" code="${titleCode}"/>
    </c:when>
    <c:otherwise>
        <spring:message var="actionTitle" code="views.list.action.${code}.title"/>
    </c:otherwise>
</c:choose>
<c:if test="${not empty ngClick}">
    <c:set var="url" value="#"/>
    <c:set var="ngClick" value=" ng-click=\"${ngClick}\""/>
</c:if>
<c:if test="${not empty target}">
    <c:set var="target"> target="${target}"</c:set>
</c:if>
<a href="${url}" ${ngClick}tabindex="${tabindex}" ${target}><b class="${actionIcon}" title="${actionTitle}"></b></a>