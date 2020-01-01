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
package org.openhab.core.service;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.StateDescription;

/**
 * Implementations of this service provide strategies for merging info from
 * different StateDescriptionProviders into one StateDescription.
 * 
 * @author Lyubomir Papazov - Initial contribution
 */
@NonNullByDefault
public interface StateDescriptionService {

    /**
     * Implementations of this method merge the StateDescriptions provided from
     * multiple StateDescriptionProviders into one final StateDescription.
     * 
     * @param itemName the item for which to get the StateDescription (must not be null)
     * @param locale locale (can be null)
     * @return state description or null if no state description could be found
     */
    @Nullable
    StateDescription getStateDescription(String itemName, @Nullable Locale locale);
}
