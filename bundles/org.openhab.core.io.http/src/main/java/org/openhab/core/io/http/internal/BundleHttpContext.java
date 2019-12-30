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
package org.openhab.core.io.http.internal;

import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

/**
 * A bundle specific http context - delegates security and mime type handling to "parent" context.
 *
 * @author Łukasz Dywicki - Initial contribution
 */
class BundleHttpContext extends DelegatingHttpContext {

    private final Bundle bundle;

    BundleHttpContext(HttpContext delegate, Bundle bundle) {
        super(delegate);
        this.bundle = bundle;
    }

    @Override
    public URL getResource(String name) {
        if (name != null) {
            String resourceName;
            if (name.startsWith("/")) {
                resourceName = name.substring(1);
            } else {
                resourceName = name;
            }

            return bundle.getResource(resourceName);
        }
        return null;
    }

}
