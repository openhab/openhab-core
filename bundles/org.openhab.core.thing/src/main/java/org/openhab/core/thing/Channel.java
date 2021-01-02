/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.thing;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.items.Item;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.AutoUpdatePolicy;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * {@link Channel} is a part of a {@link Thing} that represents a functionality of it. Therefore {@link Item}s can be
 * linked a to a channel. The channel only accepts a specific item type which is specified by
 * {@link Channel#getAcceptedItemType()} methods. Use the {@link ChannelBuilder} for building channels.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Alex Tugarev - Extended about default tags
 * @author Benedikt Niehues - fix for Bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=445137 considering default
 *         values
 * @author Chris Jackson - Added properties, label, description
 * @author Kai Kreuzer - Removed linked items from channel
 */
@NonNullByDefault
public class Channel {

    private @Nullable String acceptedItemType;

    private final ChannelKind kind;

    // uid might not have been initialized by the default constructor.
    private @NonNullByDefault({}) ChannelUID uid;

    private @Nullable ChannelTypeUID channelTypeUID;

    private @Nullable String label;

    private @Nullable String description;

    private Configuration configuration;

    private Map<String, String> properties;

    private Set<String> defaultTags = new LinkedHashSet<>();

    private @Nullable AutoUpdatePolicy autoUpdatePolicy;

    /**
     * Package protected default constructor to allow reflective instantiation.
     */
    Channel() {
        this.kind = ChannelKind.STATE;
        this.configuration = new Configuration();
        this.properties = Map.of();
    }

    /**
     * Use the {@link ChannelBuilder} for building channels.
     */
    protected Channel(ChannelUID uid, @Nullable ChannelTypeUID channelTypeUID, @Nullable String acceptedItemType,
            ChannelKind kind, @Nullable Configuration configuration, Set<String> defaultTags,
            @Nullable Map<String, String> properties, @Nullable String label, @Nullable String description,
            @Nullable AutoUpdatePolicy autoUpdatePolicy) {
        this.uid = uid;
        this.channelTypeUID = channelTypeUID;
        this.acceptedItemType = acceptedItemType;
        this.kind = kind;
        this.label = label;
        this.description = description;
        this.autoUpdatePolicy = autoUpdatePolicy;
        this.defaultTags = Set.copyOf(defaultTags);
        this.configuration = configuration == null ? new Configuration() : configuration;
        this.properties = properties == null ? Map.of() : properties;
    }

    /**
     * Returns the accepted item type.
     *
     * @return accepted item type
     */
    public @Nullable String getAcceptedItemType() {
        return acceptedItemType;
    }

    /**
     * Returns the channel kind.
     *
     * @return channel kind
     */
    public ChannelKind getKind() {
        return kind;
    }

    /**
     * Returns the unique id of the channel.
     *
     * @return unique id of the channel
     */
    public ChannelUID getUID() {
        return uid;
    }

    /**
     * Returns the channel type UID
     *
     * @return channel type UID or null if no channel type is specified
     */
    public @Nullable ChannelTypeUID getChannelTypeUID() {
        return channelTypeUID;
    }

    /**
     * Returns the label (if set).
     * If no label is set, getLabel will return null and the default label for the {@link Channel} is used.
     *
     * @return the label for the channel. Can be null.
     */
    public @Nullable String getLabel() {
        return label;
    }

    /**
     * Returns the description (if set).
     * If no description is set, getDescription will return null and the default description for the {@link Channel} is
     * used.
     *
     * @return the description for the channel. Can be null.
     */
    public @Nullable String getDescription() {
        return description;
    }

    /**
     * Returns the channel configuration
     *
     * @return channel configuration (not null)
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Returns an immutable copy of the {@link Channel} properties.
     *
     * @return an immutable copy of the {@link Channel} properties (not {@code null})
     */
    public Map<String, String> getProperties() {
        synchronized (this) {
            return Map.copyOf(properties);
        }
    }

    /**
     * Returns default tags of this channel.
     *
     * @return default tags of this channel.
     */
    public Set<String> getDefaultTags() {
        return defaultTags;
    }

    public @Nullable AutoUpdatePolicy getAutoUpdatePolicy() {
        return autoUpdatePolicy;
    }
}
