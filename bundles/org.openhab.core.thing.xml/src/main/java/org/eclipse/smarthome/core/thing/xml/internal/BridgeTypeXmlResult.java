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
package org.eclipse.smarthome.core.thing.xml.internal;

import java.util.List;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionProvider;
import org.eclipse.smarthome.config.xml.util.NodeValue;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.type.BridgeType;

import com.thoughtworks.xstream.converters.ConversionException;

/**
 * The {@link BridgeTypeXmlResult} is an intermediate XML conversion result object which
 * contains all fields needed to create a concrete {@link BridgeType} object.
 * <p>
 * If a {@link ConfigDescription} object exists, it must be added to the according {@link ConfigDescriptionProvider}.
 *
 * @author Michael Grammling - Initial Contribution
 * @author Thomas Höfer - Added thing and thing type properties
 * @author Andre Fuechsel - Added representationProperty
 */
public class BridgeTypeXmlResult extends ThingTypeXmlResult {

    public BridgeTypeXmlResult(ThingTypeUID bridgeTypeUID, List<String> supportedBridgeTypeUIDs, String label,
            String description, String category, boolean listed, List<String> extensibleChannelTypeIds,
            List<ChannelXmlResult>[] channelTypeReferenceObjects, List<NodeValue> properties,
            String representationProperty, Object[] configDescriptionObjects) {
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
