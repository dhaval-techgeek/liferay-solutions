package com.sample.service.hook;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.servlet.BrowserSnifferUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.struts.BaseStrutsPortletAction;
import com.liferay.portal.kernel.struts.StrutsPortletAction;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.JavaConstants;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutTypePortlet;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.portal.security.auth.PrincipalException;
import com.liferay.portal.service.PortletLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.expando.ColumnNameException;
import com.liferay.portlet.expando.ColumnTypeException;
import com.liferay.portlet.expando.DuplicateColumnNameException;
import com.liferay.portlet.expando.NoSuchColumnException;
import com.liferay.portlet.expando.ValueDataException;
import com.liferay.portlet.expando.model.ExpandoBridge;
import com.liferay.portlet.expando.model.ExpandoColumnConstants;
import com.liferay.portlet.expando.service.ExpandoColumnServiceUtil;
import com.liferay.portlet.expando.util.ExpandoBridgeFactoryUtil;

public class CustomEditExpandoAction extends BaseStrutsPortletAction {

	@Override
	public void processAction(StrutsPortletAction originalStrutsPortletAction, PortletConfig portletConfig, ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {
		// TODO Auto-generated method stub
		String cmd = ParamUtil.getString(actionRequest, Constants.CMD);

		try {
			if (cmd.equals(Constants.ADD)) {
				addExpando(actionRequest);
			} else if (cmd.equals(Constants.DELETE)) {
				deleteExpando(actionRequest);
			} else if (cmd.equals(Constants.UPDATE)) {
				updateExpando(actionRequest);
			}

			sendRedirect(portletConfig, actionRequest, actionResponse, null, null);
		} catch (Exception e) {
			if (e instanceof NoSuchColumnException || e instanceof PrincipalException) {

				SessionErrors.add(actionRequest, e.getClass());

				/* setForward(actionRequest, "portlet.expando.error"); */
				actionRequest.setAttribute(getForwardKey(actionRequest), "portlet.expando.error");
			} else if (e instanceof ColumnNameException || e instanceof ColumnTypeException || e instanceof DuplicateColumnNameException || e instanceof ValueDataException) {

				SessionErrors.add(actionRequest, e.getClass());
			} else {
				throw e;
			}
		}
		super.processAction(originalStrutsPortletAction, portletConfig, actionRequest, actionResponse);
	}

	@Override
	public String render(StrutsPortletAction originalStrutsPortletAction, PortletConfig portletConfig, RenderRequest renderRequest, RenderResponse renderResponse) throws Exception {
		// TODO Auto-generated method stub
		String ret = originalStrutsPortletAction.render(null, portletConfig, renderRequest, renderResponse);
		renderRequest.setAttribute(WebKeys.PORTLET_DECORATE, Boolean.TRUE);
		return ret;
	}

	protected void addExpando(ActionRequest actionRequest) throws Exception {
		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

		String modelResource = ParamUtil.getString(actionRequest, "modelResource");
		long resourcePrimKey = ParamUtil.getLong(actionRequest, "resourcePrimKey");

		String name = ParamUtil.getString(actionRequest, "name");
		String preset = ParamUtil.getString(actionRequest, "type");

		ExpandoBridge expandoBridge = ExpandoBridgeFactoryUtil.getExpandoBridge(themeDisplay.getCompanyId(), modelResource, resourcePrimKey);

		
		if (preset.startsWith("Preset")) {
			addPresetExpando(expandoBridge, preset, name);
		} else {
			int type = ParamUtil.getInteger(actionRequest, "type");

			expandoBridge.addAttribute(name, type);

			updateProperties(actionRequest, expandoBridge, name);
		}
		//Custom Code starts here
		if(null != name && name.equals("Location"))
		{
			List<User> users = UserLocalServiceUtil.getUsers(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
			for (User user : users) {
				String[] officeNames = null;
				 officeNames = (String[]) user.getExpandoBridge().getAttribute("Office");
				if (officeNames != null && officeNames.length > 0) {
					user.getExpandoBridge().setAttribute("Location", officeNames[0]);
				}
			}
		}
		
		//Custom Code ends here
	}

	protected int addPresetExpando(ExpandoBridge expandoBridge, String preset, String name) throws Exception {

		int type = 0;

		UnicodeProperties properties = null;

		try {
			properties = expandoBridge.getAttributeProperties(name);
		} catch (Exception e) {
			properties = new UnicodeProperties();
		}

		if (preset.equals("PresetSelectionIntegerArray()")) {
			type = ExpandoColumnConstants.INTEGER_ARRAY;

			properties.setProperty(ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE, ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE_SELECTION_LIST);
		} else if (preset.equals("PresetSelectionDoubleArray()")) {
			type = ExpandoColumnConstants.DOUBLE_ARRAY;

			properties.setProperty(ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE, ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE_SELECTION_LIST);
		} else if (preset.equals("PresetSelectionStringArray()")) {
			type = ExpandoColumnConstants.STRING_ARRAY;

			properties.setProperty(ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE, ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE_SELECTION_LIST);
		} else if (preset.equals("PresetTextBox()")) {
			type = ExpandoColumnConstants.STRING;

			properties.setProperty(ExpandoColumnConstants.PROPERTY_HEIGHT, "105");
			properties.setProperty(ExpandoColumnConstants.PROPERTY_WIDTH, "450");
		} else if (preset.equals("PresetTextBoxIndexed()")) {
			type = ExpandoColumnConstants.STRING;

			properties.setProperty(ExpandoColumnConstants.PROPERTY_HEIGHT, "105");
			properties.setProperty(ExpandoColumnConstants.PROPERTY_WIDTH, "450");
			properties.setProperty(ExpandoColumnConstants.INDEX_TYPE, String.valueOf(ExpandoColumnConstants.INDEX_TYPE_TEXT));
		} else if (preset.equals("PresetTextFieldSecret()")) {
			type = ExpandoColumnConstants.STRING;

			properties.setProperty(ExpandoColumnConstants.PROPERTY_SECRET, Boolean.TRUE.toString());
		} else {
			type = ExpandoColumnConstants.STRING;

			properties.setProperty(ExpandoColumnConstants.INDEX_TYPE, String.valueOf(ExpandoColumnConstants.INDEX_TYPE_TEXT));
		}

		expandoBridge.addAttribute(name, type);

		expandoBridge.setAttributeProperties(name, properties);

		return type;
	}

	protected void deleteExpando(ActionRequest actionRequest) throws Exception {
		long columnId = ParamUtil.getLong(actionRequest, "columnId");

		ExpandoColumnServiceUtil.deleteColumn(columnId);
	}

	protected Serializable getValue(PortletRequest portletRequest, String name, int type) throws PortalException, SystemException {

		String delimiter = StringPool.COMMA;

		Serializable value = null;

		if (type == ExpandoColumnConstants.BOOLEAN) {
			value = ParamUtil.getBoolean(portletRequest, name);
		} else if (type == ExpandoColumnConstants.BOOLEAN_ARRAY) {
		} else if (type == ExpandoColumnConstants.DATE) {
			User user = PortalUtil.getUser(portletRequest);

			int valueDateMonth = ParamUtil.getInteger(portletRequest, name + "Month");
			int valueDateDay = ParamUtil.getInteger(portletRequest, name + "Day");
			int valueDateYear = ParamUtil.getInteger(portletRequest, name + "Year");
			int valueDateHour = ParamUtil.getInteger(portletRequest, name + "Hour");
			int valueDateMinute = ParamUtil.getInteger(portletRequest, name + "Minute");
			int valueDateAmPm = ParamUtil.getInteger(portletRequest, name + "AmPm");

			if (valueDateAmPm == Calendar.PM) {
				valueDateHour += 12;
			}

			value = PortalUtil.getDate(valueDateMonth, valueDateDay, valueDateYear, valueDateHour, valueDateMinute, user.getTimeZone(), ValueDataException.class);
		} else if (type == ExpandoColumnConstants.DATE_ARRAY) {
		} else if (type == ExpandoColumnConstants.DOUBLE) {
			value = ParamUtil.getDouble(portletRequest, name);
		} else if (type == ExpandoColumnConstants.DOUBLE_ARRAY) {
			String paramValue = ParamUtil.getString(portletRequest, name);

			if (paramValue.contains(StringPool.NEW_LINE)) {
				delimiter = StringPool.NEW_LINE;
			}

			String[] values = StringUtil.split(paramValue, delimiter);

			value = GetterUtil.getDoubleValues(values);
		} else if (type == ExpandoColumnConstants.FLOAT) {
			value = ParamUtil.getFloat(portletRequest, name);
		} else if (type == ExpandoColumnConstants.FLOAT_ARRAY) {
			String paramValue = ParamUtil.getString(portletRequest, name);

			if (paramValue.contains(StringPool.NEW_LINE)) {
				delimiter = StringPool.NEW_LINE;
			}

			String[] values = StringUtil.split(paramValue, delimiter);

			value = GetterUtil.getFloatValues(values);
		} else if (type == ExpandoColumnConstants.INTEGER) {
			value = ParamUtil.getInteger(portletRequest, name);
		} else if (type == ExpandoColumnConstants.INTEGER_ARRAY) {
			String paramValue = ParamUtil.getString(portletRequest, name);

			if (paramValue.contains(StringPool.NEW_LINE)) {
				delimiter = StringPool.NEW_LINE;
			}

			String[] values = StringUtil.split(paramValue, delimiter);

			value = GetterUtil.getIntegerValues(values);
		} else if (type == ExpandoColumnConstants.LONG) {
			value = ParamUtil.getLong(portletRequest, name);
		} else if (type == ExpandoColumnConstants.LONG_ARRAY) {
			String paramValue = ParamUtil.getString(portletRequest, name);

			if (paramValue.contains(StringPool.NEW_LINE)) {
				delimiter = StringPool.NEW_LINE;
			}

			String[] values = StringUtil.split(paramValue, delimiter);

			value = GetterUtil.getLongValues(values);
		} else if (type == ExpandoColumnConstants.NUMBER) {
			value = ParamUtil.getNumber(portletRequest, name);
		} else if (type == ExpandoColumnConstants.NUMBER_ARRAY) {
			String paramValue = ParamUtil.getString(portletRequest, name);

			if (paramValue.contains(StringPool.NEW_LINE)) {
				delimiter = StringPool.NEW_LINE;
			}

			String[] values = StringUtil.split(paramValue, delimiter);

			value = GetterUtil.getNumberValues(values);
		} else if (type == ExpandoColumnConstants.SHORT) {
			value = ParamUtil.getShort(portletRequest, name);
		} else if (type == ExpandoColumnConstants.SHORT_ARRAY) {
			String paramValue = ParamUtil.getString(portletRequest, name);

			if (paramValue.contains(StringPool.NEW_LINE)) {
				delimiter = StringPool.NEW_LINE;
			}

			String[] values = StringUtil.split(paramValue, delimiter);

			value = GetterUtil.getShortValues(values);
		} else if (type == ExpandoColumnConstants.STRING_ARRAY) {
			String paramValue = ParamUtil.getString(portletRequest, name);

			if (paramValue.contains(StringPool.NEW_LINE)) {
				delimiter = StringPool.NEW_LINE;
			}

			value = StringUtil.split(paramValue, delimiter);
		} else {
			value = ParamUtil.getString(portletRequest, name);
		}

		return value;
	}

	protected void updateExpando(ActionRequest actionRequest) throws Exception {
		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

		String modelResource = ParamUtil.getString(actionRequest, "modelResource");
		long resourcePrimKey = ParamUtil.getLong(actionRequest, "resourcePrimKey");

		String name = ParamUtil.getString(actionRequest, "name");
		int type = ParamUtil.getInteger(actionRequest, "type");

		Serializable defaultValue = getValue(actionRequest, "defaultValue", type);

		ExpandoBridge expandoBridge = ExpandoBridgeFactoryUtil.getExpandoBridge(themeDisplay.getCompanyId(), modelResource, resourcePrimKey);

		expandoBridge.setAttributeDefault(name, defaultValue);

		updateProperties(actionRequest, expandoBridge, name);
	}

	protected void updateProperties(ActionRequest actionRequest, ExpandoBridge expandoBridge, String name) throws Exception {

		Enumeration<String> enu = actionRequest.getParameterNames();

		UnicodeProperties properties = expandoBridge.getAttributeProperties(name);

		List<String> propertyNames = new ArrayList<String>();

		while (enu.hasMoreElements()) {
			String param = enu.nextElement();

			if (param.contains("PropertyName--")) {
				String propertyName = ParamUtil.getString(actionRequest, param);

				propertyNames.add(propertyName);
			}
		}

		for (String propertyName : propertyNames) {
			String value = ParamUtil.getString(actionRequest, "Property--" + propertyName + "--");

			properties.setProperty(propertyName, value);
		}

		expandoBridge.setAttributeProperties(name, properties);
	}

	protected void sendRedirect(PortletConfig portletConfig, ActionRequest actionRequest, ActionResponse actionResponse, String redirect, String closeRedirect) throws IOException, SystemException {

		if (isDisplaySuccessMessage(actionRequest)) {
			addSuccessMessage(actionRequest, actionResponse);
		}

		if (Validator.isNull(redirect)) {
			redirect = (String) actionRequest.getAttribute(WebKeys.REDIRECT);
		}

		if (Validator.isNull(redirect)) {
			redirect = ParamUtil.getString(actionRequest, "redirect");
		}

		if ((portletConfig != null) && Validator.isNotNull(redirect) && Validator.isNotNull(closeRedirect)) {

			redirect = HttpUtil.setParameter(redirect, "closeRedirect", closeRedirect);

			SessionMessages.add(actionRequest, PortalUtil.getPortletId(actionRequest) + SessionMessages.KEY_SUFFIX_CLOSE_REDIRECT, closeRedirect);
		}

		if (Validator.isNull(redirect)) {
			return;
		}

		// LPS-1928

		HttpServletRequest request = PortalUtil.getHttpServletRequest(actionRequest);

		if (BrowserSnifferUtil.isIe(request) && (BrowserSnifferUtil.getMajorVersion(request) == 6.0) && redirect.contains(StringPool.POUND)) {

			String redirectToken = "&#";

			if (!redirect.contains(StringPool.QUESTION)) {
				redirectToken = StringPool.QUESTION + redirectToken;
			}

			redirect = StringUtil.replace(redirect, StringPool.POUND, redirectToken);
		}

		redirect = PortalUtil.escapeRedirect(redirect);

		if (Validator.isNotNull(redirect)) {
			actionResponse.sendRedirect(redirect);
		}
	}

	public static String getForwardKey(PortletRequest portletRequest) {
		String portletId = (String) portletRequest.getAttribute(WebKeys.PORTLET_ID);

		String portletNamespace = PortalUtil.getPortletNamespace(portletId);

		return portletNamespace.concat("PORTLET_STRUTS_FORWARD");
	}

	protected boolean isDisplaySuccessMessage(PortletRequest portletRequest) throws SystemException {

		if (!SessionErrors.isEmpty(portletRequest)) {
			return false;
		}

		ThemeDisplay themeDisplay = (ThemeDisplay) portletRequest.getAttribute(WebKeys.THEME_DISPLAY);

		Layout layout = themeDisplay.getLayout();

		if (layout.isTypeControlPanel()) {
			return true;
		}

		String portletId = (String) portletRequest.getAttribute(WebKeys.PORTLET_ID);

		try {
			LayoutTypePortlet layoutTypePortlet = themeDisplay.getLayoutTypePortlet();

			if (layoutTypePortlet.hasPortletId(portletId)) {
				return true;
			}
		} catch (PortalException pe) {
			pe.printStackTrace();
		}

		Portlet portlet = PortletLocalServiceUtil.getPortletById(themeDisplay.getCompanyId(), portletId);

		if (portlet.isAddDefaultResource()) {
			return true;
		}

		return false;
	}

	protected void addSuccessMessage(ActionRequest actionRequest, ActionResponse actionResponse) {

		PortletConfig portletConfig = (PortletConfig) actionRequest.getAttribute(JavaConstants.JAVAX_PORTLET_CONFIG);

		boolean addProcessActionSuccessMessage = GetterUtil.getBoolean(portletConfig.getInitParameter("add-process-action-success-action"), true);

		if (!addProcessActionSuccessMessage) {
			return;
		}

		String successMessage = ParamUtil.getString(actionRequest, "successMessage");

		SessionMessages.add(actionRequest, "requestProcessed", successMessage);
	}

}