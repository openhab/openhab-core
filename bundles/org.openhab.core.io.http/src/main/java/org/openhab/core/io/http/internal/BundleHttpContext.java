/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

/**
 * A bundle specific http context - delegates security and mime type handling to "parent" context.
 *
 * @author Łukasz Dywicki - Initial contribution
 */
@NonNullByDefault
class BundleHttpContext extends DelegatingHttpContext {

    private final @Nullable Bundle bundle;

    BundleHttpContext(HttpContext delegate, @Nullable Bundle bundle) {
        super(delegate);
        this.bundle = bundle;
    }

    @Override
    public @Nullable URL getResource(@Nullable String name) {
        if ((name != null) && (bundle != null)) {
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
