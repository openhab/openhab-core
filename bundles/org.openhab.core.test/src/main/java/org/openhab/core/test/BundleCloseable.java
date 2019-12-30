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
package org.openhab.core.test;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * A wrapper that uninstalls a bundle on close.
 *
 * <p>
 * This wrapper allows the usage in try-with-resources blocks.
 *
 * @author Markus Rathgeb - Initial contribution
 */
public class BundleCloseable implements AutoCloseable {

    private final Bundle bundle;

    public BundleCloseable(final Bundle bundle) {
        this.bundle = bundle;
    }

    @Override
    public void close() throws BundleException {
        bundle.uninstall();
    }

    public Bundle bundle() {
        return bundle;
    }
}
