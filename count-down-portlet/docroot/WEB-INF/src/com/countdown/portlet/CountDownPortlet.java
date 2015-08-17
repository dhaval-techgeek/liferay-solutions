package com.countdown.portlet;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;

/**
 * Portlet implementation class CountDownPortlet
 */
public class CountDownPortlet extends MVCPortlet {

	private static Log _log = LogFactoryUtil.getLog(CountDownPortlet.class);

	@Override
	public void processAction(ActionRequest actionRequest, ActionResponse actionResponse) throws IOException, PortletException {
		PortletPreferences prefs = actionRequest.getPreferences();
		String eventDateAndTime = actionRequest.getParameter("eventDateAndTime");

		if (eventDateAndTime != null) {
			prefs.setValue("eventDateAndTime", eventDateAndTime);
			prefs.store();
		}
	}

	@Override
	public void render(RenderRequest renderRequest, RenderResponse renderResponse) throws PortletException, IOException {
		PortletPreferences prefs = renderRequest.getPreferences();
		String eventDateAndTime = prefs.getValue("eventDateAndTime", null);
		if (eventDateAndTime != null) {
			SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
			try {
				Date selectedDate = format.parse(eventDateAndTime);
				Date now = new Date();
				long diff = selectedDate.getTime() - now.getTime();

				long diffSeconds = diff / 1000 % 60;
				long diffMinutes = diff / (60 * 1000) % 60;
				long diffHours = diff / (60 * 60 * 1000) % 24;
				long diffDays = diff / (24 * 60 * 60 * 1000);

				renderRequest.setAttribute("diffDays", diffDays);
				renderRequest.setAttribute("diffHours", diffHours);
				renderRequest.setAttribute("diffMinutes", diffMinutes);
				renderRequest.setAttribute("diffSeconds", diffSeconds);

			} catch (ParseException e) {
				_log.error(e);
			}
		}
		super.render(renderRequest, renderResponse);
	}
}
