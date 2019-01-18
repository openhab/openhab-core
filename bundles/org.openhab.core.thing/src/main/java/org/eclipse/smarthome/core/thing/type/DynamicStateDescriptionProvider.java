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
package org.eclipse.smarthome.core.thing.type;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.types.StateDescription;

/**
 * The {@link DynamicStateDescriptionProvider} is responsible for providing {@link StateDescription} for a channel
 * dynamically in the runtime.
 *
 * @author Pawel Pieczul - initial contribution
 *
 */
@NonNullByDefault
public interface DynamicStateDescriptionProvider {
    /**
     * For a given channel UID, return a {@link StateDescription} that should be used for the channel, instead of the
     * one defined statically in the {@link ChannelType}.
     *
     * For a particular channel, there should be only one provider of the dynamic state description. When more than one
     * description is provided for the same channel (by different providers), only one will be used, from the provider
     * that registered first.
     *
     * @param channel channel
     * @param originalStateDescription original state description retrieved from the channel type
     *            this is the description to be replaced by the provided one
     * @param locale locale (can be null)
     * @return state description or null if none provided
     */
    @Nullable
    StateDescription getStateDescription(Channel channel, @Nullable StateDescription originalStateDescription,
            @Nullable Locale locale);
}
