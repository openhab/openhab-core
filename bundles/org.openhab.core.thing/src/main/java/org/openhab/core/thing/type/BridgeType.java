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
package org.openhab.core.thing.type;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link BridgeType} describes a concrete type of a {@link Bridge}.
 * A {@link BridgeType} inherits a {@link ThingType} and signals a parent-child relation.
 * <p>
 * This description is used as template definition for the creation of the according concrete {@link Bridge} object.
 * <p>
 * <b>Hint:</b> This class is immutable.
 *
 * @author Michael Grammling - Initial contribution
 * @author Thomas Höfer - Added thing and thing type properties
 * @author Andre Fuechsel - Added representationProperty
 */
@NonNullByDefault
public class BridgeType extends ThingType {

    /**
     * A new instance of BridgeType.
     *
     * @see ThingType(uid, supportedBridgeTypeUIDs, label, description, category, listed, representationProperty,
     *      channelDefinitions, channelGroupDefinitions, properties, configDescriptionURI,
     *      extensibleChannelTypeIds)
     *
     * @param uid the unique identifier which identifies this Thing type within the overall system
     *            (must neither be null, nor empty)
     * @param supportedBridgeTypeUIDs the unique identifiers of the bridges this Thing type supports
     *            (could be null or empty)
     * @param label the human readable label for the according type
     *            (must neither be null nor empty)
     * @param description the human readable description for the according type
     *            (could be null or empty)
     * @param category the category of the bridge (could be null)
     * @param listed determines whether it should be listed for manually pairing or not
     * @param representationProperty name of the property that uniquely identifies this Thing
     * @param channelDefinitions the channels this Thing type provides (could be null or empty)
     * @param channelGroupDefinitions the channel groups defining the channels this Thing type
     *            provides (could be null or empty)
     * @param properties the properties this Thing type provides (could be null)
     * @param configDescriptionURI the link to the concrete ConfigDescription (could be null)
     * @param extensibleChannelTypeIds the channel-type ids this thing-type is extensible with (could be null or empty).
     * @throws IllegalArgumentException if the UID is null or empty, or the the meta information is null
     */
    BridgeType(ThingTypeUID uid, @Nullable List<String> supportedBridgeTypeUIDs, String label,
            @Nullable String description, @Nullable String category, boolean listed,
            @Nullable String representationProperty, @Nullable List<ChannelDefinition> channelDefinitions,
            @Nullable List<ChannelGroupDefinition> channelGroupDefinitions, @Nullable Map<String, String> properties,
            @Nullable URI configDescriptionURI, @Nullable List<String> extensibleChannelTypeIds)
            throws IllegalArgumentException {
        super(uid, supportedBridgeTypeUIDs, label, description, category, listed, representationProperty,
                channelDefinitions, channelGroupDefinitions, properties, configDescriptionURI,
                extensibleChannelTypeIds);
    }
}
