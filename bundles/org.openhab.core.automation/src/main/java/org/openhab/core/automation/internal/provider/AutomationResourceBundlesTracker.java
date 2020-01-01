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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openhab.core.automation.ManagedRuleProvider;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.parser.Parser;
import org.openhab.core.automation.template.Template;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.common.registry.Provider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * This class is responsible for tracking the bundles - suppliers of automation resources. It implements
 * {@link BundleTrackerCustomizer} and is notified for events for adding, modifying or removing the bundles.
 *
 * @author Ana Dimova - Initial contribution
 */
@SuppressWarnings("deprecation")
@Component(immediate = true)
public class AutomationResourceBundlesTracker implements BundleTrackerCustomizer<Bundle> {

    /**
     * This field holds a list with an {@link AutomationResourceBundlesEventQueue} instances owned by
     * {@link AbstractResourceBundleProvider}s of {@link ModuleType}s, {@link Template}s and {@link Rule}s.
     */
    @SuppressWarnings("rawtypes")
    private final List<AutomationResourceBundlesEventQueue> providerEventsQueue = new ArrayList<>();

    /**
     * This field holds a reference to an importer of {@link Rule}s.
     */
    protected RuleResourceBundleImporter rImporter;

    /**
     * This field is a bundle tracker for bundles providing automation resources.
     */
    private BundleTracker<Bundle> bTracker;

    /**
     * This field serves for saving the BundleEvents for the bundles providing automation resources until their
     * processing completes. The events have been for adding, modifying or removing a bundle.
     */
    private final List<BundleEvent> queue = new LinkedList<>();

    public AutomationResourceBundlesTracker() {
        rImporter = createImporter();
    }

    protected RuleResourceBundleImporter createImporter() {
        return new RuleResourceBundleImporter();
    }

    @Activate
    protected void activate(BundleContext bc) {
        bTracker = new BundleTracker<>(bc, ~Bundle.UNINSTALLED, this);
        bTracker.open();
    }

    @Deactivate
    protected void deactivate(BundleContext bc) {
        bTracker.close();
        bTracker = null;
        rImporter.deactivate();
    }

    @SuppressWarnings({ "rawtypes" })
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, target = "(provider.type=bundle)")
    protected void addProvider(Provider provider) {
        if (provider instanceof AbstractResourceBundleProvider) {
            addAbstractResourceBundleProvider((AbstractResourceBundleProvider) provider);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void addAbstractResourceBundleProvider(AbstractResourceBundleProvider provider) {
        AutomationResourceBundlesEventQueue queue = provider.getQueue();
        synchronized (this.queue) {
            queue.addAll(this.queue);
            providerEventsQueue.add(queue);
        }
    }

    @SuppressWarnings({ "rawtypes" })
    protected void removeProvider(Provider provider) {
        if (provider instanceof AbstractResourceBundleProvider) {
            removeAbstractResourceBundleProvider((AbstractResourceBundleProvider) provider);
        }
    }

    @SuppressWarnings({ "rawtypes" })
    protected void removeAbstractResourceBundleProvider(AbstractResourceBundleProvider provider) {
        AutomationResourceBundlesEventQueue queue = provider.getQueue();
        synchronized (this.queue) {
            providerEventsQueue.remove(queue);
        }
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setManagedRuleProvider(ManagedRuleProvider mProvider) {
        rImporter.setManagedRuleProvider(mProvider);
        rImporter.activate(null);
        addAbstractResourceBundleProvider(rImporter);
    }

    protected void unsetManagedRuleProvider(ManagedRuleProvider mProvider) {
        removeAbstractResourceBundleProvider(rImporter);
        rImporter.deactivate();
    }

    /**
     * This method provides functionality for tracking {@link Parser} services.
     *
     * @param parser {@link Parser} service
     * @param properties of the service that has been added.
     */

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, target = "(parser.type=parser.rule)")
    protected void addParser(Parser<Rule> parser, Map<String, String> properties) {
        rImporter.addParser(parser, properties);
    }

    /**
     * This method provides functionality for tracking {@link Parser} services.
     *
     * @param parser {@link Parser} service
     * @param properties of the service that has been removed.
     */
    protected void removeParser(Parser<Rule> parser, Map<String, String> properties) {
        rImporter.removeParser(parser, properties);
    }

    @Reference
    protected void setPackageAdmin(PackageAdmin pkgAdmin) {
        HostFragmentMappingUtil.pkgAdmin = pkgAdmin;
    }

    protected void unsetPackageAdmin(PackageAdmin pkgAdmin) {
        HostFragmentMappingUtil.pkgAdmin = null;
    }

    /**
     * A bundle that provides automation resources is being added to the {@code BundleTracker}.
     *
     * <p>
     * This method is called before a bundle that provides automation resources is added to the {@code BundleTracker}.
     * This method returns the object to be tracked for the specified {@code Bundle}. The returned object is stored in
     * the {@code BundleTracker} and is available from the {@link BundleTracker#getObject(Bundle) getObject} method.
     *
     * @param bundle The {@code Bundle} being added to the {@code BundleTracker} .
     * @param event The bundle event which caused this customizer method to be
     *            called or {@code null} if there is no bundle event associated with
     *            the call to this method.
     * @return The object to be tracked for the specified {@code Bundle} object
     *         or {@code null} if the specified {@code Bundle} object should not
     *         be tracked.
     */
    @Override
    public Bundle addingBundle(Bundle bundle, BundleEvent event) {
        if (isAnAutomationProvider(bundle)) {
            if (HostFragmentMappingUtil.isFragmentBundle(bundle)) {
                List<Bundle> hosts = HostFragmentMappingUtil.returnHostBundles(bundle);
                if (HostFragmentMappingUtil.needToProcessFragment(bundle, hosts)) {
                    addEvent(bundle, event);
                    HostFragmentMappingUtil.fillHostFragmentMapping(hosts);
                }
            } else {
                HostFragmentMappingUtil.fillHostFragmentMapping(bundle);
                addEvent(bundle, event);
            }
        } else if (!HostFragmentMappingUtil.isFragmentBundle(bundle)) {
            List<Bundle> fragments = HostFragmentMappingUtil.fillHostFragmentMapping(bundle);
            for (Bundle fragment : fragments) {
                if (isAnAutomationProvider(fragment)) {
                    addEvent(bundle, event);
                    break;
                }
            }
        }
        return bundle;
    }

    /**
     * A bundle tracked by the {@code BundleTracker} has been modified.
     *
     * <p>
     * This method is called when a bundle being tracked by the {@code BundleTracker} has had its state modified.
     *
     * @param bundle The {@code Bundle} whose state has been modified.
     * @param event The bundle event which caused this customizer method to be
     *            called or {@code null} if there is no bundle event associated with
     *            the call to this method.
     * @param object The tracked object for the specified bundle.
     */
    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Bundle object) {
        int type = event.getType();
        if (type == BundleEvent.UPDATED || type == BundleEvent.RESOLVED) {
            addEvent(bundle, event);
        }
    }

    /**
     * A bundle tracked by the {@code BundleTracker} has been removed.
     *
     * <p>
     * This method is called after a bundle is no longer being tracked by the {@code BundleTracker}.
     *
     * @param bundle The {@code Bundle} that has been removed.
     * @param event The bundle event which caused this customizer method to be
     *            called or {@code null} if there is no bundle event associated with
     *            the call to this method.
     * @param object The tracked object for the specified bundle.
     */
    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
        if (HostFragmentMappingUtil.isFragmentBundle(bundle)) {
            for (Entry<Bundle, List<Bundle>> entry : HostFragmentMappingUtil.getMapping()) {
                if (entry.getValue().contains(bundle)) {
                    Bundle host = entry.getKey();
                    addEvent(host, new BundleEvent(BundleEvent.UPDATED, host));
                }
            }
        } else {
            addEvent(bundle, event);
        }
    }

    /**
     * This method is called when a new event for a bundle providing automation resources is received. It causes a
     * creation of a new thread if there is no other created yet and starting the thread. If the thread already exists,
     * it is waiting for events and will be notified for the event.
     *
     * @param bundle
     *
     * @param event for a bundle tracked by the {@code BundleTracker}. It has been for adding, modifying or removing the
     *            bundle.
     */
    @SuppressWarnings({ "rawtypes" })
    protected void addEvent(Bundle bundle, BundleEvent event) {
        BundleEvent e = event != null ? event : initializeEvent(bundle);
        synchronized (queue) {
            queue.add(e);
            for (AutomationResourceBundlesEventQueue queue : providerEventsQueue) {
                queue.addEvent(bundle, e);
            }
        }
    }

    private BundleEvent initializeEvent(Bundle bundle) {
        switch (bundle.getState()) {
            case Bundle.INSTALLED:
                return new BundleEvent(BundleEvent.INSTALLED, bundle);
            case Bundle.RESOLVED:
                return new BundleEvent(BundleEvent.RESOLVED, bundle);
            default:
                return new BundleEvent(BundleEvent.STARTED, bundle);
        }
    }

    /**
     * This method is used to check if the specified {@code Bundle} contains resource files providing automation
     * resources.
     *
     * @param bundle is a {@link Bundle} object to check.
     * @return <tt>true</tt> if the specified {@link Bundle} contains resource files providing automation
     *         resources, <tt>false</tt> otherwise.
     */
    private boolean isAnAutomationProvider(Bundle bundle) {
        return bundle.getEntryPaths(AbstractResourceBundleProvider.ROOT_DIRECTORY) != null;
    }

}
