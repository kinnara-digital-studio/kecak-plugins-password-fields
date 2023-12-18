package com.kinnara.kecakplugins.passwordfields.form.element;

import com.kinnara.kecakplugins.passwordfields.commons.RestApiException;
import com.kinnara.kecakplugins.passwordfields.commons.Utils;
import com.kinnara.kecakplugins.passwordfields.form.binder.OneTimePasswordLoadBinder;
import com.kinnara.kecakplugins.passwordfields.form.validator.OneTimePasswordValidator;
import com.kinnara.kecakplugins.passwordfields.hashvariable.OneTimePasswordHashVariable;
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
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author aristo
 * <p>
 * TODO: add additional features from leagacy OneTimePasswordField
 * - encrypted data
 * -
 */
public class OneTimePasswordField extends Element implements FormBuilderPaletteElement, Utils, PluginWebSupport {
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
        if (!"true".equalsIgnoreCase(getPropertyString("keepTokenInDatabase"))) {
            // clear otp
            formData.getRequestParams().put(FormUtil.getElementParameterName(this), new String[]{""});
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

                    ApplicationContext applicationContext = AppUtil.getApplicationContext();
                    FormService formService = (FormService) applicationContext.getBean("formService");

                    FormData storeFormData = new FormData();
                    storeFormData.setPrimaryKeyValue(primaryKey);
                    storeFormData.addRequestParameterValues(FormUtil.PROPERTY_ID, new String[]{primaryKey});
                    storeFormData.addRequestParameterValues(parameterName, new String[]{""});

                    formService.executeFormStoreBinders(form, storeFormData);
                });
    }

    protected String renderTemplate(String template, FormData formData, @SuppressWarnings("rawtypes") Map dataModel) {
        ApplicationContext appContext = AppUtil.getApplicationContext();
        WorkflowUserManager wum = (WorkflowUserManager) appContext.getBean("workflowUserManager");
        AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();

//		dataModel.put("value", getBinderValue(formData));
        final String primaryKeyValue = getPrimaryKeyValue(formData);
        if (primaryKeyValue != null && !primaryKeyValue.equals("")) {
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
            if (section != null) {
                dataModel.put(BODY_SECTION_ID, section.getPropertyString(FormUtil.PROPERTY_ID));
            }

            if (section != null) {
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
        if (currentUser != null && !currentUser.isEmpty()) {
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
        return getLabel();
    }

    @Override
    public String getVersion() {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        ResourceBundle resourceBundle = pluginManager.getPluginMessageBundle(getClassName(), "/messages/BuildNumber");
        String buildNumber = resourceBundle.getString("buildNumber");
        return buildNumber;
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public String getLabel() {
        return "One-Time Password";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        final String[] args = new String[]{
                OneTimePasswordLoadBinder.class.getName(),
                OneTimePasswordValidator.class.getName()
        };

        return AppUtil.readPluginResource(getClassName(), "/properties/OneTimePasswordField.json", args, true, "/messages/OneTimePassword");
    }

    @Override
    public void webService(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        PluginManager pluginManager = (PluginManager) applicationContext.getBean("pluginManager");
        WorkflowUserManager workflowUserManager = (WorkflowUserManager) applicationContext.getBean("workflowUserManager");

        try {
            if (!"POST".equalsIgnoreCase(httpServletRequest.getMethod())) {
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
            final String primaryKey = getOptionalBodyPayload(requestBody, "primaryKey", null);
            final String username = getRequiredBodyPayload(requestBody, "username");

            workflowUserManager.setCurrentThreadUser(username);

            String activityId = getOptionalBodyPayload(requestBody, "activityId", null);
            String processId = getOptionalBodyPayload(requestBody, "processId", null);

            @Nullable final WorkflowAssignment assignment = getAssignment(activityId, processId, primaryKey);

            final AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();

            final FormData formData = new FormData();
            formData.setPrimaryKeyValue(primaryKey);

            if (assignment != null) {
                formData.setProcessId(assignment.getProcessId());
                formData.setActivityId(assignment.getActivityId());
            }

            final Form form = generateForm(appDefinition, formId);

            final OneTimePasswordField element = Optional.of(form)
                    .map(f -> FormUtil.findElement(fieldId, f, formData))
                    .filter(e -> e instanceof OneTimePasswordField)
                    .map(e -> (OneTimePasswordField) e)
                    .orElseThrow(() -> new RestApiException(HttpServletResponse.SC_NOT_FOUND, "Form [" + formId + "] field [" + fieldId + "] not found"));

            boolean ignoreNonce = element.ignoreNonce();
            if (!ignoreNonce) {
                final String nonce = getRequiredBodyPayload(requestBody, "nonce");

                // validate nonce
                final String[] nonceArgs = nonceArgumentsGenerator(appDefinition, formId, fieldId, primaryKey);
                if (!SecurityUtil.verifyNonce(nonce, nonceArgs)) {
                    throw new RestApiException(HttpServletResponse.SC_BAD_REQUEST, "Invalid nonce");
                }
            }

            // generate and load token
            final String oneTimePassword = Optional.of(element)
                    .map(Element::getLoadBinder)
                    .map(binder -> binder.load(element, primaryKey, formData))
                    .map(Collection::stream)
                    .orElseGet(Stream::empty)
                    .findFirst()
                    .map(r -> r.getProperty(fieldId))
                    .orElseThrow(() -> {
                        final String message = "Error generating OTP";
                        LogUtil.warn(getClass().getName(), "Error form [" + formId + "] field [" + fieldId + "] message [" + message + "]");
                        return new RestApiException(HttpServletResponse.SC_BAD_REQUEST, message);
                    });

            final Form formToExecute = generateForm(formId, formData);
            final Element elementToExecute = FormUtil.findElement(fieldId, formToExecute, formData);
            final Map<String, Object> notificationToolProperty = (Map<String, Object>) elementToExecute.getProperty("notificationTool");
            final DefaultApplicationPlugin notificationToolPlugin = pluginManager.getPlugin(notificationToolProperty);
            final OneTimePasswordHashVariable oneTimePasswordHashVariable = (OneTimePasswordHashVariable) pluginManager.getPlugin(OneTimePasswordHashVariable.class.getName());

            if (notificationToolPlugin != null && oneTimePasswordHashVariable != null) {
                final String prefix = oneTimePasswordHashVariable.getPrefix();

                final Map<String, Object> executionProperties = (Map<String, Object>) deepReplaceOtp(notificationToolProperty.get("properties"), "#" + prefix + "#", oneTimePassword);

                executionProperties.put("pluginManager", pluginManager);
                executionProperties.put("appDef", appDefinition);

                if (primaryKey != null) {
                    executionProperties.put("recordId", primaryKey);
                }

                if (assignment != null) {
                    executionProperties.put("workflowAssignment", assignment);
                }

                notificationToolPlugin.execute(executionProperties);
            }

            final JSONObject jsonResponse = new JSONObject();
            try {
                final String successMessage = Optional.ofNullable(elementToExecute.getPropertyString("messageOnSuccessGenerateToken"))
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

    private JSONObject getRequestBody(HttpServletRequest request) throws IOException, JSONException {
        return new JSONObject(request.getReader().lines().collect(Collectors.joining()));
    }

    @Deprecated
    private Section getElementSection(Element element) {
        if (element == null) {
            return null;
        }

        if (element instanceof Section) {
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

    protected boolean ignoreNonce() {
        return "true".equalsIgnoreCase(getPropertyString("ignoreNonce"));
    }

    protected Object deepReplaceOtp(Object value, String find, String replaceWith) {
        if (value instanceof String) {
            return ((String) value).replaceAll(find, replaceWith);
        } else if (value instanceof Map) {
            return Optional.of((Map<String, Object>) value)
                    .map(Map::entrySet)
                    .map(Collection::stream)
                    .orElseGet(Stream::empty)
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> deepReplaceOtp(e.getValue(), find, replaceWith)));
        } else {
            return value;
        }
    }
}
