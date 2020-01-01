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
package org.openhab.core.service;

import java.util.Arrays;

import org.osgi.framework.Bundle;

/**
 * Utilities for the ready maker usage.
 *
 * @author Markus Rathgeb - Initial contribution
 */
public class ReadyMarkerUtils {

    /**
     * Gets an identifier for a bundle.
     *
     * @param bundle the bundle
     * @return an identifier
     */
    public static String getIdentifier(final Bundle bundle) {
        final String bsn = bundle.getSymbolicName();
        if (bsn != null) {
            return bsn;
        } else {
            return String.format("@bundleId@0x%x", bundle.getBundleId());
        }
    }

    /**
     * Provides a string to debug bundle information.
     *
     * @param bundle the bundle
     * @return a debug string
     */
    public static String debugString(final Bundle bundle) {
        return "Bundle [getState()=" + bundle.getState() + ", getHeaders()=" + bundle.getHeaders() + ", getBundleId()="
                + bundle.getBundleId() + ", getLocation()=" + bundle.getLocation() + ", getRegisteredServices()="
                + Arrays.toString(bundle.getRegisteredServices()) + ", getServicesInUse()="
                + Arrays.toString(bundle.getServicesInUse()) + ", getSymbolicName()=" + bundle.getSymbolicName()
                + ", getLastModified()=" + bundle.getLastModified() + ", getBundleContext()="
                + bundle.getBundleContext() + ", getVersion()=" + bundle.getVersion() + "]";
    }

}
