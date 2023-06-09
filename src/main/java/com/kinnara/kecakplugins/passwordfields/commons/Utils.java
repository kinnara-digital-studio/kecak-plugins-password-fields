package com.kinnara.kecakplugins.passwordfields.commons;

import org.joget.apps.app.dao.AppDefinitionDao;
import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormService;
import org.joget.commons.util.SecurityUtil;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.WorkflowProcessLink;
import org.joget.workflow.model.dao.WorkflowProcessLinkDao;
import org.joget.workflow.model.service.WorkflowManager;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

public interface Utils {
    @Nonnull
    default Form generateForm(final String formDefId, final FormData formData) throws RestApiException {
        AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        AppService appService = (AppService) applicationContext.getBean("appService");

        return Optional.of(formDefId)
                .map(s -> {
                    try {
                        return appService.viewDataForm(appDefinition.getAppId(), appDefinition.getVersion().toString(), s, "", "", "", formData, "", "");
                    } catch (Exception e) { return null; }
                })
                .orElseThrow(() -> new RestApiException(HttpServletResponse.SC_NOT_FOUND, "Form [" + formDefId + "] not found"));
    }

    default String getRequiredBodyPayload(JSONObject requestBody, String parameterName) throws RestApiException {
        return Optional.of(parameterName)
                .map(requestBody::optString)
                .filter(s -> !s.isEmpty())
                .orElseThrow(() -> new RestApiException(HttpServletResponse.SC_BAD_REQUEST, "Request payload [" + parameterName + "] not found"));
    }

    default String getOptionalBodyPayload(JSONObject requestBody, String parameterName, String defaultValue) {
        return Optional.of(parameterName)
                .map(requestBody::optString)
                .filter(s -> !s.isEmpty())
                .orElse(defaultValue);
    }

    default FormRow storePassword(Form form, String primaryKey, String field, String tokenValue) {
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        FormDataDao formDataDao = (FormDataDao) applicationContext.getBean("formDataDao");

        FormRowSet rowSet = new FormRowSet();
        FormRow row = new FormRow();
        row.setId(primaryKey);
        row.setProperty(field, SecurityUtil.encrypt(tokenValue));
        rowSet.add(row);
        formDataDao.saveOrUpdate(form, rowSet);
        return formDataDao.load(form, primaryKey);
    }

    default WorkflowAssignment getAssignment(String activityId, String processId, String primaryKey) {
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        WorkflowManager workflowManager = (WorkflowManager) applicationContext.getBean("workflowManager");
        WorkflowProcessLinkDao workflowProcessLinkDao = (WorkflowProcessLinkDao) applicationContext.getBean("workflowProcessLinkDao");

        return Optional.ofNullable(activityId)
                .map(workflowManager::getAssignment)
                .orElseGet(() -> Optional.ofNullable(processId)
                        .map(workflowManager::getAssignmentByProcess)
                        .orElseGet(() -> Optional.ofNullable(primaryKey)
                                .map(workflowProcessLinkDao::getLinks)
                                .map(Collection::stream)
                                .orElseGet(Stream::empty)
                                .findFirst()
                                .map(WorkflowProcessLink::getProcessId)
                                .map(workflowManager::getAssignmentByProcess)
                                .orElse(null)));
    }

    default Form generateForm(String appId, String appVersion, String formDefId) {
        AppDefinitionDao appDefinitionDao = (AppDefinitionDao) AppUtil.getApplicationContext().getBean("appDefinitionDao");
        return generateForm(appDefinitionDao.loadVersion(appId, Long.valueOf(appVersion)), formDefId);
    }

    default Form generateForm(AppDefinition appDef, String formDefId) {
        // proceed without cache
        ApplicationContext appContext = AppUtil.getApplicationContext();
        FormService formService = (FormService) appContext.getBean("formService");

        if (appDef != null && formDefId != null && !formDefId.isEmpty()) {
            FormDefinitionDao formDefinitionDao =
                    (FormDefinitionDao)AppUtil.getApplicationContext().getBean("formDefinitionDao");

            FormDefinition formDef = formDefinitionDao.loadById(formDefId, appDef);
            if (formDef != null) {
                String json = formDef.getJson();
                Form form = (Form)formService.createElementFromJson(json);
                return form;
            }
        }
        return null;
    }

    @Nonnull
    default Stream<Element> elementStream(@Nonnull Element element, FormData formData) {
        if (!element.isAuthorize(formData)) {
            return Stream.empty();
        }

        Stream<Element> stream = Stream.of(element);
        for (Element child : element.getChildren()) {
            stream = Stream.concat(stream, elementStream(child, formData));
        }
        return stream;
    }

    default void storeToken(Form form, String primaryKey, String field, String tokenValue) {
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        FormDataDao formDataDao = (FormDataDao) applicationContext.getBean("formDataDao");

        FormRowSet rowSet = new FormRowSet();
        FormRow row = new FormRow();
        row.setId(primaryKey);
        row.setProperty(field, tokenValue);
        rowSet.add(row);
        formDataDao.saveOrUpdate(form, rowSet);
    }
}
