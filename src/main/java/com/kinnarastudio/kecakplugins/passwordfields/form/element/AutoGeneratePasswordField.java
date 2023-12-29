package com.kinnarastudio.kecakplugins.passwordfields.form.element;

import com.kinnarastudio.kecakplugins.passwordfields.commons.RestApiException;
import com.kinnarastudio.kecakplugins.passwordfields.commons.Utils;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.PasswordField;
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

import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * @author aristo
 *
 * Auto generate password
 *
 */
public class AutoGeneratePasswordField extends PasswordField implements PluginWebSupport, Utils {
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

    @Override
    public String getFormBuilderTemplate() {
        return "<label class='label'>Auto Generate Password</label><input type='password' />";
    }

    @Override
    public int getFormBuilderPosition() {
        return 100;
    }

    @Override
    public String getFormBuilderCategory() {
        return "Kecak";
    }

    @Override
    public String renderTemplate(FormData formData, Map map) {
        return renderTemplate("AutoGeneratePasswordField.ftl", formData, map);
    }

    protected String renderTemplate(String template, FormData formData, @SuppressWarnings("rawtypes") Map dataModel){
        ApplicationContext appContext = AppUtil.getApplicationContext();
        WorkflowUserManager workflowUserManager = (WorkflowUserManager) appContext.getBean("workflowUserManager");
        AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();

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

        String currentUser = workflowUserManager.getCurrentUsername();
        if(currentUser!=null && !currentUser.isEmpty()) {
            dataModel.put(USERNAME, currentUser);
        }

        dataModel.put(PARAMETER_APP_ID, appDefinition.getAppId());
        dataModel.put(PARAMETER_APP_VERSION, appDefinition.getVersion());
        dataModel.put("className", getClassName());

        // set value
        String value = FormUtil.getElementPropertyValue(this, formData);
        String binderValue = getBinderValue(formData);

        if (value != null && !value.isEmpty() && (value.equals(binderValue) || (binderValue != null && value.equals(SecurityUtil.decrypt(binderValue))))) {
            value = SECURE_VALUE;
        }

        dataModel.put("value", value);

        HttpServletRequest servletRequest = WorkflowUtil.getHttpServletRequest();
        dataModel.put("requestParameter", servletRequest.getQueryString());

        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        return html;
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
        return "Autogen Password";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/AutoGeneratePasswordField.json", null, true, "/messages/AutoGeneratePasswordField").replaceAll("\"", "'");
    }

    @Override
    public void webService(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        LogUtil.info(getClass().getName(), "Executing JSON Rest API [" + httpServletRequest.getRequestURI() + "] in method [" + httpServletRequest.getMethod() + "] as [" + WorkflowUtil.getCurrentUsername() + "]");

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

            if(WorkflowUtil.isCurrentUserInRole(WorkflowUtil.ROLE_ADMIN)) {
                String adminUser = WorkflowUtil.getCurrentUsername();
                final String username = getOptionalBodyPayload(requestBody, "username", "");
                if(!username.isEmpty()) {
                    workflowUserManager.setCurrentThreadUser(username);
                    LogUtil.info(getClassName(), "Admin user [" + adminUser + "] is logging in as [" + username + "]");
                }
            }

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


            Form formForLoading = generateForm(formId, formData);
            Element elementForLoading = Optional.of(formForLoading)
                    .map(e -> FormUtil.findElement(fieldId, e, formData))
                    .orElseThrow(() -> new RestApiException(HttpServletResponse.SC_NOT_FOUND, "Form [" + formId + "] field [" + fieldId + "] not found"));

            // get current data
            final int digits = getTokenDigits(elementForLoading);
            final String randomPassword = generateRandomPassword(digits, true,  false, false, false);

            storePassword(formForLoading, primaryKey, fieldId, randomPassword);

            boolean ignoreNonce = "true".equalsIgnoreCase(elementForLoading.getPropertyString("ignoreNonce"));
            if(!ignoreNonce) {
                final String nonce = getRequiredBodyPayload(requestBody, "nonce");

                // validate nonce
                String[] nonceArgs = nonceArgumentsGenerator(appDefinition, formId, fieldId, primaryKey);
                if (!SecurityUtil.verifyNonce(nonce, nonceArgs)) {
                    throw new RestApiException(HttpServletResponse.SC_BAD_REQUEST, "Invalid nonce");
                }
            }

            Form formToExecute = generateForm(formId, formData);
            Element elementToExecute = Optional.of(formToExecute)
                    .map(e -> FormUtil.findElement(fieldId, e, formData))
                    .orElseThrow(() -> new RestApiException(HttpServletResponse.SC_NOT_FOUND, "Form [" + formId + "] field [" + fieldId + "] not found"));

            Map<String, Object> notificationToolProperty = (Map<String, Object>) elementToExecute.getProperty("notificationTool");
            DefaultApplicationPlugin notificationToolPlugin = pluginManager.getPlugin(notificationToolProperty);

            if(notificationToolPlugin != null) {
                Map<String, Object> executionProperties = (Map<String, Object>) notificationToolProperty.get("properties");

                executionProperties.put("pluginManager", pluginManager);
                executionProperties.put("appDef", appDefinition);
                if (assignment != null) {
                    executionProperties.put("workflowAssignment", assignment);
                }

                if ("true".equalsIgnoreCase(elementForLoading.getPropertyString("debug"))) {
                    LogUtil.info(getClassName(), "Sending password notification, new random password [" + randomPassword + "]");
                }

                notificationToolPlugin.execute(executionProperties);
            }

            JSONObject jsonResponse = new JSONObject();
            try {
                String successMessage = Optional.ofNullable(elementToExecute.getPropertyString("messageOnSuccessGenerateToken"))
                        .filter(s -> !s.isEmpty())
                        .orElse(AppPluginUtil.getMessage("autoGeneratePassword.messageOnSuccessGeneratePassword.default", getClassName(), "/messages/AutoGeneratePasswordField"));
                jsonResponse.put("message", successMessage);
                jsonResponse.put("randomPassword", randomPassword);
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


    private int getTokenDigits(Element element) {
        return Optional.of("digitsToken")
                .map(element::getPropertyString)
                .map(s -> {
                    try {
                        return Integer.parseInt(s);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .orElse(6);
    }
}
