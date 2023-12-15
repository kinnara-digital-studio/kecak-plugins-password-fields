package com.kinnara.kecakplugins.passwordfields;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.ResourceBundleUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.plugin.base.PluginManager;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * @author aristo
 *
 */
public class OneTimePasswordValidator extends FormValidator {
    @Override
    public String getElementDecoration() {
        return "*";
    }

    @Override
    public boolean validate(Element element, FormData formData, String[] strings) {
        String elementId = element.getPropertyString("id");
        String elementLabel = element.getPropertyString("label");

        return validateMandatory(formData, elementId, elementLabel, strings, null) && validateToken(element, formData, strings);
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
        return "One-Time Password Validator";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/OneTimePasswordValidator.json", null, false, "/messages/OneTimePassword");
    }

    protected boolean validateMandatory(FormData data, String id, String label, String[] values, String message) {
        boolean result = true;
        if (message == null || message.isEmpty()) {
            message = ResourceBundleUtil.getMessage("form.defaultvalidator.err.missingValue");
        }

        if (values == null || values.length == 0) {
            result = false;
            if (id != null) {
                data.addFormError(id, message);
            }
        } else {
            for (String val : values) {
                if (val == null || val.trim().length() == 0) {
                    result = false;
                    data.addFormError(id, message);
                    break;
                }
            }
        }
        return result;
    }

    protected boolean validateToken(Element element, FormData formData, String[] values) {
        String elementId = element.getPropertyString("id");

        Form form = FormUtil.findRootForm(element);
        FormData resultFormData = FormUtil.executeLoadBinders(form, formData);
        FormRowSet rowSet = resultFormData.getLoadBinderData(form);

        FormRow row = Optional.ofNullable(rowSet)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .findFirst()
                .orElse(new FormRow());

        boolean isValid = Optional.of(row)
                .map(r -> r.getProperty(elementId))
                .map(SecurityUtil::decrypt)
                .filter(val -> Arrays.stream(values).anyMatch(val::equalsIgnoreCase))
                .isPresent();

        if(!isValid) {
            formData.addFormError(elementId, "Invalid token");
            return false;
        }

        boolean isExpired = isTokenExpired(row.getDateModified());
        if(isExpired) {
            formData.addFormError(elementId, "Token is expired");
            return false;
        }

        return true;
    }

    private boolean isTokenExpired(Date modifiedDate) {
        int expires = Optional.ofNullable(getPropertyString("expires")).map(s -> {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return 0;
            }
        }).orElse(0);
        if(expires == 0)
            return false;

        long diffInMilies = Math.abs(modifiedDate.getTime() - new Date().getTime());
        long diffInMin = TimeUnit.MILLISECONDS.toMinutes(diffInMilies);

        return expires < diffInMin;
    }
}
