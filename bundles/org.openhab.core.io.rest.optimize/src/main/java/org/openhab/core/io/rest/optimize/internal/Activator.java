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
package org.openhab.core.io.rest.optimize.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;

import com.eclipsesource.jaxrs.publisher.ResourceFilter;

/**
 * Activator
 *
 * It registers a {@link ResourceFilter} in order to prevent the JAX-RS implementation to
 * enforce starting all services once they are registered.
 *
 * @author Simon Kaufmann - Initial contribution
 */
public class Activator implements BundleActivator {

    private ServiceRegistration<?> resourceFilterRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        registerResourceFilter(context);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        unregisterResourceFilter();
    }

    private void registerResourceFilter(BundleContext context) throws InvalidSyntaxException {
        resourceFilterRegistration = context.registerService(ResourceFilter.class.getName(), new ResourceFilterImpl(),
                null);
    }

    private void unregisterResourceFilter() {
        if (resourceFilterRegistration != null) {
            resourceFilterRegistration.unregister();
            resourceFilterRegistration = null;
        }
    }
}
