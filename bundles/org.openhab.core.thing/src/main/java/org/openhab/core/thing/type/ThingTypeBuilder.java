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

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingTypeUID;

/**
 * A {@link ThingType} builder.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public class ThingTypeBuilder {

    private @Nullable List<ChannelGroupDefinition> channelGroupDefinitions;
    private @Nullable List<ChannelDefinition> channelDefinitions;
    private @Nullable List<String> extensibleChannelTypeIds;
    private @Nullable List<String> supportedBridgeTypeUIDs;
    private @Nullable Map<String, String> properties;
    private @Nullable String representationProperty;
    private @Nullable URI configDescriptionURI;
    private boolean listed;
    private @Nullable String category;
    private @Nullable String description;

    private final String bindingId;
    private final String thingTypeId;
    private String label;

    /**
     * Create and return a {@link ThingTypeBuilder} with the given {@code bindingId}, {@code thingTypeId} and
     * {@code label}. Also, {@code listed} defaults to {@code true}.
     *
     * @param bindingId the binding Id the resulting {@link ThingType} will have. Must not be null or empty.
     * @param thingTypeId the thingTypeId the resulting {@link ThingType} will have (builds a {@link ThingTypeUID} with
     *            {@code bindingId:thingTypeId}. Must not be null or empty.
     * @param label the label of the resulting {@link ThingType}. Must not be null or empty.
     * @return the new {@link ThingTypeBuilder}.
     */
    public static ThingTypeBuilder instance(String bindingId, String thingTypeId, String label) {
        return new ThingTypeBuilder(bindingId, thingTypeId, label);
    }

    /**
     * Create and return a {@link ThingTypeBuilder} with the given {@link ThingTypeUID} and {@code label}. Also,
     * {@code listed} defaults to {@code true}.
     *
     * @param thingTypeUID the {@link ThingTypeUID} the resulting {@link ThingType} will have. Must not be null.
     * @param label the label of the resulting {@link ThingType}. Must not be null or empty.
     * @return the new {@link ThingTypeBuilder}.
     */
    public static ThingTypeBuilder instance(ThingTypeUID thingTypeUID, String label) {
        return new ThingTypeBuilder(thingTypeUID.getBindingId(), thingTypeUID.getId(), label);
    }

    /**
     * Create this builder with all properties from the given {@link ThingType}.
     *
     * @param thingType take all properties from this {@link ThingType}.
     * @return a new {@link ThingTypeBuilder} configured with all properties from the given {@link ThingType};
     * @return the new {@link ThingTypeBuilder}.
     */
    public static ThingTypeBuilder instance(ThingType thingType) {
        return new ThingTypeBuilder(thingType);
    }

    private ThingTypeBuilder(String bindingId, String thingTypeId, String label) {
        this.bindingId = bindingId;
        this.thingTypeId = thingTypeId;
        this.label = label;
        this.listed = true;
    }

    private ThingTypeBuilder(ThingType thingType) {
        this(thingType.getBindingId(), thingType.getUID().getId(), thingType.getLabel());
        description = thingType.getDescription();
        channelGroupDefinitions = thingType.getChannelGroupDefinitions();
        channelDefinitions = thingType.getChannelDefinitions();
        extensibleChannelTypeIds = thingType.getExtensibleChannelTypeIds();
        supportedBridgeTypeUIDs = thingType.getSupportedBridgeTypeUIDs();
        properties = thingType.getProperties();
        representationProperty = thingType.getRepresentationProperty();
        configDescriptionURI = thingType.getConfigDescriptionURI();
        listed = thingType.isListed();
        category = thingType.getCategory();
    }

    /**
     * Builds and returns a new {@link ThingType} according to the given values from this builder.
     *
     * @return a new {@link ThingType} according to the given values from this builder.
     * @throws IllegalStateException if one of {@code bindingId}, {@code thingTypeId} or {@code label} are not given.
     */
    public ThingType build() {
        if (StringUtils.isBlank(bindingId)) {
            throw new IllegalArgumentException("The bindingId must neither be null nor empty.");
        }
        if (StringUtils.isBlank(thingTypeId)) {
            throw new IllegalArgumentException("The thingTypeId must neither be null nor empty.");
        }
        if (StringUtils.isBlank(label)) {
            throw new IllegalArgumentException("The label must neither be null nor empty.");
        }

        return new ThingType(new ThingTypeUID(bindingId, thingTypeId), supportedBridgeTypeUIDs, label, description,
                category, listed, representationProperty, channelDefinitions, channelGroupDefinitions, properties,
                configDescriptionURI, extensibleChannelTypeIds);
    }

    /**
     * Builds and returns a new {@link BridgeType} according to the given values from this builder.
     *
     * @return a new {@link BridgeType} according to the given values from this builder.
     * @throws IllegalStateException if one of {@code bindingId}, {@code thingTypeId} or {@code label} are not given.
     */
    public BridgeType buildBridge() {
        if (StringUtils.isBlank(bindingId)) {
            throw new IllegalArgumentException("The bindingId must neither be null nor empty.");
        }
        if (StringUtils.isBlank(thingTypeId)) {
            throw new IllegalArgumentException("The thingTypeId must neither be null nor empty.");
        }
        if (StringUtils.isBlank(label)) {
            throw new IllegalArgumentException("The label must neither be null nor empty.");
        }

        return new BridgeType(new ThingTypeUID(bindingId, thingTypeId), supportedBridgeTypeUIDs, label, description,
                category, listed, representationProperty, channelDefinitions, channelGroupDefinitions, properties,
                configDescriptionURI, extensibleChannelTypeIds);
    }

    public ThingTypeBuilder withLabel(String label) {
        this.label = label;
        return this;
    }

    public ThingTypeBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public ThingTypeBuilder withCategory(String category) {
        this.category = category;
        return this;
    }

    public ThingTypeBuilder isListed(boolean listed) {
        this.listed = listed;
        return this;
    }

    public ThingTypeBuilder withRepresentationProperty(String representationProperty) {
        this.representationProperty = representationProperty;
        return this;
    }

    public ThingTypeBuilder withChannelDefinitions(List<ChannelDefinition> channelDefinitions) {
        this.channelDefinitions = channelDefinitions;
        return this;
    }

    public ThingTypeBuilder withChannelGroupDefinitions(List<ChannelGroupDefinition> channelGroupDefinitions) {
        this.channelGroupDefinitions = channelGroupDefinitions;
        return this;
    }

    public ThingTypeBuilder withProperties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }

    public ThingTypeBuilder withConfigDescriptionURI(URI configDescriptionURI) {
        this.configDescriptionURI = configDescriptionURI;
        return this;
    }

    public ThingTypeBuilder withExtensibleChannelTypeIds(List<String> extensibleChannelTypeIds) {
        this.extensibleChannelTypeIds = extensibleChannelTypeIds;
        return this;
    }

    public ThingTypeBuilder withSupportedBridgeTypeUIDs(List<String> supportedBridgeTypeUIDs) {
        this.supportedBridgeTypeUIDs = supportedBridgeTypeUIDs;
        return this;
    }

}
