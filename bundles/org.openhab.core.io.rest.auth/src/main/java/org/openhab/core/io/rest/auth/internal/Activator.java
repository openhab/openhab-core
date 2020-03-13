/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.io.rest.auth.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Registers dynamic features contained in this bundle.
 *
 * @author Yannick Schaus - initial contribution
 */
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
