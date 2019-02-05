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
package org.eclipse.smarthome.core.service;

import org.osgi.framework.Bundle;

/**
 * Utilities for the ready maker usage.
 *
 * @author Markus Rathgeb - Initial contribution and API
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

}
