/**
 * Copyright (c) 2015-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.jetty.certificate.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

    private Logger logger;

    @Override
    public void start(BundleContext context) throws Exception {
        logger = LoggerFactory.getLogger(Activator.class);
        logger.debug("Certifiate Management bundle has started");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        logger.debug("Certifiate Management bundle has stopped");
    }

}
