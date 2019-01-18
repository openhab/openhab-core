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
package org.eclipse.smarthome.core.thing.profiles;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.type.ChannelType;

/**
 * Implementors can give advice which {@link Profile}s can/should be used for a given link.
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
@NonNullByDefault
public interface ProfileAdvisor {

    /**
     * Suggest a custom profile for the given channel (and potentially also the itemType).
     *
     * Please note:
     * <ul>
     * <li>This will override any default behavior
     * <li>A "profile" configuration on the link will override this suggestion
     * </ul>
     *
     * @param channel the linked channel
     * @param itemType the linked itemType (not applicable for trigger channels)
     * @return the profile identifier or {@code null} if this advisor does not have any advice
     */
    @Nullable
    ProfileTypeUID getSuggestedProfileTypeUID(Channel channel, @Nullable String itemType);

    /**
     * Suggest a custom profile for a given {@link ChannelType} (and potentially also the itemType).
     *
     * Please note:
     * <ul>
     * <li>This will override any default behavior
     * <li>A "profile" configuration on the link will override this suggestion
     * </ul>
     *
     * @param channelType the {@link ChannelType} of the linked channel
     * @param itemType the linked itemType (not applicable for trigger channels)
     * @return the profile identifier or {@code null} if this advisor does not have any advice
     */
    @Nullable
    ProfileTypeUID getSuggestedProfileTypeUID(ChannelType channelType, @Nullable String itemType);

}
