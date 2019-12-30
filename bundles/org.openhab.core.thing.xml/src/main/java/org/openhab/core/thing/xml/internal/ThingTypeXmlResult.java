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
package org.openhab.core.thing.xml.internal;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.config.xml.util.NodeValue;
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
public class ThingTypeXmlResult {

    protected ThingTypeUID thingTypeUID;
    protected List<String> supportedBridgeTypeUIDs;
    protected String label;
    protected String description;
    protected String category;
    protected boolean listed;
    protected List<String> extensibleChannelTypeIds;
    protected String representationProperty;
    protected List<ChannelXmlResult> channelTypeReferences;
    protected List<ChannelXmlResult> channelGroupTypeReferences;
    protected List<NodeValue> properties;
    protected URI configDescriptionURI;
    protected ConfigDescription configDescription;

    public ThingTypeXmlResult(ThingTypeUID thingTypeUID, List<String> supportedBridgeTypeUIDs, String label,
            String description, String category, boolean listed, List<String> extensibleChannelTypeIds,
            List<ChannelXmlResult>[] channelTypeReferenceObjects, List<NodeValue> properties,
            String representationProperty, Object[] configDescriptionObjects) {
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

    protected List<ChannelDefinition> toChannelDefinitions(List<ChannelXmlResult> channelTypeReferences)
            throws ConversionException {
        List<ChannelDefinition> channelTypeDefinitions = null;

        if (channelTypeReferences != null && !channelTypeReferences.isEmpty()) {
            channelTypeDefinitions = new ArrayList<>(channelTypeReferences.size());

            for (ChannelXmlResult channelTypeReference : channelTypeReferences) {
                channelTypeDefinitions.add(channelTypeReference.toChannelDefinition(this.thingTypeUID.getBindingId()));
            }
        }

        return channelTypeDefinitions;
    }

    protected List<ChannelGroupDefinition> toChannelGroupDefinitions(List<ChannelXmlResult> channelGroupTypeReferences)
            throws ConversionException {
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

    protected Map<String, String> toPropertiesMap() {
        if (properties == null) {
            return null;
        }

        Map<String, String> propertiesMap = new HashMap<>();
        for (NodeValue property : properties) {
            propertiesMap.put(property.getAttributes().get("name"), (String) property.getValue());
        }
        return propertiesMap;
    }

    ThingTypeBuilder getBuilder() {
        return ThingTypeBuilder.instance(thingTypeUID, label) //
                .withSupportedBridgeTypeUIDs(supportedBridgeTypeUIDs) //
                .withDescription(description) //
                .withCategory(category) //
                .isListed(listed) //
                .withRepresentationProperty(representationProperty) //
                .withChannelDefinitions(toChannelDefinitions(channelTypeReferences)) //
                .withChannelGroupDefinitions(toChannelGroupDefinitions(channelGroupTypeReferences)) //
                .withProperties(toPropertiesMap()) //
                .withConfigDescriptionURI(configDescriptionURI) //
                .withExtensibleChannelTypeIds(extensibleChannelTypeIds); //
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
