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
package org.openhab.core.internal.service;

import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;

/**
 * Default implementation of {@link BundleResolver}. Use the {@link FrameworkUtil} to resolve bundles.
 *
 * @author Henning Treu - Initial contribution
 */
@Component(service = BundleResolver.class)
public class BundleResolverImpl implements BundleResolver {

    @Override
    public Bundle resolveBundle(Class<?> clazz) {
        return FrameworkUtil.getBundle(clazz);
    }

}
