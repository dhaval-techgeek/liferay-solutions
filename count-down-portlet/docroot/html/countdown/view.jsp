<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<portlet:defineObjects />
<div class="container">
	<h2>Countdown</h2>
	<div class='countdown' data-role='countdown' data-days='<%=renderRequest.getAttribute("diffDays")%>' data-hours='<%=renderRequest.getAttribute("diffHours") %>' data-mins='<%= renderRequest.getAttribute("diffMinutes") %>' data-secs='<%= renderRequest.getAttribute("diffSeconds") %>'></div>
</div>