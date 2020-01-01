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
package org.openhab.core.types;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Implementations provide an item specific, localized {@link CommandDescription}.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public interface CommandDescriptionProvider {

    /**
     * Returns the item specific, localized {@link CommandDescription}.
     *
     * @param itemName the name of the item
     * @param locale the locale
     * @return the item specific, localized {@link CommandDescription}
     */
    @Nullable
    CommandDescription getCommandDescription(String itemName, @Nullable Locale locale);
}
