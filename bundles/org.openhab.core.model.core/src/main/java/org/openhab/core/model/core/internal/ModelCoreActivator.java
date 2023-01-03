/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.model.core.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class ModelCoreActivator implements BundleActivator {

    private static @Nullable BundleContext context;

    static @Nullable BundleContext getContext() {
        return context;
    }

    @Override
    public void start(@Nullable BundleContext bundleContext) throws Exception {
        ModelCoreActivator.context = bundleContext;
    }

    @Override
    public void stop(@Nullable BundleContext bundleContext) throws Exception {
        ModelCoreActivator.context = null;
    }
}
