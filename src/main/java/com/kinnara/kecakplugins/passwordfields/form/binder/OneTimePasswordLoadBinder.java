package com.kinnara.kecakplugins.passwordfields.form.binder;

import com.kinnara.kecakplugins.passwordfields.form.element.OneTimePasswordField;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.springframework.context.ApplicationContext;

import java.util.Optional;
import java.util.Random;
import java.util.ResourceBundle;

/**
 * Binder to generate one time password value. Later will be validated by {@link com.kinnara.kecakplugins.passwordfields.form.validator.OneTimePasswordValidator}
 */
public class OneTimePasswordLoadBinder extends FormBinder implements FormLoadBinder {
    public final static String LABEL = "One Time Password Binder";

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
        return AppUtil.readPluginResource(getClass().getName(), "/properties/OneTimaPasswordLoadBinder.json");
    }


    protected String generateRandomToken(int digits) {
        Random rand = new Random();
        return String.format("%0" + digits + "d", rand.nextInt((int) Math.pow(10, digits)));
    }

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        LogUtil.info(getClass().getName(), "load : primaryKey [" + primaryKey + "]");

        final String fieldId = element.getPropertyString(FormUtil.PROPERTY_ID);
        if (!(element instanceof OneTimePasswordField)) {
            LogUtil.warn(getClass().getName(), "Element [" + fieldId + "] is not [" + OneTimePasswordField.class.getSimpleName() + "]");
            return null;
        }

        if(primaryKey == null) {
            LogUtil.warn(getClass().getName(), "Primary key is not provided");
            return null;
        }

        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        FormDataDao formDataDao = (FormDataDao) applicationContext.getBean("formDataDao");

        final int digits = getDigits();
        final String tokenValue = generateRandomToken(digits);
        LogUtil.info(getClassName(), "Token generated [" + tokenValue + "]");

        final FormRowSet rowSet = new FormRowSet();
        final FormRow row = new FormRow();
        row.setId(primaryKey);
        row.setProperty(fieldId, tokenValue);
        rowSet.add(row);

        final Form form = FormUtil.findRootForm(element);
        formDataDao.saveOrUpdate(form, rowSet);

        return rowSet;
    }

    protected int getDigits() {
        return Optional.ofNullable(getPropertyString("digitsToken"))
                .filter(s -> s.matches("[0-9]+"))
                .map(Integer::parseInt)
                .orElse(4);
    }
}
