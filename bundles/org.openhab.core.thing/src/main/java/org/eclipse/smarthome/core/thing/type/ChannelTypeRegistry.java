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
 * The {@link ChannelTypeRegistry} tracks all {@link ChannelType}s provided by registered {@link ChannelTypeProvider}s.
 *
 * @author Dennis Nobel - Initial contribution
 *
 */
@NonNullByDefault
@Component(service = ChannelTypeRegistry.class)
public class ChannelTypeRegistry {

    private final List<ChannelTypeProvider> channelTypeProviders = new CopyOnWriteArrayList<>();

    /**
     * Returns all channel types with the default {@link Locale}.
     *
     * @return all channel types or empty list if no channel type exists
     */
    public List<ChannelType> getChannelTypes() {
        return getChannelTypes(null);
    }

    /**
     * Returns all channel types for the given {@link Locale}.
     *
     * @param locale (can be null)
     * @return all channel types or empty list if no channel type exists
     */
    public List<ChannelType> getChannelTypes(@Nullable Locale locale) {
        List<ChannelType> channelTypes = new ArrayList<>();
        for (ChannelTypeProvider channelTypeProvider : channelTypeProviders) {
            channelTypes.addAll(channelTypeProvider.getChannelTypes(locale));
        }
        return Collections.unmodifiableList(channelTypes);
    }

    /**
     * Returns the channel type for the given UID with the default {@link Locale}.
     *
     * @return channel type or null if no channel type for the given UID exists
     */
    @Nullable
    public ChannelType getChannelType(@Nullable ChannelTypeUID channelTypeUID) {
        return getChannelType(channelTypeUID, null);
    }

    /**
     * Returns the channel type for the given UID and the given {@link Locale}.
     *
     * @param locale (can be null)
     * @return channel type or null if no channel type for the given UID exists
     */
    @Nullable
    public ChannelType getChannelType(@Nullable ChannelTypeUID channelTypeUID, @Nullable Locale locale) {
        if (channelTypeUID == null) {
            return null;
        }
        for (ChannelTypeProvider channelTypeProvider : channelTypeProviders) {
            ChannelType channelType = channelTypeProvider.getChannelType(channelTypeUID, locale);
            if (channelType != null) {
                return channelType;
            }
        }
        return null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addChannelTypeProvider(ChannelTypeProvider channelTypeProviders) {
        this.channelTypeProviders.add(channelTypeProviders);
    }

    protected void removeChannelTypeProvider(ChannelTypeProvider channelTypeProviders) {
        this.channelTypeProviders.remove(channelTypeProviders);
    }

}
