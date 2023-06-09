package com.kinnara.kecakplugins.passwordfields;

import java.util.ArrayList;
import java.util.Collection;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Register plugin here
        registrationList.add(context.registerService(AutoGeneratePasswordField.class.getName(), new AutoGeneratePasswordField(), null));
//        registrationList.add(context.registerService(OneTimePasswordField.class.getName(), new OneTimePasswordField(), null));
//        registrationList.add(context.registerService(OneTimePasswordValidator.class.getName(), new OneTimePasswordValidator(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}