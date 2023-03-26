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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.config.core.xml.util.NodeValue;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.BridgeType;

import com.thoughtworks.xstream.converters.ConversionException;

/**
 * The {@link BridgeTypeXmlResult} is an intermediate XML conversion result object which
 * contains all fields needed to create a concrete {@link BridgeType} object.
 * <p>
 * If a {@link ConfigDescription} object exists, it must be added to the according {@link ConfigDescriptionProvider}.
 *
 * @author Michael Grammling - Initial contribution
 * @author Thomas Höfer - Added thing and thing type properties
 * @author Andre Fuechsel - Added representationProperty
 */
@NonNullByDefault
public class BridgeTypeXmlResult extends ThingTypeXmlResult {

    public BridgeTypeXmlResult(ThingTypeUID bridgeTypeUID, @Nullable List<String> supportedBridgeTypeUIDs, String label,
            @Nullable String description, @Nullable String category, boolean listed,
            @Nullable List<String> extensibleChannelTypeIds,
            @Nullable List<ChannelXmlResult>[] channelTypeReferenceObjects, @Nullable List<NodeValue> properties,
            @Nullable String representationProperty, Object[] configDescriptionObjects) {
        super(bridgeTypeUID, supportedBridgeTypeUIDs, label, description, category, listed, extensibleChannelTypeIds,
                channelTypeReferenceObjects, properties, representationProperty, configDescriptionObjects);
    }

    @Override
    public BridgeType toThingType() throws ConversionException {
        return getBuilder().buildBridge();
    }

    @Override
    public String toString() {
        return "BridgeTypeXmlResult [thingTypeUID=" + thingTypeUID + ", supportedBridgeTypeUIDs="
                + supportedBridgeTypeUIDs + ", label=" + label + ", description=" + description + ", category="
                + category + ", listed=" + listed + ", representationProperty=" + representationProperty
                + ", channelTypeReferences=" + channelTypeReferences + ", channelGroupTypeReferences="
                + channelGroupTypeReferences + ", extensibelChannelTypeIds=" + extensibleChannelTypeIds
                + ", properties=" + properties + ", configDescriptionURI=" + configDescriptionURI
                + ", configDescription=" + configDescription + "]";
    }
}
