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
package org.openhab.core.thing.dto;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.type.AutoUpdatePolicy;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * This is a data transfer object that is used to serialize things in the old thing structure.
 * Before OpenHAB 3.1, ThingImpl was persisted in the JSON storage. This class represents the structure to allow
 * conversion.
 *
 * @author Simon Lamon - Initial contribution
 */
@Deprecated
@NonNullByDefault
public class LegacyChannelDTO {

    private @Nullable String acceptedItemType;

    private @Nullable ChannelKind kind;

    // uid might not have been initialized by the default constructor.
    private @NonNullByDefault({}) ChannelUID uid;

    private @Nullable ChannelTypeUID channelTypeUID;

    private @Nullable String label;

    private @Nullable String description;

    private @Nullable Configuration configuration;

    private @Nullable Map<String, String> properties;

    private Set<String> defaultTags = new LinkedHashSet<>();

    private @Nullable AutoUpdatePolicy autoUpdatePolicy;

    /**
     * Package protected default constructor to allow reflective instantiation.
     */
    public LegacyChannelDTO() {
    }

    public @Nullable String getAcceptedItemType() {
        return acceptedItemType;
    }

    public @Nullable ChannelKind getKind() {
        return kind;
    }

    public ChannelUID getUid() {
        return uid;
    }

    public @Nullable ChannelTypeUID getChannelTypeUID() {
        return channelTypeUID;
    }

    public @Nullable String getLabel() {
        return label;
    }

    public @Nullable String getDescription() {
        return description;
    }

    public @Nullable Configuration getConfiguration() {
        return configuration;
    }

    public @Nullable Map<String, String> getProperties() {
        return properties;
    }

    public Set<String> getDefaultTags() {
        return defaultTags;
    }

    public @Nullable AutoUpdatePolicy getAutoUpdatePolicy() {
        return autoUpdatePolicy;
    }
}
