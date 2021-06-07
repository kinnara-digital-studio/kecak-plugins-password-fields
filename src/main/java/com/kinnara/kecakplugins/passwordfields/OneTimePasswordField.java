package com.kinnara.kecakplugins.passwordfields;

import com.kinnara.kecakplugins.passwordfields.commons.RestApiException;
import com.kinnara.kecakplugins.passwordfields.commons.Utils;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author aristo
 *
 * TODO: add additional features from leagacy OneTimePasswordField
 * - encrypted data
 * -
 *
 */
public class OneTimePasswordField extends Element implements FormBuilderPaletteElement, Utils, PluginWebSupport, AceFormElement, AdminLteFormElement {
	private final static String BODY_FORM_ID = "FORM_ID";
	private final static String BODY_SECTION_ID = "SECTION_ID";
	private final static String BODY_FIELD_ID = "FIELD_ID";
	private final static String PRIMARY_KEY = "PRIMARY_KEY";
	private final static String PROCESS_ID_KEY = "PROCESS_ID";
	private final static String ACTIVITY_ID_KEY = "ACTIVITY_ID";
	private final static String USERNAME = "USERNAME";
	private final static String BODY_NONCE = "NONCE";

	private final static String PARAMETER_APP_ID = "appId";
	private final static String PARAMETER_APP_VERSION = "appVersion";
	private final static String PARAMETER_GENERATE_TOKEN = "_GENERATE_TOKEN";

	@Override
	public FormRowSet formatData(FormData formData) {
		if(!"true".equalsIgnoreCase(getPropertyString("keepTokenInDatabase"))) {
			// clear otp
			String defaultEmptyTokenValue = getDefaultEmptyTokenValue(this);
			formData.getRequestParams().put(FormUtil.getElementParameterName(this), new String[]{ defaultEmptyTokenValue });
		}

		// save data
		return super.formatData(formData);
	}

	@Override
	public String renderTemplate(FormData formData, Map dataModel) {
		String template = "OneTimePassword.ftl";
		return renderTemplate(template, formData, dataModel);
	}

	@Override
	public String renderAdminLteTemplate(FormData formData, Map dataModel) {
		String template = "OneTimePasswordAdminLte.ftl";
		return renderTemplate(template, formData, dataModel);
	}

	@Override
	public String renderAceTemplate(FormData formData, Map dataModel) {
		String template = "OneTimePasswordAce.ftl";
		return renderTemplate(template, formData, dataModel);
	}

	@Override
	public Object handleElementValueResponse(@Nonnull Element element, @Nonnull FormData formData) throws JSONException {
		AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
		Form rootForm = FormUtil.findRootForm(this);
		String primaryKeyValue = formData.getPrimaryKeyValue();

		return SecurityUtil.generateNonce(nonceArgumentsGenerator(
				appDefinition,
				rootForm.getPropertyString(FormUtil.PROPERTY_ID),
				getPropertyString(FormUtil.PROPERTY_ID),
				primaryKeyValue),
				1);
	}

	private void storeInitialValue(@Nonnull final Form form, @Nonnull final Element element, final FormData formData) {
		// TODO : currently not working with mobile / API

		Optional.ofNullable(formData)

				// value has to be empty
				.filter(fd -> FormUtil.getElementPropertyValue(element, formData).isEmpty())

				// primary key is not empty
				.map(FormData::getPrimaryKeyValue)
				.filter(s -> !s.isEmpty())


				.ifPresent(primaryKey -> {
					String parameterName = FormUtil.getElementParameterName(element);
					String defaultEmptyTokenValue = getDefaultEmptyTokenValue(element);

					ApplicationContext applicationContext = AppUtil.getApplicationContext();
					FormService formService = (FormService) applicationContext.getBean("formService");

					FormData storeFormData = new FormData();
					storeFormData.setPrimaryKeyValue(primaryKey);
					storeFormData.addRequestParameterValues(FormUtil.PROPERTY_ID, new String[] { primaryKey });
					storeFormData.addRequestParameterValues(parameterName, new String[] { defaultEmptyTokenValue });

					formService.executeFormStoreBinders(form, storeFormData);
				});
	}

	protected String renderTemplate(String template, FormData formData, @SuppressWarnings("rawtypes") Map dataModel){
		ApplicationContext appContext = AppUtil.getApplicationContext();
		WorkflowUserManager wum = (WorkflowUserManager) appContext.getBean("workflowUserManager");
		AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();

//		dataModel.put("value", getBinderValue(formData));
		final String primaryKeyValue = getPrimaryKeyValue(formData);
		if(primaryKeyValue != null && !primaryKeyValue.equals("")) {
			dataModel.put(PRIMARY_KEY, primaryKeyValue);
		}

		// process id
		Optional.of(formData)
				.map(FormData::getProcessId)
				.ifPresent(s -> dataModel.put(PROCESS_ID_KEY, s));

		// activity id
		Optional.of(formData)
				.map(FormData::getActivityId)
				.ifPresent(s -> dataModel.put(ACTIVITY_ID_KEY, s));

		Form rootForm = FormUtil.findRootForm(this);
		if (rootForm != null) {
			dataModel.put(BODY_FORM_ID, rootForm.getPropertyString(FormUtil.PROPERTY_ID));

//			storeInitialValue(rootForm, this, formData);

			Section section = this.getElementSection(this);
			if(section != null) {
				dataModel.put(BODY_SECTION_ID, section.getPropertyString(FormUtil.PROPERTY_ID));
			}

			if(section != null) {
				dataModel.put(BODY_NONCE,
						SecurityUtil.generateNonce(
								nonceArgumentsGenerator(
										appDefinition,
										rootForm.getPropertyString(FormUtil.PROPERTY_ID),
										getPropertyString(FormUtil.PROPERTY_ID),
										primaryKeyValue),
								1));
			}
		}

		dataModel.put(BODY_FIELD_ID, getPropertyString(FormUtil.PROPERTY_ID));

		String currentUser = wum.getCurrentUsername();
		if(currentUser!=null && !currentUser.isEmpty()) {
			dataModel.put(USERNAME, currentUser);
		}

		dataModel.put(PARAMETER_APP_ID, appDefinition.getAppId());
		dataModel.put(PARAMETER_APP_VERSION, appDefinition.getVersion());
		dataModel.put("className", getClassName());

		HttpServletRequest servletRequest = WorkflowUtil.getHttpServletRequest();
		dataModel.put("requestParameter", servletRequest.getQueryString());

		String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
		return html;
	}

	@Override
	public String getFormBuilderCategory() {
		return "Kecak";
	}

	@Override
	public int getFormBuilderPosition() {
		return 100;
	}

	@Override
	public String getFormBuilderIcon() {
		return "/plugin/org.joget.apps.form.lib.TextField/images/textField_icon.gif";
	}

	@Override
	public String getFormBuilderTemplate() {
		return "<label class='label'>OneTimePassword</label><input type='text' />";
	}

	@Override
	public String getName() {
		return getLabel() + getVersion();
	}

	@Override
	public String getVersion() {
		return getClass().getPackage().getImplementationVersion();
	}

	@Override
	public String getDescription() {
		return getClass().getPackage().getImplementationTitle();
	}

	@Override
	public String getLabel() {
		return "One-Time Password Field";
	}

	@Override
	public String getClassName() {
		return getClass().getName();
	}

	@Override
	public String getPropertyOptions() {
		return AppUtil.readPluginResource(getClassName(), "/properties/OneTimePasswordElement.json", new String[] {OneTimePasswordValidator.class.getName()}, true, "/messages/OneTimePassword").replaceAll("\"", "'");
	}

	@Override
    public void webService(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
		PluginManager pluginManager = (PluginManager) applicationContext.getBean("pluginManager");
		WorkflowUserManager workflowUserManager = (WorkflowUserManager) applicationContext.getBean("workflowUserManager");

        try {
            if(!"POST".equalsIgnoreCase(httpServletRequest.getMethod())) {
                throw new RestApiException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Only accept POST method");
            }

            JSONObject requestBody;
            try {
                requestBody = getRequestBody(httpServletRequest);
            } catch (JSONException e) {
                throw new RestApiException(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            }

            final String formId = getRequiredBodyPayload(requestBody, "formId");
            final String fieldId = getRequiredBodyPayload(requestBody, "fieldId");
            final String primaryKey = getRequiredBodyPayload(requestBody, "primaryKey");
            final String username = getRequiredBodyPayload(requestBody, "username");

            workflowUserManager.setCurrentThreadUser(username);

            String activityId = getOptionalBodyPayload(requestBody, "activityId", null);
            String processId = getOptionalBodyPayload(requestBody, "processId", null);


            @Nullable
            final WorkflowAssignment assignment =  getAssignment(activityId, processId, primaryKey);

            AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();

			final FormData formData = new FormData();
			formData.setPrimaryKeyValue(primaryKey);

			if(assignment != null) {
				formData.setProcessId(assignment.getProcessId());
				formData.setActivityId(assignment.getActivityId());
			}

			Form form = generateForm(appDefinition, formId);

			Element element = Optional.of(form)
					.map(e -> FormUtil.findElement(fieldId, e, formData))
					.orElseThrow(() -> new RestApiException(HttpServletResponse.SC_NOT_FOUND, "Form [" + formId + "] field [" + fieldId + "] not found"));

			boolean ignoreNonce = "true".equalsIgnoreCase(element.getPropertyString("ignoreNonce"));
			if(!ignoreNonce) {
				final String nonce = getRequiredBodyPayload(requestBody, "nonce");

				// validate nonce
				String[] nonceArgs = nonceArgumentsGenerator(appDefinition, formId, fieldId, primaryKey);
				if (!SecurityUtil.verifyNonce(nonce, nonceArgs)) {
					throw new RestApiException(HttpServletResponse.SC_BAD_REQUEST, "Invalid nonce");
				}
			}

            // get current data
            final int digits = getTokenDigits(element);
            final String randomToken = generateRandomToken(digits);

			storeToken(form, primaryKey, fieldId, randomToken);

			Form formToExecute = generateForm(formId, formData);
			Element elementToExecute = FormUtil.findElement(fieldId, formToExecute, formData);
            Map<String, Object> notificationToolProperty = (Map<String, Object>) elementToExecute.getProperty("notificationTool");
            DefaultApplicationPlugin notificationToolPlugin = pluginManager.getPluginObject(notificationToolProperty);

            if(notificationToolPlugin != null) {
				Map<String, Object> executionProperties = (Map<String, Object>) notificationToolProperty.get("properties");

				executionProperties.put("pluginManager", pluginManager);
				executionProperties.put("appDef", appDefinition);
				if (assignment != null) {
					executionProperties.put("workflowAssignment", assignment);
				}

				if ("true".equalsIgnoreCase(element.getPropertyString("debug"))) {
					LogUtil.info(getClassName(), "Sending OTP notification token [" + randomToken + "]");
				}

				notificationToolPlugin.execute(executionProperties);
			}

            JSONObject jsonResponse = new JSONObject();
            try {
            	String successMessage = Optional.ofNullable(elementToExecute.getPropertyString("messageOnSuccessGenerateToken"))
						.filter(s -> !s.isEmpty())
						.orElse(AppPluginUtil.getMessage("oneTimePassword.messageOnSuccessGenerateToken.default", getClassName(), "/messages/OneTimePassword"));
                jsonResponse.put("message", successMessage);
            } catch (JSONException ex) {
                throw new RestApiException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
            }

            httpServletResponse.setContentType("application/json");
            httpServletResponse.getWriter().write(jsonResponse.toString());
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);

        } catch (RestApiException ex) {
            LogUtil.warn(getClassName(), ex.getMessage());
            httpServletResponse.sendError(ex.getErrorCode(), ex.getMessage());
        }
    }

    private String generateRandomToken(int digits) {
        Random rand = new Random();
        return String.format("%0"+digits+"d", rand.nextInt((int) Math.pow(10, digits)));
    }

    private JSONObject getRequestBody(HttpServletRequest request) throws IOException, JSONException {
        return new JSONObject(request.getReader().lines().collect(Collectors.joining()));
    }

    @Deprecated
	private Section getElementSection(Element element) {
		if(element == null) {
			return null;
		}

		if(element instanceof Section) {
			return (Section) element;
		}

		return getElementSection(element.getParent());
	}

	private String[] nonceArgumentsGenerator(AppDefinition appDefinition, String formId, String fieldId, String primaryKey) {
		String[] strings = {
				appDefinition.getAppId(),
				String.valueOf(appDefinition.getVersion()),
				formId,
				fieldId,
				primaryKey
		};

		return strings;
	}

	/**
	 * Get Default Empty Token Value
	 *
	 * @return
	 */
	private String getDefaultEmptyTokenValue(Element element) {
		return Optional.of("defaultEmptyTokenValue")
				.map(element::getPropertyString)
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.orElse("");
	}

	/**
	 * Determine number of digits for token, default = 4
	 *
	 * @param element
	 * @return
	 */
	private int getTokenDigits(Element element) {
		return Optional.ofNullable(element.getPropertyString("digitsToken"))
				.filter(s -> s.matches("[0-9]+"))
				.map(Integer::parseInt)
				.orElse(4);
	}

	protected String getBinderValue(FormData formData) {
		String id = getPropertyString(FormUtil.PROPERTY_ID);
		String value = getPropertyString(FormUtil.PROPERTY_VALUE);

		// load from binder if available
		if (formData != null) {
			String binderValue = formData.getLoadBinderDataProperty(this, id);
			if (binderValue != null) {
				value = binderValue;
			}
		}

		return value;
	}
}
