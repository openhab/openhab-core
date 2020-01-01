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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link ThingType} describes a concrete type of a {@link Thing}.
 * <p>
 * This description is used as template definition for the creation of the according concrete {@link Thing} object.
 * <p>
 * <b>Hint:</b> This class is immutable.
 *
 * @author Michael Grammling - Initial contribution
 * @author Dennis Nobel - Initial contribution
 * @author Thomas Höfer - Added thing and thing type properties
 * @author Simon Kaufmann - Added listed field
 * @author Andre Fuechsel - Added representationProperty field
 * @author Stefan Triller - Added category field
 */
@NonNullByDefault
public class ThingType extends AbstractDescriptionType {

    private final List<ChannelGroupDefinition> channelGroupDefinitions;
    private final List<ChannelDefinition> channelDefinitions;
    private final List<String> extensibleChannelTypeIds;
    private final List<String> supportedBridgeTypeUIDs;
    private final Map<String, String> properties;
    private final @Nullable String representationProperty;
    private final @Nullable URI configDescriptionURI;
    private final boolean listed;
    private final @Nullable String category;

    /**
     * @deprecated Use the {@link ThingTypeBuilder} instead.
     *
     * @throws IllegalArgumentException if the UID is null or empty, or the the meta information is null
     */
    @Deprecated
    public ThingType(String bindingId, String thingTypeId, String label) throws IllegalArgumentException {
        this(new ThingTypeUID(bindingId, thingTypeId), null, label, null, null, true, null, null, null, null, null);
    }

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @deprecated Use the {@link ThingTypeBuilder} instead.
     *
     * @throws IllegalArgumentException if the UID is null or empty, or the the meta information is null
     */
    @Deprecated
    public ThingType(ThingTypeUID uid, List<String> supportedBridgeTypeUIDs, String label, String description,
            List<ChannelDefinition> channelDefinitions, List<ChannelGroupDefinition> channelGroupDefinitions,
            Map<String, String> properties, URI configDescriptionURI) throws IllegalArgumentException {
        this(uid, supportedBridgeTypeUIDs, label, description, null, true, null, channelDefinitions,
                channelGroupDefinitions, properties, configDescriptionURI);
    }

    /**
     *
     * Creates a new instance of this class with the specified parameters.
     *
     * @deprecated Use the {@link ThingTypeBuilder} instead.
     *
     * @throws IllegalArgumentException if the UID is null or empty, or the the meta information is null
     */
    @Deprecated
    public ThingType(ThingTypeUID uid, List<String> supportedBridgeTypeUIDs, String label, String description,
            String category, boolean listed, List<ChannelDefinition> channelDefinitions,
            List<ChannelGroupDefinition> channelGroupDefinitions, @Nullable Map<String, String> properties,
            URI configDescriptionURI) throws IllegalArgumentException {
        this(uid, supportedBridgeTypeUIDs, label, description, category, listed, null, channelDefinitions,
                channelGroupDefinitions, properties, configDescriptionURI);
    }

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @deprecated Use the {@link ThingTypeBuilder} instead.
     *
     * @throws IllegalArgumentException if the UID is null or empty, or the the meta information is null
     */
    @Deprecated
    public ThingType(ThingTypeUID uid, @Nullable List<String> supportedBridgeTypeUIDs, String label,
            @Nullable String description, @Nullable String category, boolean listed,
            @Nullable String representationProperty, @Nullable List<ChannelDefinition> channelDefinitions,
            @Nullable List<ChannelGroupDefinition> channelGroupDefinitions, @Nullable Map<String, String> properties,
            @Nullable URI configDescriptionURI) throws IllegalArgumentException {
        this(uid, supportedBridgeTypeUIDs, label, description, category, listed, representationProperty,
                channelDefinitions, channelGroupDefinitions, properties, configDescriptionURI, null);
    }

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param uid the unique identifier which identifies this Thing type within the overall system
     *            (must neither be null, nor empty)
     * @param supportedBridgeTypeUIDs the unique identifiers of the bridges this Thing type supports
     *            (could be null or empty)
     * @param label the human readable label for the according type
     *            (must neither be null nor empty)
     * @param description the human readable description for the according type
     *            (could be null or empty)
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
    ThingType(ThingTypeUID uid, @Nullable List<String> supportedBridgeTypeUIDs, String label,
            @Nullable String description, @Nullable String category, boolean listed,
            @Nullable String representationProperty, @Nullable List<ChannelDefinition> channelDefinitions,
            @Nullable List<ChannelGroupDefinition> channelGroupDefinitions, @Nullable Map<String, String> properties,
            @Nullable URI configDescriptionURI, @Nullable List<String> extensibleChannelTypeIds)
            throws IllegalArgumentException {
        super(uid, label, description);

        this.category = category;
        this.listed = listed;
        this.representationProperty = representationProperty;

        if (supportedBridgeTypeUIDs != null) {
            this.supportedBridgeTypeUIDs = Collections.unmodifiableList(supportedBridgeTypeUIDs);
        } else {
            this.supportedBridgeTypeUIDs = Collections.emptyList();
        }

        if (channelDefinitions != null) {
            this.channelDefinitions = Collections.unmodifiableList(channelDefinitions);
        } else {
            this.channelDefinitions = Collections.emptyList();
        }

        if (channelGroupDefinitions != null) {
            this.channelGroupDefinitions = Collections.unmodifiableList(channelGroupDefinitions);
        } else {
            this.channelGroupDefinitions = Collections.emptyList();
        }

        if (extensibleChannelTypeIds != null) {
            this.extensibleChannelTypeIds = Collections.unmodifiableList(extensibleChannelTypeIds);
        } else {
            this.extensibleChannelTypeIds = Collections.emptyList();
        }

        if (properties != null) {
            this.properties = Collections.unmodifiableMap(properties);
        } else {
            this.properties = Collections.emptyMap();
        }

        this.configDescriptionURI = configDescriptionURI;
    }

    /**
     * Returns the unique identifier which identifies this Thing type within the overall system.
     *
     * @return the unique identifier which identifies this Thing type within the overall system
     *         (not null)
     */
    @Override
    public ThingTypeUID getUID() {
        return (ThingTypeUID) super.getUID();
    }

    /**
     * Returns the binding ID this Thing type belongs to.
     *
     * @return the binding ID this Thing type belongs to (not null)
     */
    public String getBindingId() {
        return getUID().getBindingId();
    }

    /**
     * Returns the unique identifiers of the bridges this {@link ThingType} supports.
     * <p>
     * The returned list is immutable.
     *
     * @return the unique identifiers of the bridges this Thing type supports
     *         (not null, could be empty)
     */
    public List<String> getSupportedBridgeTypeUIDs() {
        return this.supportedBridgeTypeUIDs;
    }

    /**
     * Returns the channels this {@link ThingType} provides.
     * <p>
     * The returned list is immutable.
     *
     * @return the channels this Thing type provides (not null, could be empty)
     */
    public List<ChannelDefinition> getChannelDefinitions() {
        return this.channelDefinitions;
    }

    /**
     * Returns the channel groups defining the channels this {@link ThingType} provides.
     * <p>
     * The returned list is immutable.
     *
     * @return the channel groups defining the channels this Thing type provides
     *         (not null, could be empty)
     */
    public List<ChannelGroupDefinition> getChannelGroupDefinitions() {
        return this.channelGroupDefinitions;
    }

    /**
     * Returns the link to a concrete {@link ConfigDescription}.
     *
     * @return the link to a concrete ConfigDescription (could be null)
     */
    public @Nullable URI getConfigDescriptionURI() {
        return this.configDescriptionURI;
    }

    /**
     * Returns the properties for this {@link ThingType}
     *
     * @return the properties for this {@link ThingType} (not null)
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    public @Nullable String getCategory() {
        return this.category;
    }

    /**
     * Check, if things of this thing type should be listed for manually pairing or not.
     *
     * @return {@code true}, if manual pairing is allowed
     */
    public boolean isListed() {
        return listed;
    }

    /**
     * Get the name of the representation property of this thing type. May be {code null}.
     *
     * @return representation property name or {@code null}
     */
    public @Nullable String getRepresentationProperty() {
        return representationProperty;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        ThingType other = (ThingType) obj;

        return this.getUID().equals(other.getUID());
    }

    @Override
    public int hashCode() {
        return getUID().hashCode();
    }

    @Override
    public String toString() {
        return getUID().toString();
    }

    public List<String> getExtensibleChannelTypeIds() {
        return extensibleChannelTypeIds;
    }

}
