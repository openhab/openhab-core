/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
import org.openhab.core.common.registry.Registry;

/**
 * A namespace-specific {@link Registry} for UI components.
 * It is normally instantiated for a specific namespace by the {@link UIComponentRegistryFactory}.
 *
 * @author Yannick Schaus - Initial contribution
 */
@NonNullByDefault
public interface UIComponentRegistry extends Registry<RootUIComponent, String> {

    /**
     * Checks whether the component with the given UID is managed (i.e. editable via the REST API).
     * File-based components (e.g. loaded from YAML configuration files) are not managed and therefore read-only.
     *
     * @param uid the UID of the component
     * @return {@code true} if the component exists in the managed (jsondb) provider, {@code false} otherwise
     */
    default boolean isEditable(String uid) {
        return false;
    }
}
