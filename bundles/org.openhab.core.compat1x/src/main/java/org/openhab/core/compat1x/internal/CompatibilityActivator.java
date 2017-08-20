/**
 * Copyright (c) 2015-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.core.compat1x.internal;

import org.eclipse.smarthome.model.script.engine.ScriptEngine;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.ItemRegistry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class CompatibilityActivator implements BundleActivator {

    private static BundleContext context;

    public static ServiceTracker<ItemRegistry, ItemRegistry> itemRegistryTracker;
    public static ServiceTracker<EventPublisher, EventPublisher> eventPublisherTracker;
    public static ServiceTracker<ScriptEngine, ScriptEngine> scriptEngineTracker;

    static public BundleContext getContext() {
        return context;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext bundleContext) throws Exception {
        CompatibilityActivator.context = bundleContext;

        itemRegistryTracker = new ServiceTracker<>(bundleContext, ItemRegistry.class, null);
        itemRegistryTracker.open();

        eventPublisherTracker = new ServiceTracker<>(bundleContext, EventPublisher.class, null);
        eventPublisherTracker.open();

        scriptEngineTracker = new ServiceTracker<>(bundleContext, ScriptEngine.class, null);
        scriptEngineTracker.open();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        CompatibilityActivator.context = null;
        itemRegistryTracker.close();
        eventPublisherTracker.close();
        scriptEngineTracker.close();
    }

}
