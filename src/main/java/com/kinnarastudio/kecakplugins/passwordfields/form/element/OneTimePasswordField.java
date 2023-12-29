package com.kinnarastudio.kecakplugins.passwordfields.form.element;

import com.kinnarastudio.kecakplugins.passwordfields.commons.RestApiException;
import com.kinnarastudio.kecakplugins.passwordfields.commons.Utils;
import com.kinnarastudio.kecakplugins.passwordfields.form.binder.JwtBasedOneTimePasswordLoadBinder;
import com.kinnarastudio.kecakplugins.passwordfields.form.validator.JwtBasedOneTimePasswordValidator;
import com.kinnarastudio.kecakplugins.passwordfields.hashvariable.OneTimePasswordHashVariable;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
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
    public final static String LABEL = "One-Time Password";
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
        // clear otp
        formData.getRequestParams().put(FormUtil.getElementParameterName(this), new String[]{""});

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

    protected String renderTemplate(String template, FormData formData, @SuppressWarnings("rawtypes") Map dataModel) {
        ApplicationContext appContext = AppUtil.getApplicationContext();
        WorkflowUserManager wum = (WorkflowUserManager) appContext.getBean("workflowUserManager");
        AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();

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
        return LABEL;
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
        return LABEL;
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        final String[] args = new String[]{
                JwtBasedOneTimePasswordLoadBinder.class.getName(),
                JwtBasedOneTimePasswordValidator.class.getName()
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

            final Optional<FormLoadBinder> optLoadBinder = Optional.of(element)
                    .map(e -> e.getProperty("generatorLoadBinder"))
                    .map(prop -> (FormLoadBinder) pluginManager.getPlugin((Map<String, Object>) prop));
            // generate and load token
            final FormRow row = optLoadBinder
                    .map(binder -> binder.load(element, primaryKey, formData))
                    .map(Collection::stream)
                    .orElseGet(Stream::empty)
                    .findFirst()
                    .orElseGet(FormRow::new);

            final String oneTimePassword = Optional.of(row)
                    .map(r -> {
                        final String otp = r.getProperty(fieldId);
                        r.remove(fieldId);
                        return otp;
                    })
                    .filter(s -> !s.isEmpty())
                    .orElseGet(() -> {
                        final String loadBinderPluginClass = optLoadBinder
                                .map(FormLoadBinder::getClass)
                                .map(Class::getName)
                                .orElse("");

                        LogUtil.warn(getClassName(), "One-Time Password has not been generated by plugin [" + loadBinderPluginClass + "] field [" + fieldId + "]");

                        return "";
                    });

            row.remove(fieldId);

            if (element.isDebug()) {
                LogUtil.info(getClassName(), "Password generated [" + oneTimePassword + "]");
            }

            final Form formToExecute = generateForm(formId, formData);
            final Element elementToExecute = FormUtil.findElement(fieldId, formToExecute, formData);
            final Map<String, Object> notificationToolProperty = (Map<String, Object>) elementToExecute.getProperty("notificationTool");
            final DefaultApplicationPlugin notificationToolPlugin = pluginManager.getPlugin(notificationToolProperty);
            final OneTimePasswordHashVariable oneTimePasswordHashVariable = (OneTimePasswordHashVariable) pluginManager.getPlugin(OneTimePasswordHashVariable.class.getName());

            if (notificationToolPlugin != null && oneTimePasswordHashVariable != null) {
                final String prefix = oneTimePasswordHashVariable.getPrefix();

                final Map<String, Object> executionProperties = (Map<String, Object>) deepReplaceOtp(notificationToolProperty.get("properties"), "(?<=[#{]{1}" + prefix + ")(?=[#}]{1})", "." + oneTimePassword);

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
                jsonResponse.put("row", new JSONObject(row));
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

    protected boolean isDebug() {
        return "true".equalsIgnoreCase(getPropertyString("debug"));
    }
}
