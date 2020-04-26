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
package org.openhab.core.thing.profiles;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.type.ChannelType;

/**
 * Implementors can give advice which {@link Profile}s can/should be used for a given link.
 *
 * @author Simon Kaufmann - Initial contribution
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
