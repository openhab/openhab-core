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
package org.eclipse.smarthome.io.rest.core.internal;

import org.eclipse.smarthome.io.rest.JSONResponse;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Extension of the default OSGi bundle activator
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Benedikt Niehues - made serviceRegistration of ExceptionMapper compatible with older OSGI versions
 */
public class RESTCoreActivator implements BundleActivator {

    private static BundleContext context;
    @SuppressWarnings("rawtypes")
    private ServiceRegistration mExcMapper;

    /**
     * Called whenever the OSGi framework starts our bundle
     */
    @Override
    public void start(BundleContext bc) throws Exception {
        context = bc;
        mExcMapper = bc.registerService(JSONResponse.ExceptionMapper.class.getName(), new JSONResponse.ExceptionMapper(), null);
    }

    /**
     * Called whenever the OSGi framework stops our bundle
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        RESTCoreActivator.context = null;
        mExcMapper.unregister();
    }

    public static BundleContext getBundleContext() {
        return context;
    }

}
