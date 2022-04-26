/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.ui.internal.components;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.ui.components.RootUIComponent;

/**
 * Provides components (pages, widgets, etc.) at runtime.
 *
 * @author Łukasz Dywicki - Initial contribution
 */
@NonNullByDefault
public interface UIProvider extends Provider<RootUIComponent> {
    String getNamespace();
}
