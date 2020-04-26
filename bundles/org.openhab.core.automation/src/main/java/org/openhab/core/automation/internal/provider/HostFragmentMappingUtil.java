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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * @author Ana Dimova - Initial contribution
 */
@SuppressWarnings("deprecation")
public class HostFragmentMappingUtil {

    private static Map<Bundle, List<Bundle>> hostFragmentMapping = new HashMap<>();

    static PackageAdmin pkgAdmin;

    /**
     * @return
     */
    static Set<Entry<Bundle, List<Bundle>>> getMapping() {
        return hostFragmentMapping.entrySet();
    }

    /**
     * This method is used to get the host bundles of the parameter which is a fragment bundle.
     *
     * @param pkgAdmin
     *
     * @param bundle an OSGi fragment bundle.
     * @return a list with the hosts of the <code>fragment</code> parameter.
     */
    static List<Bundle> returnHostBundles(Bundle fragment) {
        List<Bundle> hosts = new ArrayList<>();
        Bundle[] bundles = pkgAdmin.getHosts(fragment);
        if (bundles != null) {
            hosts = Arrays.asList(bundles);
        } else {
            for (Bundle host : hostFragmentMapping.keySet()) {
                if (hostFragmentMapping.get(host).contains(fragment)) {
                    hosts.add(host);
                }
            }
        }
        return hosts;
    }

    static List<Bundle> fillHostFragmentMapping(Bundle host) {
        List<Bundle> fragments = new ArrayList<>();
        Bundle[] bundles = pkgAdmin.getFragments(host);
        if (bundles != null) {
            fragments = Arrays.asList(bundles);
        }
        synchronized (hostFragmentMapping) {
            hostFragmentMapping.put(host, fragments);
        }
        return fragments;
    }

    static void fillHostFragmentMapping(List<Bundle> hosts) {
        for (Bundle host : hosts) {
            fillHostFragmentMapping(host);
        }
    }

    static boolean needToProcessFragment(Bundle fragment, List<Bundle> hosts) {
        if (hosts.isEmpty()) {
            return false;
        }
        synchronized (hostFragmentMapping) {
            for (Bundle host : hosts) {
                List<Bundle> fragments = hostFragmentMapping.get(host);
                if (fragments != null && fragments.contains(fragment)) {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean isFragmentBundle(Bundle bundle) {
        PackageAdmin pkgAdmin = HostFragmentMappingUtil.pkgAdmin;
        if (pkgAdmin == null) {
            throw new IllegalStateException();
        }
        return pkgAdmin.getBundleType(bundle) == PackageAdmin.BUNDLE_TYPE_FRAGMENT;
    }
}
