package com.kinnarastudio.kecakplugins.passwordfields.form.binder;

import com.kinnarastudio.kecakplugins.passwordfields.form.element.OneTimePasswordField;
import com.kinnarastudio.kecakplugins.passwordfields.form.validator.OneTimePasswordValidator;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;
import org.kecak.apps.app.service.AuthTokenService;

import java.util.*;

/**
 * Binder to generate one time password value. Later will be validated by {@link OneTimePasswordValidator}
 */
public class JwtBasedOneTimePasswordLoadBinder extends FormBinder implements FormLoadBinder {
    public final static String JWT_KEY = "token";
    public final static String LABEL = "JWT One Time Password Binder";
    public final static String PASSWORD_KEY = "otp";

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
        final String fieldId = element.getPropertyString(FormUtil.PROPERTY_ID);
        if (!(element instanceof OneTimePasswordField)) {
            LogUtil.warn(getClass().getName(), "Element [" + fieldId + "] is not [" + OneTimePasswordField.class.getSimpleName() + "]");
            return null;
        }

        final int digits = getDigits();
        final String password = generateRandomPassword(digits);

        AuthTokenService authTokenService = (AuthTokenService) AppUtil.getApplicationContext().getBean("authTokenService");

        String username = WorkflowUtil.getCurrentUsername();
        Map<String, Object> claims = new HashMap<>();
        claims.put(PASSWORD_KEY, password);

        final int expiredInMinutes = getExpiryTime();
        final String jwtToken = authTokenService.generateToken(username, claims, expiredInMinutes);

        final FormRowSet rowSet = new FormRowSet();
        final FormRow row = new FormRow();
        row.setId(primaryKey);
        row.setProperty(fieldId, password);
        row.setProperty(JWT_KEY + "-" + getTokenDifferentiator(), jwtToken);
        rowSet.add(row);

        return rowSet;
    }

    protected int getDigits() {
        return Optional.ofNullable(getPropertyString("digitsToken"))
                .filter(s -> s.matches("[0-9]+"))
                .map(Integer::parseInt)
                .orElse(4);
    }

    protected long getTokenDifferentiator() {
        final Date now = new Date();
        return now.getTime();
    }

    /**
     * Get Expiry Time
     *
     * @return (in minutes)
     */
    protected int getExpiryTime() {
        return 1;
    }
}
