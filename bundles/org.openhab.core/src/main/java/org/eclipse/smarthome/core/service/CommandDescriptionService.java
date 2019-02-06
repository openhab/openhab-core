/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.service;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.types.CommandDescription;

/**
 * An implementation of this service provides locale specific {@link CommandDescription}s for the given item.
 *
 * @author Henning Treu - Initial contribution
 *
 */
@NonNullByDefault
public interface CommandDescriptionService {

    /**
     * Returns the locale specific {@link CommandDescription} for the given item name.
     *
     * @param itemName the name of the item
     * @param locale the locale for translated command labels
     * @return the locale specific {@link CommandDescription} for the given item name
     */
    @Nullable
    CommandDescription getCommandDescription(String itemName, @Nullable Locale locale);
}
