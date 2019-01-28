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
package org.eclipse.smarthome.core.thing.type;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link BridgeType} describes a concrete type of a {@link Bridge}.
 * A {@link BridgeType} inherits a {@link ThingType} and signals a parent-child relation.
 * <p>
 * This description is used as template definition for the creation of the according concrete {@link Bridge} object.
 * <p>
 * <b>Hint:</b> This class is immutable.
 *
 * @author Michael Grammling - Initial Contribution
 * @author Thomas Höfer - Added thing and thing type properties
 * @author Andre Fuechsel - Added representationProperty
 */
@NonNullByDefault
public class BridgeType extends ThingType {

    /**
     * @deprecated Use {@link ThingTypeBuilder}.buildBridge() instead.
     *
     */
    @Deprecated
    public BridgeType(String bindingId, String thingTypeId, String label) throws IllegalArgumentException {
        this(new ThingTypeUID(bindingId, thingTypeId), null, label, null, null, true, null, null, null, null, null);
    }

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @deprecated Use {@link ThingTypeBuilder}.buildBridge() instead.
     *
     * @throws IllegalArgumentException if the UID is null or empty,
     *             or the the meta information is null
     */
    @Deprecated
    public BridgeType(ThingTypeUID uid, List<String> supportedBridgeTypeUIDs, String label, String description,
            List<ChannelDefinition> channelDefinitions, List<ChannelGroupDefinition> channelGroupDefinitions,
            Map<String, String> properties, URI configDescriptionURI) throws IllegalArgumentException {
        this(uid, supportedBridgeTypeUIDs, label, description, null, true, null, channelDefinitions,
                channelGroupDefinitions, properties, configDescriptionURI);
    }

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @deprecated Use {@link ThingTypeBuilder}.buildBridge() instead.
     *
     * @throws IllegalArgumentException if the UID is null or empty,
     *             or the the meta information is null
     */
    @Deprecated
    public BridgeType(ThingTypeUID uid, List<String> supportedBridgeTypeUIDs, String label, String description,
            String category, boolean listed, List<ChannelDefinition> channelDefinitions,
            List<ChannelGroupDefinition> channelGroupDefinitions, Map<String, String> properties,
            URI configDescriptionURI) throws IllegalArgumentException {
        this(uid, supportedBridgeTypeUIDs, label, description, category, listed, null, channelDefinitions,
                channelGroupDefinitions, properties, configDescriptionURI);
    }

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @deprecated Use {@link ThingTypeBuilder}.buildBridge() instead.
     *
     * @throws IllegalArgumentException if the UID is null or empty,
     *             or the the meta information is null
     */
    @Deprecated
    public BridgeType(ThingTypeUID uid, @Nullable List<String> supportedBridgeTypeUIDs, String label,
            @Nullable String description, @Nullable String category, boolean listed,
            @Nullable String representationProperty, @Nullable List<ChannelDefinition> channelDefinitions,
            @Nullable List<ChannelGroupDefinition> channelGroupDefinitions, @Nullable Map<String, String> properties,
            @Nullable URI configDescriptionURI) throws IllegalArgumentException {
        super(uid, supportedBridgeTypeUIDs, label, description, category, listed, representationProperty,
                channelDefinitions, channelGroupDefinitions, properties, configDescriptionURI);
    }

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
