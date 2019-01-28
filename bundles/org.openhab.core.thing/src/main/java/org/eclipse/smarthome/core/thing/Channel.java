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
package org.eclipse.smarthome.core.thing;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.type.AutoUpdatePolicy;
import org.eclipse.smarthome.core.thing.type.ChannelKind;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;

/**
 * {@link Channel} is a part of a {@link Thing} that represents a functionality
 * of it. Therefore {@link Item}s can be linked a to a channel. The channel only
 * accepts a specific item type which is specified by {@link Channel#getAcceptedItemType()} methods.
 *
 * @author Dennis Nobel - Initial contribution and API
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

    @NonNullByDefault({}) // uid might not have been initialized by the default constructor.
    private ChannelUID uid;

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
        this.properties = Collections.unmodifiableMap(new HashMap<String, String>(0));
    }

    /**
     * @deprecated - use {@link ChannelBuilder} instead
     */
    @Deprecated
    public Channel(ChannelUID uid, String acceptedItemType) {
        this.uid = uid;
        this.acceptedItemType = acceptedItemType;
        this.kind = ChannelKind.STATE;
        this.configuration = new Configuration();
        this.properties = Collections.unmodifiableMap(new HashMap<String, String>(0));
    }

    /**
     * @deprecated - use {@link ChannelBuilder} instead
     */
    @Deprecated
    public Channel(ChannelUID uid, String acceptedItemType, Configuration configuration) {
        this(uid, null, acceptedItemType, ChannelKind.STATE, configuration, new HashSet<String>(0), null, null, null,
                null);
    }

    /**
     * @deprecated - use {@link ChannelBuilder} instead
     */
    @Deprecated
    public Channel(ChannelUID uid, String acceptedItemType, Set<String> defaultTags) {
        this(uid, null, acceptedItemType, ChannelKind.STATE, null, defaultTags, null, null, null, null);
    }

    /**
     * @deprecated - use {@link ChannelBuilder} instead
     */
    @Deprecated
    public Channel(ChannelUID uid, String acceptedItemType, Configuration configuration, Set<String> defaultTags,
            Map<String, String> properties) {
        this(uid, null, acceptedItemType, ChannelKind.STATE, null, defaultTags, properties, null, null, null);
    }

    /**
     * @deprecated - use ChannelBuilder instead
     */
    @Deprecated
    public Channel(ChannelUID uid, @Nullable ChannelTypeUID channelTypeUID, @Nullable String acceptedItemType,
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
        this.defaultTags = Collections.<String> unmodifiableSet(new HashSet<String>(defaultTags));
        if (configuration == null) {
            this.configuration = new Configuration();
        } else {
            this.configuration = configuration;
        }
        if (properties == null) {
            this.properties = Collections.unmodifiableMap(new HashMap<String, String>(0));
        } else {
            this.properties = properties;
        }
    }

    /**
     * Returns the accepted item type.
     *
     * @return accepted item type
     */
    public @Nullable String getAcceptedItemType() {
        return this.acceptedItemType;
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
        return this.uid;
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
        return this.label;
    }

    /**
     * Returns the description (if set).
     * If no description is set, getDescription will return null and the default description for the {@link Channel} is
     * used.
     *
     * @return the description for the channel. Can be null.
     */
    public @Nullable String getDescription() {
        return this.description;
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
            return Collections.unmodifiableMap(new HashMap<>(properties));
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
