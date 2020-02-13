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
import org.openhab.core.common.registry.Registry;

/**
 * A namespace-specific {@link Registry} for UI components.
 * It is normally instantiated for a specific namespace by the {@link UIComponentRegistryFactory}.
 *
 * @author Yannick Schaus - Initial contribution
 */
@NonNullByDefault
public interface UIComponentRegistry extends Registry<RootUIComponent, String> {

}
