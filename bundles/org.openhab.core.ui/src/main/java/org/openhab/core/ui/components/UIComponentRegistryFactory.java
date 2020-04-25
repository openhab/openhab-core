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
package org.openhab.core.ui.components;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A factory for {@link UIComponentRegistry} instances based on the namespace.
 *
 * @author Yannick Schaus - Initial contribution
 */
@NonNullByDefault
public interface UIComponentRegistryFactory {

    /**
     * Gets the {@link UIComponentRegistry} for the specified namespace.
     *
     * @param namespace the namespace
     * @return a registry for UI elements in the namespace
     */
    UIComponentRegistry getRegistry(String namespace);
}
