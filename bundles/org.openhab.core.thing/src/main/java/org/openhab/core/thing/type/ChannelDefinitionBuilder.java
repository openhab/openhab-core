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

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Builder for a channel definition.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class ChannelDefinitionBuilder {

    private final String id;
    private final ChannelTypeUID channelTypeUID;
    private @Nullable Map<String, String> properties;
    private @Nullable String label;
    private @Nullable String description;
    private @Nullable AutoUpdatePolicy autoUpdatePolicy;

    /**
     * Creates a new channel definition builder.
     *
     * @param channelDefinition the channel definition the builder should be initialized by
     */
    public ChannelDefinitionBuilder(final ChannelDefinition channelDefinition) {
        this(channelDefinition.getId(), channelDefinition.getChannelTypeUID());
        this.properties = channelDefinition.getProperties();
        this.label = channelDefinition.getLabel();
        this.description = channelDefinition.getDescription();
        this.autoUpdatePolicy = channelDefinition.getAutoUpdatePolicy();
    }

    /**
     * Creates a new channel definition builder.
     *
     * @param id the identifier of the channel (must neither be null nor empty)
     * @param channelTypeUID the type UID of the channel (must not be null)
     */
    public ChannelDefinitionBuilder(final String id, final ChannelTypeUID channelTypeUID) {
        this.id = id;
        this.channelTypeUID = channelTypeUID;
    }

    /**
     * Sets the properties.
     *
     * @param properties the properties
     * @return the builder
     */
    public ChannelDefinitionBuilder withProperties(final @Nullable Map<String, String> properties) {
        this.properties = properties;
        return this;
    }

    /**
     * Sets the label.
     *
     * @param label the label
     * @return the builder
     */
    public ChannelDefinitionBuilder withLabel(final @Nullable String label) {
        this.label = label;
        return this;
    }

    /**
     * Sets the description.
     *
     * @param description the description
     * @return the builder
     */
    public ChannelDefinitionBuilder withDescription(final @Nullable String description) {
        this.description = description;
        return this;
    }

    /**
     * Sets the auto update policy.
     *
     * @param autoUpdatePolicy the auto update policy
     * @return the builder
     */
    public ChannelDefinitionBuilder withAutoUpdatePolicy(final @Nullable AutoUpdatePolicy autoUpdatePolicy) {
        this.autoUpdatePolicy = autoUpdatePolicy;
        return this;
    }

    /**
     * Build a channel definition.
     *
     * @return channel definition
     */
    public ChannelDefinition build() {
        return new ChannelDefinition(id, channelTypeUID, label, description, properties, autoUpdatePolicy);
    }
}
