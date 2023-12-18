package com.kinnara.kecakplugins.passwordfields.hashvariable;

import org.joget.apps.app.model.DefaultHashVariablePlugin;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;

import java.util.Collection;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

public class OneTimePasswordHashVariable extends DefaultHashVariablePlugin {
    public final static String LABEL = "One Time Password Hash Variable";

    @Override
    public String getPrefix() {
        return "oneTimePassword";
    }

    @Override
    public String processHashVariable(String key) {
        LogUtil.info(getClassName(), "processHashVariable : key [" + key + "]");
        return key;
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
        syntax.add(this.getPrefix() + ".CURRENT_FORM.CURRENT_FIELD");
        return syntax;
    }
}
