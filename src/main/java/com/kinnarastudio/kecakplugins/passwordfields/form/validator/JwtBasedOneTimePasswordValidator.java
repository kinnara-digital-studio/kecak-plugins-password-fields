package com.kinnarastudio.kecakplugins.passwordfields.form.validator;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.kecakplugins.passwordfields.form.binder.JwtBasedOneTimePasswordLoadBinder;
import io.jsonwebtoken.ExpiredJwtException;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormValidator;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.ResourceBundleUtil;
import org.joget.plugin.base.PluginManager;
import org.kecak.apps.app.service.AuthTokenService;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * @author aristo
 * <p>
 * Validate password generated by {@link JwtBasedOneTimePasswordLoadBinder}
 */
public class JwtBasedOneTimePasswordValidator extends FormValidator {
    public final static String LABEL = "JWT One-Time Password Validator";

    @Override
    public String getElementDecoration() {
        return "*";
    }

    @Override
    public boolean validate(Element element, FormData formData, String[] values) {
        final AuthTokenService authTokenService = (AuthTokenService) AppUtil.getApplicationContext().getBean("authTokenService");

        final String elementId = element.getPropertyString("id");
        final String elementLabel = element.getPropertyString("label");

        final String[] onetimePasswords = ((Map<String, String>)getRow(element, formData).getCustomProperties()).entrySet().stream()
                .filter(e -> e.getKey().startsWith(JwtBasedOneTimePasswordLoadBinder.JWT_KEY + "-"))
                .map(Map.Entry::getValue)
                .map(Try.onFunction( token -> authTokenService.getClaimDataFromToken(token, JwtBasedOneTimePasswordLoadBinder.PASSWORD_KEY, String.class), (RuntimeException e) -> {
                    formData.addFormError(elementId, "Token expired");
                    return null;
                }))
                .filter(Objects::nonNull)
                .toArray(String[]::new);

        return onetimePasswords.length > 0 && validateMandatory(formData, elementId, elementLabel, values, null) && validateToken(element, formData, onetimePasswords, values);
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
        return null;
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

    protected boolean validateToken(Element element, FormData formData, String[] storedPasswords, String[] values) {
        final String elementId = element.getPropertyString(FormUtil.PROPERTY_ID);

        boolean isValid = Arrays.stream(values).anyMatch(Arrays.asList(storedPasswords)::contains);

        if (!isValid) {
            formData.addFormError(elementId, "Invalid token");
            return false;
        }

        return true;
    }

    protected FormRow getRow(Element element, FormData formData) {
        final String requestParameter = FormUtil.getElementParameterName(element);

        return formData.getRequestParams().entrySet().stream()
                .filter(e -> e.getKey().startsWith(requestParameter + "-"))
                .collect(Collectors.toMap(e -> e.getKey().replaceAll("^" + requestParameter + "-", ""), e -> Arrays.stream(e.getValue()).findFirst().orElse(""), (a, b) -> a, FormRow::new));
    }
}