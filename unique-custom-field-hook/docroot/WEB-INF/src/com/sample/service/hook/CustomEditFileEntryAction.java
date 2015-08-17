package com.sample.service.hook;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.DuplicateLockException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.portlet.LiferayWindowState;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.servlet.ServletResponseConstants;
import com.liferay.portal.kernel.servlet.ServletResponseUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.struts.BaseStrutsPortletAction;
import com.liferay.portal.kernel.struts.StrutsPortletAction;
import com.liferay.portal.kernel.upload.UploadException;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.MimeTypesUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.Layout;
import com.liferay.portal.security.auth.PrincipalException;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.ServiceContextFactory;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PortletKeys;
import com.liferay.portal.util.PrefsPropsUtil;
import com.liferay.portlet.PortletPreferencesFactoryUtil;
import com.liferay.portlet.asset.AssetCategoryException;
import com.liferay.portlet.asset.AssetTagException;
import com.liferay.portlet.assetpublisher.util.AssetPublisherUtil;
import com.liferay.portlet.documentlibrary.DuplicateFileException;
import com.liferay.portlet.documentlibrary.DuplicateFolderNameException;
import com.liferay.portlet.documentlibrary.FileExtensionException;
import com.liferay.portlet.documentlibrary.FileMimeTypeException;
import com.liferay.portlet.documentlibrary.FileNameException;
import com.liferay.portlet.documentlibrary.FileSizeException;
import com.liferay.portlet.documentlibrary.InvalidFileVersionException;
import com.liferay.portlet.documentlibrary.NoSuchFileEntryException;
import com.liferay.portlet.documentlibrary.NoSuchFolderException;
import com.liferay.portlet.documentlibrary.SourceFileNameException;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.service.DLAppServiceUtil;
import com.liferay.portlet.documentlibrary.util.DLUtil;
import com.liferay.portlet.dynamicdatamapping.StorageFieldRequiredException;

public class CustomEditFileEntryAction extends BaseStrutsPortletAction {

	@Override
	public void processAction(StrutsPortletAction originalStrutsPortletAction, PortletConfig portletConfig, ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {

		String cmd = ParamUtil.getString(actionRequest, Constants.CMD);

		FileEntry fileEntry = null;

		try {
			if (Validator.isNull(cmd)) {
				UploadException uploadException = (UploadException) actionRequest.getAttribute(WebKeys.UPLOAD_EXCEPTION);

				if (uploadException != null) {
					if (uploadException.isExceededSizeLimit()) {
						throw new FileSizeException(uploadException.getCause());
					}

					throw new PortalException(uploadException.getCause());
				}
			} else if (cmd.equals(Constants.ADD) || cmd.equals(Constants.ADD_DYNAMIC) || cmd.equals(Constants.UPDATE) || cmd.equals(Constants.UPDATE_AND_CHECKIN)) {

				fileEntry = updateFileEntry(portletConfig, actionRequest, actionResponse);
			} else if (cmd.equals(Constants.ADD_MULTIPLE)) {
				addMultipleFileEntries(portletConfig, actionRequest, actionResponse);
			} else if (cmd.equals(Constants.ADD_TEMP)) {
				addTempFileEntry(actionRequest, actionResponse);
			} else if (cmd.equals(Constants.DELETE)) {
				deleteFileEntry(actionRequest, false);
			} else if (cmd.equals(Constants.DELETE_TEMP)) {
				deleteTempFileEntry(actionRequest, actionResponse);
			} else if (cmd.equals(Constants.CANCEL_CHECKOUT)) {
				cancelFileEntriesCheckOut(actionRequest);
			} else if (cmd.equals(Constants.CHECKIN)) {
				checkInFileEntries(actionRequest);
			} else if (cmd.equals(Constants.CHECKOUT)) {
				checkOutFileEntries(actionRequest);
			} else if (cmd.equals(Constants.MOVE)) {
				moveFileEntries(actionRequest, false);
			} else if (cmd.equals(Constants.MOVE_FROM_TRASH)) {
				moveFileEntries(actionRequest, true);
			} else if (cmd.equals(Constants.MOVE_TO_TRASH)) {
				deleteFileEntry(actionRequest, true);
			} else if (cmd.equals(Constants.REVERT)) {
				revertFileEntry(actionRequest);
			}

			WindowState windowState = actionRequest.getWindowState();

			if (cmd.equals(Constants.ADD_TEMP) || cmd.equals(Constants.DELETE_TEMP)) {

				setForward(actionRequest, ActionConstants.COMMON_NULL);
			} else if (cmd.equals(Constants.PREVIEW)) {
			} else if (!cmd.equals(Constants.MOVE_FROM_TRASH) && !windowState.equals(LiferayWindowState.POP_UP)) {

				sendRedirect(actionRequest, actionResponse);
			} else {
				String redirect = ParamUtil.getString(actionRequest, "redirect");
				int workflowAction = ParamUtil.getInteger(actionRequest, "workflowAction", WorkflowConstants.ACTION_SAVE_DRAFT);

				if ((fileEntry != null) && (workflowAction == WorkflowConstants.ACTION_SAVE_DRAFT)) {

					redirect = getSaveAndContinueRedirect(portletConfig, actionRequest, fileEntry, redirect);

					sendRedirect(actionRequest, actionResponse, redirect);
				} else {
					if (!windowState.equals(LiferayWindowState.POP_UP)) {
						sendRedirect(actionRequest, actionResponse);
					} else {
						redirect = PortalUtil.escapeRedirect(ParamUtil.getString(actionRequest, "redirect"));

						if (Validator.isNotNull(redirect)) {
							if (cmd.equals(Constants.ADD) && (fileEntry != null)) {

								String portletId = HttpUtil.getParameter(redirect, "p_p_id", false);

								String namespace = PortalUtil.getPortletNamespace(portletId);

								redirect = HttpUtil.addParameter(redirect, namespace + "className", DLFileEntry.class.getName());
								redirect = HttpUtil.addParameter(redirect, namespace + "classPK", fileEntry.getFileEntryId());
							}

							actionResponse.sendRedirect(redirect);
						}
					}
				}
			}
		} catch (Exception e) {
			handleUploadException(portletConfig, actionRequest, actionResponse, cmd, e);
		}
	}
	
	protected void handleUploadException(
			PortletConfig portletConfig, ActionRequest actionRequest,
			ActionResponse actionResponse, String cmd, Exception e)
		throws Exception {

		if (e instanceof AssetCategoryException ||
			e instanceof AssetTagException) {

			SessionErrors.add(actionRequest, e.getClass(), e);
		}
		else if (e instanceof DuplicateFileException ||
				 e instanceof DuplicateFolderNameException ||
				 e instanceof FileExtensionException ||
				 e instanceof FileMimeTypeException ||
				 e instanceof FileNameException ||
				 e instanceof FileSizeException ||
				 e instanceof NoSuchFolderException ||
				 e instanceof SourceFileNameException ||
				 e instanceof StorageFieldRequiredException) {

			if (!cmd.equals(Constants.ADD_DYNAMIC) &&
				!cmd.equals(Constants.ADD_MULTIPLE) &&
				!cmd.equals(Constants.ADD_TEMP)) {

				SessionErrors.add(actionRequest, e.getClass());

				return;
			}

			if (e instanceof DuplicateFileException ||
				e instanceof FileExtensionException ||
				e instanceof FileNameException ||
				e instanceof FileSizeException) {

				HttpServletResponse response =
					PortalUtil.getHttpServletResponse(actionResponse);

				response.setContentType(ContentTypes.TEXT_HTML);
				response.setStatus(HttpServletResponse.SC_OK);

				String errorMessage = StringPool.BLANK;
				int errorType = 0;

				ThemeDisplay themeDisplay =
					(ThemeDisplay)actionRequest.getAttribute(
						WebKeys.THEME_DISPLAY);

				if (e instanceof DuplicateFileException) {
					errorMessage = themeDisplay.translate(
						"please-enter-a-unique-document-name");
					errorType =
						ServletResponseConstants.SC_DUPLICATE_FILE_EXCEPTION;
				}
				else if (e instanceof FileExtensionException) {
					errorMessage = themeDisplay.translate(
						"please-enter-a-file-with-a-valid-extension-x",
						StringUtil.merge(
							getAllowedFileExtensions(
								portletConfig, actionRequest, actionResponse)));
					errorType =
						ServletResponseConstants.SC_FILE_EXTENSION_EXCEPTION;
				}
				else if (e instanceof FileNameException) {
					errorMessage = themeDisplay.translate(
						"please-enter-a-file-with-a-valid-file-name");
					errorType = ServletResponseConstants.SC_FILE_NAME_EXCEPTION;
				}
				else if (e instanceof FileSizeException) {
					long fileMaxSize = PrefsPropsUtil.getLong(
						PropsKeys.DL_FILE_MAX_SIZE);

					if (fileMaxSize == 0) {
						fileMaxSize = PrefsPropsUtil.getLong(
							PropsKeys.UPLOAD_SERVLET_REQUEST_IMPL_MAX_SIZE);
					}

					fileMaxSize /= 1024;

					errorMessage = themeDisplay.translate(
						"please-enter-a-file-with-a-valid-file-size-no-larger" +
							"-than-x",
						fileMaxSize);

					errorType = ServletResponseConstants.SC_FILE_SIZE_EXCEPTION;
				}

				JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

				jsonObject.put("message", errorMessage);
				jsonObject.put("status", errorType);

				writeJSON(actionRequest, actionResponse, jsonObject);
			}

			SessionErrors.add(actionRequest, e.getClass());
		}
		else if (e instanceof DuplicateLockException ||
				 e instanceof InvalidFileVersionException ||
				 e instanceof NoSuchFileEntryException ||
				 e instanceof PrincipalException) {

			if (e instanceof DuplicateLockException) {
				DuplicateLockException dle = (DuplicateLockException)e;

				SessionErrors.add(actionRequest, dle.getClass(), dle.getLock());
			}
			else {
				SessionErrors.add(actionRequest, e.getClass());
			}

			setForward(actionRequest, "portlet.document_library.error");
		}
		else {
			throw e;
		}
	}

	protected FileEntry updateFileEntry(PortletConfig portletConfig, ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {

		UploadPortletRequest uploadPortletRequest = PortalUtil.getUploadPortletRequest(actionRequest);

		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

		String cmd = ParamUtil.getString(uploadPortletRequest, Constants.CMD);

		long fileEntryId = ParamUtil.getLong(uploadPortletRequest, "fileEntryId");

		long repositoryId = ParamUtil.getLong(uploadPortletRequest, "repositoryId");
		long folderId = ParamUtil.getLong(uploadPortletRequest, "folderId");
		String sourceFileName = uploadPortletRequest.getFileName("file");
		String title = ParamUtil.getString(uploadPortletRequest, "title");
		String description = ParamUtil.getString(uploadPortletRequest, "description");
		String changeLog = ParamUtil.getString(uploadPortletRequest, "changeLog");
		boolean majorVersion = ParamUtil.getBoolean(uploadPortletRequest, "majorVersion");

		if (folderId > 0) {
			Folder folder = DLAppServiceUtil.getFolder(folderId);

			if (folder.getGroupId() != themeDisplay.getScopeGroupId()) {
				throw new NoSuchFolderException();
			}
		}

		InputStream inputStream = null;

		try {
			String contentType = uploadPortletRequest.getContentType("file");

			long size = uploadPortletRequest.getSize("file");

			if ((cmd.equals(Constants.ADD) || cmd.equals(Constants.ADD_DYNAMIC)) && (size == 0)) {

				contentType = MimeTypesUtil.getContentType(title);
			}

			if (cmd.equals(Constants.ADD) || cmd.equals(Constants.ADD_DYNAMIC) || (size > 0)) {

				String portletName = portletConfig.getPortletName();

				if (portletName.equals(PortletKeys.MEDIA_GALLERY_DISPLAY)) {
					PortletPreferences portletPreferences = getStrictPortletSetup(actionRequest);

					if (portletPreferences == null) {
						portletPreferences = actionRequest.getPreferences();
					}

					String[] mimeTypes = DLUtil.getMediaGalleryMimeTypes(portletPreferences, actionRequest);

					if (Arrays.binarySearch(mimeTypes, contentType) < 0) {
						throw new FileMimeTypeException(contentType);
					}
				}
			}

			inputStream = uploadPortletRequest.getFileAsStream("file");

			ServiceContext serviceContext = ServiceContextFactory.getInstance(DLFileEntry.class.getName(), uploadPortletRequest);

			FileEntry fileEntry = null;

			if (cmd.equals(Constants.ADD) || cmd.equals(Constants.ADD_DYNAMIC)) {

				// Add file entry

				fileEntry = DLAppServiceUtil.addFileEntry(repositoryId, folderId, sourceFileName, contentType, title, description, changeLog, inputStream, size, serviceContext);

				AssetPublisherUtil.addAndStoreSelection(actionRequest, DLFileEntry.class.getName(), fileEntry.getFileEntryId(), -1);

				if (cmd.equals(Constants.ADD_DYNAMIC)) {
					JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

					jsonObject.put("fileEntryId", fileEntry.getFileEntryId());

					writeJSON(actionRequest, actionResponse, jsonObject);
				}
			} else if (cmd.equals(Constants.UPDATE_AND_CHECKIN)) {

				// Update file entry and checkin

				fileEntry = DLAppServiceUtil.updateFileEntryAndCheckIn(fileEntryId, sourceFileName, contentType, title, description, changeLog, majorVersion, inputStream, size, serviceContext);
			} else {

				// Update file entry

				fileEntry = DLAppServiceUtil.updateFileEntry(fileEntryId, sourceFileName, contentType, title, description, changeLog, majorVersion, inputStream, size, serviceContext);
			}

			AssetPublisherUtil.addRecentFolderId(actionRequest, DLFileEntry.class.getName(), folderId);

			return fileEntry;
		} catch (Exception e) {
			UploadException uploadException = (UploadException) actionRequest.getAttribute(WebKeys.UPLOAD_EXCEPTION);

			if ((uploadException != null) && uploadException.isExceededSizeLimit()) {

				throw new FileSizeException(uploadException.getCause());
			}

			throw e;
		} finally {
			StreamUtil.cleanUp(inputStream);
		}
	}

	@Override
	public String render(StrutsPortletAction originalStrutsPortletAction, PortletConfig portletConfig, RenderRequest renderRequest, RenderResponse renderResponse) throws Exception {
		// TODO Auto-generated method stub
		String ret = originalStrutsPortletAction.render(null, portletConfig, renderRequest, renderResponse);
		renderRequest.setAttribute(WebKeys.PORTLET_DECORATE, Boolean.TRUE);
		return ret;
	}

	protected void writeJSON(PortletRequest portletRequest, ActionResponse actionResponse, Object json) throws IOException {

		HttpServletResponse response = PortalUtil.getHttpServletResponse(actionResponse);

		response.setContentType(ContentTypes.APPLICATION_JSON);

		ServletResponseUtil.write(response, json.toString());

		response.flushBuffer();

		//setForward(portletRequest, "/common/null.jsp");
		portletRequest.setAttribute(getForwardKey(portletRequest), "portlet.expando.error");
	}
	
	public static String getForwardKey(PortletRequest portletRequest) {
		String portletId = (String) portletRequest.getAttribute(WebKeys.PORTLET_ID);

		String portletNamespace = PortalUtil.getPortletNamespace(portletId);

		return portletNamespace.concat("PORTLET_STRUTS_FORWARD");
	}

	protected PortletPreferences getStrictPortletSetup(
			Layout layout, String portletId)
		throws PortalException, SystemException {

		if (Validator.isNull(portletId)) {
			return null;
		}

		PortletPreferences portletPreferences =	PortletPreferencesFactoryUtil.getStrictPortletSetup(layout, portletId);

		if (portletPreferences instanceof StrictPortletPreferencesImpl) {
			throw new PrincipalException();
		}

		return portletPreferences;
	}

	protected PortletPreferences getStrictPortletSetup(
			PortletRequest portletRequest)
		throws PortalException, SystemException {

		String portletResource = ParamUtil.getString(
			portletRequest, "portletResource");

		ThemeDisplay themeDisplay = (ThemeDisplay)portletRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		return getStrictPortletSetup(themeDisplay.getLayout(), portletResource);
	}
	private static final String _TEMP_FOLDER_NAME = CustomEditFileEntryAction.class.getName();

}
