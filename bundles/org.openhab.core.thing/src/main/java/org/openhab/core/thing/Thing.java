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
package org.openhab.core.thing;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.items.Item;
import org.openhab.core.thing.binding.ThingHandler;

/**
 * A {@link Thing} is a representation of a connected part (e.g. physical device or cloud service) from the real world.
 * It contains a list of {@link Channel}s, which can be bound to {@link Item}s.
 * <p>
 * A {@link Thing} might be connected through a {@link Bridge}.
 * <p>
 *
 * @author Dennis Nobel - Initial contribution
 * @author Thomas Höfer - Added thing and thing type properties
 * @author Simon Kaufmann - Added label, location
 * @author Kai Kreuzer - Removed linked items from Thing
 * @author Yordan Zhelev - Added method for getting the enabled status
 * @author Christoph Weitkamp - Added method `getChannel(ChannelUID)`
 */
@NonNullByDefault
public interface Thing extends Identifiable<ThingUID> {

    /** the key for the vendor property */
    final String PROPERTY_VENDOR = "vendor";

    /** the key for the model ID property */
    final String PROPERTY_MODEL_ID = "modelId";

    /** the key for the serial number property */
    final String PROPERTY_SERIAL_NUMBER = "serialNumber";

    /** the key for the hardware version property */
    final String PROPERTY_HARDWARE_VERSION = "hardwareVersion";

    /** the key for the firmware version property */
    final String PROPERTY_FIRMWARE_VERSION = "firmwareVersion";

    /** the key for the MAC address property */
    final String PROPERTY_MAC_ADDRESS = "macAddress";

    /**
     * Returns the human readable label for this thing.
     *
     * @return the human readable label
     */
    @Nullable
    String getLabel();

    /**
     * Sets the human readable label for this thing.
     *
     * @param label the human readable label
     */
    void setLabel(@Nullable String label);

    /**
     * Gets the channels.
     *
     * @return the channels
     */
    List<Channel> getChannels();

    /**
     * Gets the channels of the given channel group or an empty list if no channel group with the id exists or the
     * channel group does not have channels.
     *
     * @return the channels of the given channel group
     */
    List<Channel> getChannelsOfGroup(String channelGroupId);

    /**
     * Gets the channel for the given id or null if no channel with the id exists.
     *
     * @param channelId channel ID
     * @return the channel for the given id or null if no channel with the id exists
     */
    @Nullable
    Channel getChannel(String channelId);

    /**
     * Gets the channel for the given UID or null if no channel with the UID exists.
     *
     * @param channelUID channel UID
     * @return the channel for the given UID or null if no channel with the UID exists
     */
    @Nullable
    Channel getChannel(ChannelUID channelUID);

    /**
     * Gets the status of a thing.
     *
     * In order to get all status information (status, status detail and status description) please use
     * {@link Thing#getStatusInfo()}.
     *
     * @return the status
     */
    ThingStatus getStatus();

    /**
     * Gets the status info of a thing.
     *
     * The status info consists of the status itself, the status detail and a status description.
     *
     * @return the status info
     */
    ThingStatusInfo getStatusInfo();

    /**
     * Sets the status info.
     *
     * @param status the new status info
     */
    void setStatusInfo(ThingStatusInfo status);

    /**
     * Sets the handler.
     *
     * @param thingHandler the new handler
     */
    void setHandler(@Nullable ThingHandler thingHandler);

    /**
     * Gets the handler.
     *
     * @return the handler (can be null)
     */
    @Nullable
    ThingHandler getHandler();

    /**
     * Gets the bridge UID.
     *
     * @return the bridge UID (can be null)
     */
    @Nullable
    ThingUID getBridgeUID();

    /**
     * Sets the bridge.
     *
     * @param bridgeUID the new bridge UID
     */
    void setBridgeUID(@Nullable ThingUID bridgeUID);

    /**
     * Gets the configuration.
     *
     * @return the configuration (not null)
     */
    Configuration getConfiguration();

    /**
     * Gets the uid.
     *
     * @return the uid
     */
    @Override
    ThingUID getUID();

    /**
     * Gets the thing type UID.
     *
     * @return the thing type UID
     */
    ThingTypeUID getThingTypeUID();

    /**
     * Returns an immutable copy of the {@link Thing} properties.
     *
     * @return an immutable copy of the {@link Thing} properties (not null)
     */
    Map<String, String> getProperties();

    /**
     * Sets the property value for the property identified by the given name. If the value to be set is null then the
     * property will be removed.
     *
     * @param name the name of the property to be set (must not be null or empty)
     * @param value the value of the property (if null then the property with the given name is removed)
     * @return the previous value associated with the name, or null if there was no mapping for the name
     */
    @Nullable
    String setProperty(String name, @Nullable String value);

    /**
     * Updates all properties of the thing.
     *
     * @param properties the properties to set (must not be null)
     */
    void setProperties(Map<String, String> properties);

    /**
     * Get the physical location of the {@link Thing}.
     *
     * @return the location identifier (presumably an item name) or {@code null} if no location has been configured.
     */
    @Nullable
    String getLocation();

    /**
     * Set the physical location of the {@link Thing}.
     *
     * @param location the location identifier (preferably an item name) or {@code null} if no location has been
     *            configured.
     */
    void setLocation(@Nullable String location);

    /**
     * Returns information whether the {@link Thing} is enabled or not.
     *
     * @return Returns {@code true} if the thing is enabled. Return {@code false} otherwise.
     */
    boolean isEnabled();
}
