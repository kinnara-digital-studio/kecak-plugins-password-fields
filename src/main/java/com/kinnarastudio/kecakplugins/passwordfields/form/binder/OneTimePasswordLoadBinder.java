package com.kinnarastudio.kecakplugins.passwordfields.form.binder;

import com.kinnarastudio.kecakplugins.passwordfields.commons.Utils;
import com.kinnarastudio.kecakplugins.passwordfields.form.element.OneTimePasswordField;
import com.kinnarastudio.kecakplugins.passwordfields.form.validator.OneTimePasswordValidator;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;

import java.util.Optional;
import java.util.Random;
import java.util.ResourceBundle;

/**
 * Binder to generate one time password value. Later will be validated by {@link OneTimePasswordValidator}
 */
public class OneTimePasswordLoadBinder extends FormBinder implements FormLoadBinder, Utils {
    public final static String LABEL = "One-Time Password Binder";

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
        final String[] args = new String[] { getLabel() };
        return AppUtil.readPluginResource(getClass().getName(), "/properties/OneTimaPasswordLoadBinder.json", args, true, "/messages/OneTimePassword");
    }


    protected String generateRandomPassword(int digits) {
        Random rand = new Random();
        return String.format("%0" + digits + "d", rand.nextInt((int) Math.pow(10, digits)));
    }

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        if(primaryKey == null) {
            LogUtil.warn(getClass().getName(), "Primary key is not NULL");
            return null;
        }

        if (!(element instanceof OneTimePasswordField)) {
            final String fieldId = element.getPropertyString(FormUtil.PROPERTY_ID);
            LogUtil.warn(getClass().getName(), "Element [" + fieldId + "] is not [" + OneTimePasswordField.class.getSimpleName() + "]");
            return null;
        }

        final int digits = getDigits();
        final String otpValue = generateRandomPassword(digits);
        return storeToken(element, primaryKey, otpValue);
    }

    protected int getDigits() {
        return Optional.ofNullable(getPropertyString("digitsToken"))
                .filter(s -> s.matches("[0-9]+"))
                .map(Integer::parseInt)
                .orElse(4);
    }
}
