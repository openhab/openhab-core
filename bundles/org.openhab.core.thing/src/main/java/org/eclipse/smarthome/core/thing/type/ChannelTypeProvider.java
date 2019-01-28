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

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link ChannelTypeProvider} is responsible for providing channel types.
 *
 * @see ChannelTypeRegistry
 *
 * @author Dennis Nobel - Initial contribution
 */
@NonNullByDefault
public interface ChannelTypeProvider {

    /**
     * @see ChannelTypeRegistry#getChannelTypes(Locale)
     */
    @Nullable
    Collection<ChannelType> getChannelTypes(@Nullable Locale locale);

    /**
     * @see ChannelTypeRegistry#getChannelType(ChannelTypeUID, Locale)
     */
    @Nullable
    ChannelType getChannelType(ChannelTypeUID channelTypeUID, @Nullable Locale locale);

    /**
     * @deprecated The {@link ChannelGroupTypeProvider} is now to be implemented/used instead.
     */
    @Deprecated
    @Nullable
    default ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID, @Nullable Locale locale) {
        return null;
    }

    /**
     * @deprecated The {@link ChannelGroupTypeProvider} is now to be implemented/used instead.
     */
    @Deprecated
    @Nullable
    default Collection<ChannelGroupType> getChannelGroupTypes(@Nullable Locale locale) {
        return Collections.emptyList();
    }
}
