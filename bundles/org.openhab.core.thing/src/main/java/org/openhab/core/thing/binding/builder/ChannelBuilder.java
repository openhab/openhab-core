/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.thing.binding.builder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.type.AutoUpdatePolicy;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * {@link ChannelBuilder} is responsible for creating {@link Channel}s.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Alex Tugarev - Extended about default tags
 * @author Chris Jackson - Added properties and label/description
 */
@NonNullByDefault
public class ChannelBuilder {

    private class ChannelImpl extends Channel {
        ChannelImpl(ChannelUID uid, @Nullable ChannelTypeUID channelTypeUID, @Nullable String acceptedItemType,
                ChannelKind kind, @Nullable Configuration configuration, Set<String> defaultTags,
                @Nullable Map<String, String> properties, @Nullable String label, @Nullable String description,
                @Nullable AutoUpdatePolicy autoUpdatePolicy) {
            super(channelUID, channelTypeUID, acceptedItemType, kind, configuration, defaultTags, properties, label,
                    description, autoUpdatePolicy);
        }
    }

    private final ChannelUID channelUID;
    private @Nullable String acceptedItemType;
    private ChannelKind kind;
    private @Nullable Configuration configuration;
    private Set<String> defaultTags;
    private @Nullable Map<String, String> properties;
    private @Nullable String label;
    private @Nullable String description;
    private @Nullable ChannelTypeUID channelTypeUID;
    private @Nullable AutoUpdatePolicy autoUpdatePolicy;

    private ChannelBuilder(ChannelUID channelUID, @Nullable String acceptedItemType, Set<String> defaultTags) {
        this.channelUID = channelUID;
        this.acceptedItemType = acceptedItemType;
        this.defaultTags = defaultTags;
        this.kind = ChannelKind.STATE;
    }

    /**
     * Creates a {@link ChannelBuilder} for the given {@link ChannelUID}.
     *
     * @param channelUID the {@link ChannelUID}
     * @return channel builder
     */
    public static ChannelBuilder create(ChannelUID channelUID) {
        return new ChannelBuilder(channelUID, null, new HashSet<>());
    }

    /**
     * Creates a {@link ChannelBuilder} for the given {@link ChannelUID} and item type.
     *
     * @param channelUID the {@link ChannelUID}
     * @param acceptedItemType item type that is accepted by this channel
     * @return channel builder
     */
    public static ChannelBuilder create(ChannelUID channelUID, @Nullable String acceptedItemType) {
        return new ChannelBuilder(channelUID, acceptedItemType, new HashSet<>());
    }

    /**
     * Creates a {@link ChannelBuilder} from the given {@link Channel}.
     *
     * @param channel the channel to be changed
     * @return channel builder
     */
    public static ChannelBuilder create(Channel channel) {
        ChannelBuilder channelBuilder = create(channel.getUID(), channel.getAcceptedItemType())
                .withConfiguration(channel.getConfiguration()).withDefaultTags(channel.getDefaultTags())
                .withKind(channel.getKind()).withProperties(channel.getProperties())
                .withType(channel.getChannelTypeUID());
        String label = channel.getLabel();
        if (label != null) {
            channelBuilder.withLabel(label);
        }
        String description = channel.getDescription();
        if (description != null) {
            channelBuilder.withDescription(description);
        }
        return channelBuilder;
    }

    /**
     * Appends the {@link ChannelType} given by its {@link ChannelTypeUID} to the {@link Channel} to be build
     *
     * @param channelTypeUID the {@link ChannelTypeUID}
     * @return channel builder
     */
    public ChannelBuilder withType(@Nullable ChannelTypeUID channelTypeUID) {
        this.channelTypeUID = channelTypeUID;
        return this;
    }

    /**
     * Appends a {@link Configuration} to the {@link Channel} to be build.
     *
     * @param configuration the {@link Configuration}
     * @return channel builder
     */
    public ChannelBuilder withConfiguration(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    /**
     * Adds properties to the {@link Channel}.
     *
     * @param properties properties to add
     * @return channel builder
     */
    public ChannelBuilder withProperties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }

    /**
     * Sets the channel label. This allows overriding of the default label set in the {@link ChannelType}.
     *
     * @param label the channel label to override the label set in the {@link ChannelType}
     * @return channel builder
     */
    public ChannelBuilder withLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * Sets the channel description. This allows overriding of the default description set in the {@link ChannelType}.
     *
     * @param label the channel label to override the description set in the {@link ChannelType}
     * @return channel builder
     */
    public ChannelBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Appends default tags to the {@link Channel} to be build.
     *
     * @param defaultTags default tags
     * @return channel builder
     */
    public ChannelBuilder withDefaultTags(Set<String> defaultTags) {
        this.defaultTags = defaultTags;
        return this;
    }

    /**
     * Sets the {@link ChannelKind} of the {@link Channel} to be build.
     *
     * @param kind the {@link ChannelKind}
     * @return channel builder
     */
    public ChannelBuilder withKind(ChannelKind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("'ChannelKind' must not be null");
        }
        this.kind = kind;
        return this;
    }

    /**
     * Sets the accepted item type of the {@link Channel} to be build. See
     * {@link CoreItemFactory#getSupportedItemTypes()} for a list of available item types.
     *
     * @param acceptedItemType item type that is accepted by this channel
     * @return channel builder
     */
    public ChannelBuilder withAcceptedItemType(@Nullable String acceptedItemType) {
        this.acceptedItemType = acceptedItemType;
        return this;
    }

    /**
     * Sets the {@link AutoUpdatePolicy} to the {@link Channel} to be build.
     *
     * @param policy the {@link AutoUpdatePolicy} to be used
     * @return channel builder
     */
    public ChannelBuilder withAutoUpdatePolicy(@Nullable AutoUpdatePolicy policy) {
        this.autoUpdatePolicy = policy;
        return this;
    }

    /**
     * Builds and returns the {@link Channel}.
     *
     * @return the {@link Channel}
     */
    public Channel build() {
        return new ChannelImpl(channelUID, channelTypeUID, acceptedItemType, kind, configuration, defaultTags,
                properties, label, description, autoUpdatePolicy);
    }
}
