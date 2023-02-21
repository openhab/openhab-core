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
package org.openhab.core.thing.xml.internal;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.config.core.xml.util.NodeValue;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelGroupDefinition;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;

import com.thoughtworks.xstream.converters.ConversionException;

/**
 * The {@link ThingTypeXmlResult} is an intermediate XML conversion result object which
 * contains all fields needed to create a concrete {@link ThingType} object.
 * <p>
 * If a {@link ConfigDescription} object exists, it must be added to the according {@link ConfigDescriptionProvider}.
 *
 * @author Michael Grammling - Initial contribution
 * @author Ivan Iliev - Added support for system wide channel types
 * @author Thomas Höfer - Added thing and thing type properties
 * @author Chris Jackson - Added channel properties
 * @author Simon Kaufmann - Added listed field
 * @author Andre Fuechsel - Added representationProperty field
 * @author Stefan Triller - Added category field
 */
@NonNullByDefault
public class ThingTypeXmlResult {

    protected ThingTypeUID thingTypeUID;
    protected @Nullable List<String> supportedBridgeTypeUIDs;
    protected String label;
    protected @Nullable String description;
    protected @Nullable String category;
    protected boolean listed;
    protected @Nullable List<String> extensibleChannelTypeIds;
    protected @Nullable String representationProperty;
    protected @Nullable List<ChannelXmlResult> channelTypeReferences;
    protected @Nullable List<ChannelXmlResult> channelGroupTypeReferences;
    protected @Nullable List<NodeValue> properties;
    protected URI configDescriptionURI;
    protected ConfigDescription configDescription;

    public ThingTypeXmlResult(ThingTypeUID thingTypeUID, @Nullable List<String> supportedBridgeTypeUIDs, String label,
            @Nullable String description, @Nullable String category, boolean listed,
            @Nullable List<String> extensibleChannelTypeIds,
            @Nullable List<ChannelXmlResult>[] channelTypeReferenceObjects, @Nullable List<NodeValue> properties,
            @Nullable String representationProperty, Object[] configDescriptionObjects) {
        this.thingTypeUID = thingTypeUID;
        this.supportedBridgeTypeUIDs = supportedBridgeTypeUIDs;
        this.label = label;
        this.description = description;
        this.category = category;
        this.listed = listed;
        this.extensibleChannelTypeIds = extensibleChannelTypeIds;
        this.representationProperty = representationProperty;
        this.channelTypeReferences = channelTypeReferenceObjects[0];
        this.channelGroupTypeReferences = channelTypeReferenceObjects[1];
        this.properties = properties;
        this.configDescriptionURI = (URI) configDescriptionObjects[0];
        this.configDescription = (ConfigDescription) configDescriptionObjects[1];
    }

    public ThingTypeUID getUID() {
        return thingTypeUID;
    }

    public ConfigDescription getConfigDescription() {
        return configDescription;
    }

    protected @Nullable List<ChannelDefinition> toChannelDefinitions(
            @Nullable List<ChannelXmlResult> channelTypeReferences) throws ConversionException {
        List<ChannelDefinition> channelTypeDefinitions = null;

        if (channelTypeReferences != null && !channelTypeReferences.isEmpty()) {
            channelTypeDefinitions = new ArrayList<>(channelTypeReferences.size());

            for (ChannelXmlResult channelTypeReference : channelTypeReferences) {
                channelTypeDefinitions.add(channelTypeReference.toChannelDefinition(this.thingTypeUID.getBindingId()));
            }
        }

        return channelTypeDefinitions;
    }

    protected @Nullable List<ChannelGroupDefinition> toChannelGroupDefinitions(
            @Nullable List<ChannelXmlResult> channelGroupTypeReferences) throws ConversionException {
        List<ChannelGroupDefinition> channelGroupTypeDefinitions = null;

        if (channelGroupTypeReferences != null && !channelGroupTypeReferences.isEmpty()) {
            channelGroupTypeDefinitions = new ArrayList<>(channelGroupTypeReferences.size());

            for (ChannelXmlResult channelGroupTypeReference : channelGroupTypeReferences) {
                String id = channelGroupTypeReference.getId();
                String typeId = channelGroupTypeReference.getTypeId();

                String typeUID = String.format("%s:%s", this.thingTypeUID.getBindingId(), typeId);

                ChannelGroupDefinition channelGroupDefinition = new ChannelGroupDefinition(id,
                        new ChannelGroupTypeUID(typeUID), channelGroupTypeReference.getLabel(),
                        channelGroupTypeReference.getDescription());

                channelGroupTypeDefinitions.add(channelGroupDefinition);
            }
        }

        return channelGroupTypeDefinitions;
    }

    protected @Nullable Map<String, String> toPropertiesMap() {
        List<NodeValue> properties = this.properties;
        if (properties == null) {
            return null;
        }

        Map<String, String> propertiesMap = new HashMap<>();
        for (NodeValue property : properties) {
            Map<String, String> attributes = property.getAttributes();
            if (attributes != null) {
                String name = attributes.get("name");
                if (name != null) {
                    String value = (String) property.getValue();
                    if (value != null) {
                        propertiesMap.put(name, value);
                    }
                }
            }
        }
        return propertiesMap;
    }

    ThingTypeBuilder getBuilder() {
        ThingTypeBuilder builder = ThingTypeBuilder.instance(thingTypeUID, label) //
                .isListed(listed) //
                .withConfigDescriptionURI(configDescriptionURI);

        List<String> supportedBridgeTypeUIDs = this.supportedBridgeTypeUIDs;
        if (supportedBridgeTypeUIDs != null) {
            builder.withSupportedBridgeTypeUIDs(supportedBridgeTypeUIDs);
        }

        String description = this.description;
        if (description != null) {
            builder.withDescription(description);
        }

        String category = this.category;
        if (category != null) {
            builder.withCategory(category);
        }

        String representationProperty = this.representationProperty;
        if (representationProperty != null) {
            builder.withRepresentationProperty(representationProperty);
        }

        List<ChannelDefinition> channelDefinitions = toChannelDefinitions(channelTypeReferences);
        if (channelDefinitions != null) {
            builder.withChannelDefinitions(channelDefinitions);
        }

        List<ChannelGroupDefinition> channelGroupDefinitions = toChannelGroupDefinitions(channelGroupTypeReferences);
        if (channelGroupDefinitions != null) {
            builder.withChannelGroupDefinitions(channelGroupDefinitions);
        }

        Map<String, String> properties = toPropertiesMap();
        if (properties != null) {
            builder.withProperties(properties);
        }

        List<String> extensibleChannelTypeIds = this.extensibleChannelTypeIds;
        if (extensibleChannelTypeIds != null) {
            builder.withExtensibleChannelTypeIds(extensibleChannelTypeIds);
        }

        return builder;
    }

    public ThingType toThingType() throws ConversionException {
        return getBuilder().build();
    }

    @Override
    public String toString() {
        return "ThingTypeXmlResult [thingTypeUID=" + thingTypeUID + ", supportedBridgeTypeUIDs="
                + supportedBridgeTypeUIDs + ", label=" + label + ", description=" + description + ",  category="
                + category + ", listed=" + listed + ", representationProperty=" + representationProperty
                + ", channelTypeReferences=" + channelTypeReferences + ", channelGroupTypeReferences="
                + channelGroupTypeReferences + ", extensibelChannelTypeIds=" + extensibleChannelTypeIds
                + ", properties=" + properties + ", configDescriptionURI=" + configDescriptionURI
                + ", configDescription=" + configDescription + "]";
    }
}
