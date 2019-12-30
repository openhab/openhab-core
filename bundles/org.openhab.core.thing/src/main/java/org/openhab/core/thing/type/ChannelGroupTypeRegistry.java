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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The {@link ChannelGroupTypeRegistry} tracks all {@link ChannelGroupType}s provided by
 * registered {@link ChannelGroupTypeProvider}s.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
@Component(service = ChannelGroupTypeRegistry.class)
public class ChannelGroupTypeRegistry {

    private final List<ChannelGroupTypeProvider> channelGroupTypeProviders = new CopyOnWriteArrayList<>();

    /**
     * Returns all channel group types with the default {@link Locale}.
     *
     * @return all channel group types or empty list if no channel group type exists
     */
    public List<ChannelGroupType> getChannelGroupTypes() {
        return getChannelGroupTypes(null);
    }

    /**
     * Returns all channel group types for the given {@link Locale}.
     *
     * @param locale (can be null)
     * @return all channel group types or empty list if no channel group type exists
     */
    public List<ChannelGroupType> getChannelGroupTypes(@Nullable Locale locale) {
        List<ChannelGroupType> channelGroupTypes = new ArrayList<>();
        for (ChannelGroupTypeProvider channelTypeProvider : channelGroupTypeProviders) {
            channelGroupTypes.addAll(channelTypeProvider.getChannelGroupTypes(locale));
        }
        return Collections.unmodifiableList(channelGroupTypes);
    }

    /**
     * Returns the channel group type for the given UID with the default {@link Locale}.
     *
     * @return channel group type or null if no channel group type for the given UID exists
     */
    public @Nullable ChannelGroupType getChannelGroupType(@Nullable ChannelGroupTypeUID channelGroupTypeUID) {
        return getChannelGroupType(channelGroupTypeUID, null);
    }

    /**
     * Returns the channel group type for the given UID and the given {@link Locale}.
     *
     * @param locale (can be null)
     * @return channel group type or null if no channel group type for the given UID exists
     */
    public @Nullable ChannelGroupType getChannelGroupType(@Nullable ChannelGroupTypeUID channelGroupTypeUID,
            @Nullable Locale locale) {
        if (channelGroupTypeUID == null) {
            return null;
        }
        for (ChannelGroupTypeProvider channelTypeProvider : channelGroupTypeProviders) {
            ChannelGroupType channelGroupType = channelTypeProvider.getChannelGroupType(channelGroupTypeUID, locale);
            if (channelGroupType != null) {
                return channelGroupType;
            }
        }
        return null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addChannelGroupTypeProvider(ChannelGroupTypeProvider channelGroupTypeProvider) {
        this.channelGroupTypeProviders.add(channelGroupTypeProvider);
    }

    protected void removeChannelGroupTypeProvider(ChannelGroupTypeProvider channelGroupTypeProvider) {
        this.channelGroupTypeProviders.remove(channelGroupTypeProvider);
    }

}
