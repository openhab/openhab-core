/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.automation.internal.sample.json.internal.handler;

import org.eclipse.smarthome.automation.handler.ModuleHandlerFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * OSGi Bundle Activator
 *
 * @author Vasil Ilchev - Initial Contribution
 * @author Benedikt Niehues - moved ModuleFactory registration
 */
public class Activator implements BundleActivator {
    private BundleContext bc;
    private SampleHandlerFactory sampleHandlerFactory;
    private SampleHandlerFactoryCommands commands;
    @SuppressWarnings("rawtypes")
    private ServiceRegistration factoryRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        bc = context;
        sampleHandlerFactory = new SampleHandlerFactory();
        this.factoryRegistration = bc.registerService(ModuleHandlerFactory.class.getName(), sampleHandlerFactory, null);
        commands = new SampleHandlerFactoryCommands(sampleHandlerFactory, bc);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        commands.stop();
        sampleHandlerFactory.deactivate();
        if (this.factoryRegistration != null) {
            this.factoryRegistration.unregister();
        }
        commands = null;
        sampleHandlerFactory = null;
        bc = null;
    }

}
