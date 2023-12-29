package com.kinnarastudio.kecakplugins.passwordfields;

import com.kinnarastudio.kecakplugins.passwordfields.form.binder.JwtBasedOneTimePasswordLoadBinder;
import com.kinnarastudio.kecakplugins.passwordfields.form.binder.OneTimePasswordLoadBinder;
import com.kinnarastudio.kecakplugins.passwordfields.form.element.OneTimePasswordField;
import com.kinnarastudio.kecakplugins.passwordfields.form.validator.JwtBasedOneTimePasswordValidator;
import com.kinnarastudio.kecakplugins.passwordfields.form.validator.OneTimePasswordValidator;
import com.kinnarastudio.kecakplugins.passwordfields.hashvariable.OneTimePasswordHashVariable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.ArrayList;
import java.util.Collection;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Register plugin here
//        registrationList.add(context.registerService(AutoGeneratePasswordField.class.getName(), new AutoGeneratePasswordField(), null));
        registrationList.add(context.registerService(OneTimePasswordField.class.getName(), new OneTimePasswordField(), null));

        // Element Binders
        registrationList.add(context.registerService(OneTimePasswordLoadBinder.class.getName(), new OneTimePasswordLoadBinder(), null));
        registrationList.add(context.registerService(JwtBasedOneTimePasswordLoadBinder.class.getName(), new JwtBasedOneTimePasswordLoadBinder(), null));

        // Validators
        registrationList.add(context.registerService(OneTimePasswordValidator.class.getName(), new OneTimePasswordValidator(), null));
        registrationList.add(context.registerService(JwtBasedOneTimePasswordValidator.class.getName(), new JwtBasedOneTimePasswordValidator(), null));

        // Hash Variables
        registrationList.add(context.registerService(OneTimePasswordHashVariable.class.getName(), new OneTimePasswordHashVariable(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}