/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.io.transport.mdns.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Addon of the default OSGi bundle activator
 *
 * @author Kai Kreuzer - Initial contribution
 */
public final class MDNSActivator implements BundleActivator {

    private final Logger logger = LoggerFactory.getLogger(MDNSActivator.class);

    /**
     * Called whenever the OSGi framework starts our bundle
     */
    @Override
    public void start(BundleContext context) throws Exception {
        logger.debug("mDNS service has been started.");
    }

    /**
     * Called whenever the OSGi framework stops our bundle
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        logger.debug("mDNS service has been stopped.");
    }
}
