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
package org.openhab.core.automation.internal.provider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.openhab.core.automation.Rule;
import org.openhab.core.automation.template.TemplateProvider;
import org.openhab.core.automation.type.ModuleTypeProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for tracking the bundles providing automation resources and delegating the processing to
 * the responsible providers in separate thread.
 *
 * @author Ana Dimova - Initial contribution
 * @author Kai Kreuzer - refactored (managed) provider and registry implementation
 * @param <E>
 */
public class AutomationResourceBundlesEventQueue<E> implements Runnable {

    /**
     * This field keeps instance of {@link Logger} that is used for logging.
     */
    protected final Logger logger = LoggerFactory.getLogger(AutomationResourceBundlesEventQueue.class);

    /**
     * This field serves for saving the BundleEvents for the bundles providing automation resources until their
     * processing completes.
     */
    private List<BundleEvent> queue = new ArrayList<>();

    /**
     * This field is for synchronization purposes
     */
    private boolean running = false;

    private Thread runningThread;

    /**
     * This field is for synchronization purposes
     */
    private boolean closed = false;

    /**
     * This field is for synchronization purposes
     */
    private boolean shared = false;

    private final AbstractResourceBundleProvider<E> provider;

    /**
     * This constructor is responsible for initializing a queue for bundles providing automation resources.
     *
     * @param provider
     *            is a reference to an implementation of {@link TemplateProvider} or {@link ModuleTypeProvider} or an
     *            importer of {@link Rule}s.
     */
    public AutomationResourceBundlesEventQueue(AbstractResourceBundleProvider<E> provider) {
        this.provider = provider;
    }

    /**
     * When a new event for a bundle providing automation resources is received, this will causes a creation of a new
     * thread if there is no other created yet. If the thread already exists, then it will be notified for the event.
     * Starting the thread will cause the execution of this method in separate thread.
     * <p>
     * The general contract of this method <code>run</code> is invoking of the
     * {@link #processBundleChanged(BundleEvent)} method and executing it in separate thread.
     *
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        boolean waitForEvents = true;
        while (true) {
            List<BundleEvent> lQueue = null;
            synchronized (this) {
                if (closed) {
                    notifyAll();
                    return;
                }
                if (queue.isEmpty()) {
                    if (waitForEvents) {
                        try {
                            wait(180000);
                        } catch (Throwable t) {
                        }
                        waitForEvents = false;
                        continue;
                    }
                    running = false;
                    runningThread = null;
                    notifyAll();
                    return;
                }
                lQueue = queue;
                shared = true;
            }
            Iterator<BundleEvent> events = lQueue.iterator();
            while (events.hasNext()) {
                BundleEvent event = events.next();
                try {
                    processBundleChanged(event);
                    synchronized (this) {
                        if (closed) {
                            notifyAll();
                            return;
                        }
                    }
                } catch (Throwable t) {
                    if (!closed && !(t instanceof IllegalStateException)) {
                        logger.warn("Processing bundle event {}, for automation resource bundle '{}' failed",
                                event.getType(), event.getBundle().getSymbolicName(), t);
                    }
                }
            }
            synchronized (this) {
                if (shared) {
                    queue.clear();
                }
                shared = false;
                waitForEvents = true;
                notifyAll();
            }
        }
    }

    /**
     * This method is invoked when this component is deactivated to stop the separate thread if still running.
     */
    public void stop() {
        synchronized (this) {
            closed = true;
            notifyAll();
        }
        Thread runningThread = this.runningThread;
        if (runningThread != null) {
            try {
                runningThread.join(30000);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * This method is called when a new event for a bundle providing automation resources is received. It causes a
     * creation of a new thread if there is no other created yet and starting the it. If the thread already exists,
     * it is waiting for events and will be notified for the event.
     *
     * @param bundle providing automation resources
     * @param event for a bundle tracked by the {@code BundleTracker}. It has been for adding, modifying or removing the
     *            bundle.
     */
    protected synchronized void addEvent(Bundle bundle, BundleEvent event) {
        if (closed) {
            return;
        }
        if (shared) {
            queue = new LinkedList<>();
            shared = false;
        }
        if (queue.add(event)) {
            logger.debug("Process bundle event {}, for automation bundle '{}' ", event.getType(),
                    event.getBundle().getSymbolicName());
            if (running) {
                notifyAll();
            } else {
                runningThread = new Thread(this, "Automation Provider Processing Queue");
                runningThread.start();
                running = true;
            }
        }
    }

    /**
     * Depending on the action committed against the bundle supplier of automation resources, this method performs the
     * appropriate action - calls for it's host bundles:
     * <ul>
     * {@link AbstractResourceBundleProvider#processAutomationProviderUninstalled(Bundle)} method
     * </ul>
     * or
     * <ul>
     * {@link AbstractResourceBundleProvider#processAutomationProvider(Bundle)} method
     * </ul>
     *
     * @param event for a bundle tracked by the {@code BundleTracker}. It has been for adding, modifying or removing the
     *            bundle.
     */
    protected void processBundleChanged(BundleEvent event) {
        Bundle bundle = event.getBundle();
        if (HostFragmentMappingUtil.isFragmentBundle(bundle)) {
            for (Bundle host : HostFragmentMappingUtil.returnHostBundles(bundle)) {
                provider.processAutomationProvider(host);
            }
        } else {
            switch (event.getType()) {
                case BundleEvent.UNINSTALLED:
                    provider.processAutomationProviderUninstalled(bundle);
                    break;
                default:
                    provider.processAutomationProvider(bundle);
            }
        }
    }

    /**
     * This method is responsible for initializing the queue with all already received BundleEvents and starting a
     * thread that should process them.
     *
     * @param queue list with all already received BundleEvents
     */
    protected synchronized void addAll(List<BundleEvent> queue) {
        if (closed) {
            return;
        }
        if (shared) {
            this.queue = new LinkedList<>();
            shared = false;
        }
        if (this.queue.addAll(queue)) {
            if (running) {
                notifyAll();
            } else {
                runningThread = new Thread(this, "Automation Provider Processing Queue");
                runningThread.start();
                running = true;
            }
        }
    }

}
