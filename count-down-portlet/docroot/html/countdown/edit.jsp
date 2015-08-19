<%@include file="/html/init.jsp" %>

<%
	PortletPreferences prefs = renderRequest.getPreferences();
	String eventDateAndTime = (String) prefs.getValue("eventDateAndTime", "");
%>

<portlet:actionURL var="setPreferences">
	<portlet:param name="mvcPath" value="/html/countdown/edit.jsp" />
</portlet:actionURL>
<portlet:renderURL var="viewCountDown" portletMode="view">
</portlet:renderURL>
<aui:form action="<%=setPreferences%>" method="post">
	<h2>Set Your Event Date &amp; Time:</h2>
	<aui:input type="text" value="<%=eventDateAndTime%>" id="datetimepicker" name="eventDateAndTime" label="Select Date and Time"></aui:input>
	<aui:button-row>
		<aui:button type="submit" />

		<aui:button type="cancel" onClick="<%= viewCountDown %>" />
	</aui:button-row>
</aui:form>
<script>
	$('#_<%=themeDisplay.getPortletDisplay().getId()%>_datetimepicker').datetimepicker({
				dayOfWeekStart : 7,
				lang : 'en',
				startDate : new Date,
				minDate : new Date,
				format:'d/m/Y H:i',
				step:5
			});
</script>