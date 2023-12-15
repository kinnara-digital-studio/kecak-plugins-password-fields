package com.kinnara.kecakplugins.passwordfields;

import com.kinnara.kecakplugins.passwordfields.commons.Utils;
import org.joget.plugin.base.DefaultApplicationPlugin;

import java.util.Map;

public class OneTimePasswordGeneratorTool extends DefaultApplicationPlugin implements Utils {
    @Override
    public String getName() {
        return "One Time Password Generator Tool";
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public Object execute(Map map) {
        return null;
    }

    @Override
    public String getLabel() {
        return getName();
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return null;
    }
}
