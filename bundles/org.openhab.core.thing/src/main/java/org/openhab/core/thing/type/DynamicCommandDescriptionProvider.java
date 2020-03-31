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
package org.openhab.core.thing.type;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Channel;
import org.openhab.core.types.CommandDescription;

/**
 * Implementations may provide {@link Channel} specific {@link CommandDescription}s. Therefore the provider must be
 * registered as OSGi service.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public interface DynamicCommandDescriptionProvider {
    /**
     * For a given {@link Channel}, return a {@link CommandDescription} that should be used for the channel, instead of
     * the one defined statically in the {@link ChannelType}.
     *
     * For a particular channel, there should be only one provider of the dynamic command description. When more than
     * one description is provided for the same channel (by different providers), only one will be used, from the
     * provider that registered first.
     *
     * If the given channel will not be managed by the provider null should be returned. You never must return the
     * original command description in such case.
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
