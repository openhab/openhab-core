/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.core.internal;

import java.security.Permission;

import org.eclipse.smarthome.model.rule.runtime.RuleEngine;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This is the activator of the core openHAB bundle.
 *
 * @author Kai Kreuzer
 * @author Thomas.Eichstaedt-Engelen
 *
 */
public class CoreActivator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        startRuleEngine(context);
    }

    private void startRuleEngine(BundleContext context) throws InterruptedException {
        // TODO: This is a workaround as long as we cannot determine the time when all models have been loaded
        Thread.sleep(2000);

        // we now request the RuleEngine, so that it is activated and starts processing the rules
        // TODO: This should probably better be moved in a new bundle, so that the core bundle does
        // not have a (optional) dependency on model.rule.runtime anymore.
        try {
            ServiceTracker<RuleEngine, RuleEngine> tracker = new ServiceTracker<>(context, RuleEngine.class, null);
            tracker.open();
            tracker.waitForService(10000);
        } catch (NoClassDefFoundError e) {
        }

        // see https://github.com/eclipse/smarthome/issues/6291
        final SecurityManager securityManager = new SecurityManager() {
            @Override
            public void checkPermission(Permission permission) {
                if (permission.getName().contains("exitVM")) {
                    throw new SecurityException("Prevented System.exit() call.");
                }
            }
        };
        System.setSecurityManager(securityManager);

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        System.setSecurityManager(null);
    }

}
