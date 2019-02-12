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
import org.eclipse.smarthome.core.types.CommandDescription;

/**
 * Implementations may provide channel specific {@link CommandDescription}s.
 *
 * @author Henning Treu - Initial contribution
 *
 */
@NonNullByDefault
public interface DynamicCommandDescriptionProvider {

    /**
     * For a given channel UID, return a {@link CommandDescription} that should be used for the channel, instead of the
     * one defined statically in the {@link ChannelType}.
     *
     * For a particular channel, there should be only one provider of the dynamic command description. When more than
     * one description is provided for the same channel (by different providers), only one will be used, from the
     * provider that registered first.
     *
     * @param channel channel
     * @param originalCommandDescription original command description retrieved from the channel type
     *            this is the description to be replaced by the provided one
     * @param locale locale (can be null)
     * @return command description or null if none provided
     */
    @Nullable
    CommandDescription getCommandDescription(Channel channel, @Nullable CommandDescription originalCommandDescription,
            @Nullable Locale locale);
}
