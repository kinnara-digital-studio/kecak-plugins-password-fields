package com.kinnara.kecakplugins.passwordfields.hashvariable;

import org.joget.apps.app.model.DefaultHashVariablePlugin;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;

import java.util.Collection;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Hash variable to show one-time password value in notification plugin
 */
public class OneTimePasswordHashVariable extends DefaultHashVariablePlugin {
    public final static String LABEL = "One Time Password Hash Variable";

    @Override
    public String getPrefix() {
        return "oneTimePassword";
    }

    /**
     * Hash variable will not be processed as plugin
     *
     * @param key
     * @return
     */
    @Override
    public String processHashVariable(String key) {
        // value is not used
        return "";
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

    @Override
    public Collection<String> availableSyntax() {
        final Set<String> syntax = new HashSet<>();
        syntax.add(this.getPrefix());
        return syntax;
    }
}
