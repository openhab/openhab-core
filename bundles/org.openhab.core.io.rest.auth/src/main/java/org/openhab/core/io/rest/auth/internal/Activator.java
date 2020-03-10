package org.openhab.core.io.rest.auth.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
    private static Activator instance;

    private ServiceRegistration rolesAllowedDynamicFeatureRegistration;

    public static Activator getInstance() {
        return instance;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        rolesAllowedDynamicFeatureRegistration = context.registerService(RolesAllowedDynamicFeatureImpl.class.getName(),
                new RolesAllowedDynamicFeatureImpl(), null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        instance = null;
        if (rolesAllowedDynamicFeatureRegistration != null) {
            rolesAllowedDynamicFeatureRegistration.unregister();
        }
    }
}
