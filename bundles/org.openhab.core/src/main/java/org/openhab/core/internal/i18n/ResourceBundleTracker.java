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
package org.openhab.core.internal.i18n;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openhab.core.i18n.LocaleProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.BundleTracker;

/**
 * The {@link ResourceBundleTracker} class tracks all <i>OSGi</i> bundles which are in the {@link Bundle#RESOLVED} state
 * or which it already passed (e.g. {@link Bundle#STARTING} or {@link Bundle#ACTIVE}). Only bundles which contains i18n
 * resource files are considered within this tracker.
 * <p>
 * This tracker must be started by calling {@link #open()} and stopped by calling {@link #close()}.
 *
 * @author Michael Grammling - Initial contribution
 * @author Markus Rathgeb - Add locale provider support
 * @author Ana Dimova - fragments support
 */
@SuppressWarnings({ "deprecation", "rawtypes" })
public class ResourceBundleTracker extends BundleTracker {

    private LocaleProvider localeProvider;
    private Map<Bundle, LanguageResourceBundleManager> bundleLanguageResourceMap;
    private PackageAdmin pkgAdmin;

    @SuppressWarnings("unchecked")
    public ResourceBundleTracker(BundleContext bundleContext, LocaleProvider localeProvider) {
        super(bundleContext, Bundle.RESOLVED | Bundle.STARTING | Bundle.STOPPING | Bundle.ACTIVE, null);
        this.localeProvider = localeProvider;
        pkgAdmin = (PackageAdmin) bundleContext
                .getService(bundleContext.getServiceReference(PackageAdmin.class.getName()));
        this.bundleLanguageResourceMap = new LinkedHashMap<>();
    }

    @Override
    public synchronized void open() {
        super.open();
    }

    @Override
    public synchronized void close() {
        super.close();
        this.bundleLanguageResourceMap.clear();
    }

    @Override
    public synchronized Object addingBundle(Bundle bundle, BundleEvent event) {
        if (isFragmentBundle(bundle)) {
            List<Bundle> hosts = returnHostBundles(bundle);
            for (Bundle host : hosts) {
                addResourceBundle(host);
            }
        } else {
            addResourceBundle(bundle);
        }
        return bundle;
    }

    @Override
    public synchronized void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        LanguageResourceBundleManager languageResource = this.bundleLanguageResourceMap.remove(bundle);
        if (languageResource != null) {
            languageResource.clearCache();
        }
        if (isFragmentBundle(bundle)) {
            List<Bundle> hosts = returnHostBundles(bundle);
            for (Bundle host : hosts) {
                this.bundleLanguageResourceMap.remove(host);
                addResourceBundle(host);
            }
        } else {
            this.bundleLanguageResourceMap.remove(bundle);
        }
    }

    /**
     * Returns the {@link LanguageResourceBundleManager} instance for the specified bundle,
     * or {@code null} if it cannot be found within that tracker.
     *
     * @param bundle the bundle which points to the specific resource manager (could be null)
     * @return the specific resource manager (could be null)
     */
    public LanguageResourceBundleManager getLanguageResource(Bundle bundle) {
        if (bundle != null) {
            return this.bundleLanguageResourceMap.get(bundle);
        }

        return null;
    }

    /**
     * Returns all {@link LanguageResourceBundleManager} instances managed by this tracker.
     *
     * @return the list of all resource managers (not null, could be empty)
     */
    public Collection<LanguageResourceBundleManager> getAllLanguageResources() {
        return this.bundleLanguageResourceMap.values();
    }

    /**
     * This method is used to get the host bundles of the parameter which is a fragment bundle.
     *
     * @param bundle an OSGi fragment bundle.
     * @return a list with the hosts of the <code>fragment</code> parameter.
     */
    private List<Bundle> returnHostBundles(Bundle fragment) {
        List<Bundle> hosts = new ArrayList<>();
        Bundle[] bundles = pkgAdmin.getHosts(fragment);
        if (bundles != null) {
            for (int i = 0; i < bundles.length; i++) {
                hosts.add(bundles[i]);
            }
        }
        return hosts;
    }

    /**
     * This method checks if the <i>OSGi</i> bundle parameter is a fragment bundle.
     *
     * @param bundle the <i>OSGi</i> bundle that should be checked is it a fragment bundle.
     * @return <code>true</code> if the bundle is a fragment and <code>false</code> if it is a host.
     */
    private boolean isFragmentBundle(Bundle bundle) {
        return pkgAdmin.getBundleType(bundle) == PackageAdmin.BUNDLE_TYPE_FRAGMENT;
    }

    /**
     * This method adds the localization resources provided by this <i>OSGi</i> bundle parameter if the bundle is not in
     * UNINSTALLED state.
     *
     * @param bundle the <i>OSGi</i> bundle that was detected
     */
    private void addResourceBundle(Bundle bundle) {
        if (bundle.getState() != Bundle.UNINSTALLED) {
            LanguageResourceBundleManager languageResource = new LanguageResourceBundleManager(localeProvider, bundle);
            if (languageResource.containsResources()) {
                this.bundleLanguageResourceMap.put(bundle, languageResource);
            }
        }
    }

}
