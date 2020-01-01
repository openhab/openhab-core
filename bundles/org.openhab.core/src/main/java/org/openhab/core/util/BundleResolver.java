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
package org.openhab.core.util;

import org.osgi.framework.Bundle;

/**
 * Resolve bundle specific information from the framework.
 *
 * @author Henning Treu - Initial contribution
 */
public interface BundleResolver {

    /**
     * Resolve the bundle associated with the given {@link Class}.
     *
     * @param clazz the {@link Class} to resolve the bundle for.
     * @return the bundle associated with the given {@link Class}.
     */
    Bundle resolveBundle(Class<?> clazz);
}
