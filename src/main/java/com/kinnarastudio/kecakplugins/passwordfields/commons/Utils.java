package com.kinnarastudio.kecakplugins.passwordfields.commons;

import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.WorkflowProcessLink;
import org.joget.workflow.model.dao.WorkflowProcessLinkDao;
import org.joget.workflow.model.service.WorkflowManager;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

public interface Utils {
    Character[] DIC_NUMERIC = new Character[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
    Character[] DIC_UPPER = new Character[] { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };
    Character[] DIC_LOWER = new Character[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };
    Character[] DIC_SPECIAL = new Character[] { '!', '@', '#', '%', '^', '&', '*', '-', '+' };

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

    default WorkflowAssignment getAssignment(String activityId, String processId, @Nullable String primaryKey) {
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

    default FormRowSet storeToken(Element element, String primaryKey, String value) {
        final Form form = FormUtil.findRootForm(element);
        final String fieldId = element.getPropertyString(FormUtil.PROPERTY_ID);
        return storeToken(form,  primaryKey, fieldId, value);
    }

    default FormRowSet storeToken(Form form, String primaryKey, String field, String tokenValue) {
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        FormDataDao formDataDao = (FormDataDao) applicationContext.getBean("formDataDao");

        FormRowSet rowSet = new FormRowSet();
        FormRow row = new FormRow();
        row.setId(primaryKey);
        row.setProperty(field, tokenValue);
        rowSet.add(row);
        formDataDao.saveOrUpdate(form, rowSet);

        return rowSet;
    }

    default String generateRandomPassword(int digits, boolean numeric, boolean upperCase, boolean lowerCase, boolean specialCharacter) {
        Random rand = new Random();

        if(numeric && !upperCase && !lowerCase && !specialCharacter) {
            return String.format("%0" + digits + "d", rand.nextInt((int) Math.pow(10, digits)));
        }

        Character[] dictionary = Stream.of(Arrays.stream(DIC_NUMERIC).filter(c -> numeric), Arrays.stream(DIC_UPPER).filter(c -> upperCase), Arrays.stream(DIC_LOWER).filter(c -> lowerCase),Arrays.stream(DIC_SPECIAL).filter(c -> specialCharacter))
                .flatMap(stream -> stream)
                .toArray(Character[]::new);

        final StringBuilder sb = new StringBuilder();
        for(int i = 0; i < digits; i++) {
            sb.append(generateRandomCharacter(dictionary, rand));
        }
        return sb.toString();
    }

    default Character generateRandomCharacter(final Character[] dictionary, final Random rand) {
        final int len = dictionary.length;
        final int index = rand.nextInt(len);
        return dictionary[index];
    }
}
