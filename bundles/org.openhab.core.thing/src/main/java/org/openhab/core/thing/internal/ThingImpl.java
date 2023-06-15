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
package org.openhab.core.thing.internal;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.builder.ThingStatusInfoBuilder;

/**
 * The {@link ThingImpl} class is a concrete implementation of the {@link Thing}.
 * <p>
 * This class is mutable.
 *
 * @author Denis Nobel - Initial contribution
 * @author Michael Grammling - Configuration could never be null but may be empty
 * @author Benedikt Niehues - Fix ESH Bug 450236
 *         https://bugs.eclipse.org/bugs/show_bug.cgi?id=450236 - Considering
 *         ThingType Description
 * @author Thomas Höfer - Added thing and thing type properties
 * @author Simon Kaufmann - Added label
 * @author Christoph Weitkamp - Added method `getChannel(ChannelUID)`
 */
@NonNullByDefault
public class ThingImpl implements Thing {

    private @Nullable String label;

    private @Nullable ThingUID bridgeUID;

    private final Map<ChannelUID, Channel> channels = new LinkedHashMap<>();

    private Configuration configuration = new Configuration();

    private Map<String, String> properties = new HashMap<>();

    private @NonNullByDefault({}) ThingUID uid;

    private @NonNullByDefault({}) ThingTypeUID thingTypeUID;

    private @Nullable String location;

    private transient volatile ThingStatusInfo status = ThingStatusInfoBuilder
            .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.NONE).build();

    private transient volatile @Nullable ThingHandler thingHandler;

    /**
     * Package protected default constructor to allow reflective instantiation.
     *
     * !!! DO NOT REMOVE - Gson needs it !!!
     */
    ThingImpl() {
    }

    /**
     * @param thingTypeUID thing type UID
     * @param thingId thing ID
     */
    public ThingImpl(ThingTypeUID thingTypeUID, String thingId) {
        this.uid = new ThingUID(thingTypeUID.getBindingId(), thingTypeUID.getId(), thingId);
        this.thingTypeUID = thingTypeUID;
    }

    /**
     * @param thingTypeUID thing type UID
     * @param thingUID thing UID
     */
    public ThingImpl(ThingTypeUID thingTypeUID, ThingUID thingUID) {
        this.uid = thingUID;
        this.thingTypeUID = thingTypeUID;
    }

    @Override
    public @Nullable String getLabel() {
        return label;
    }

    @Override
    public void setLabel(@Nullable String label) {
        this.label = label;
    }

    @Override
    public @Nullable ThingUID getBridgeUID() {
        return this.bridgeUID;
    }

    @Override
    public List<Channel> getChannels() {
        return List.copyOf(this.channels.values());
    }

    @Override
    public List<Channel> getChannelsOfGroup(String channelGroupId) {
        return this.channels.entrySet().stream().filter(c -> channelGroupId.equals(c.getKey().getGroupId()))
                .map(Map.Entry::getValue).toList();
    }

    @Override
    public @Nullable Channel getChannel(String channelId) {
        return getChannel(new ChannelUID(uid, channelId));
    }

    @Override
    public @Nullable Channel getChannel(ChannelUID channelUID) {
        return this.channels.get(channelUID);
    }

    @Override
    public Configuration getConfiguration() {
        return this.configuration;
    }

    @Override
    public @Nullable ThingHandler getHandler() {
        return this.thingHandler;
    }

    @Override
    public ThingUID getUID() {
        return uid;
    }

    @Override
    public ThingStatus getStatus() {
        return status.getStatus();
    }

    @Override
    public ThingStatusInfo getStatusInfo() {
        return status;
    }

    @Override
    public void setBridgeUID(@Nullable ThingUID bridgeUID) {
        this.bridgeUID = bridgeUID;
    }

    public void addChannel(Channel channel) {
        this.channels.put(channel.getUID(), channel);
    }

    public void setChannels(List<Channel> channels) {
        this.channels.clear();
        channels.forEach(this::addChannel);
    }

    public void setConfiguration(@Nullable Configuration configuration) {
        this.configuration = (configuration == null) ? new Configuration() : configuration;
    }

    @Override
    public void setHandler(@Nullable ThingHandler thingHandler) {
        this.thingHandler = thingHandler;
    }

    public void setId(ThingUID id) {
        this.uid = id;
    }

    @Override
    public void setStatusInfo(ThingStatusInfo status) {
        this.status = status;
    }

    @Override
    public ThingTypeUID getThingTypeUID() {
        return this.thingTypeUID;
    }

    public void setThingTypeUID(ThingTypeUID thingTypeUID) {
        this.thingTypeUID = thingTypeUID;
    }

    @Override
    public Map<String, String> getProperties() {
        synchronized (this) {
            return Map.copyOf(properties);
        }
    }

    @Override
    public @Nullable String setProperty(String name, @Nullable String value) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Property name must not be null or empty");
        }
        synchronized (this) {
            if (value == null) {
                return properties.remove(name);
            }
            return properties.put(name, value);
        }
    }

    @Override
    public void setProperties(Map<String, String> properties) {
        this.properties = new HashMap<>(properties);
    }

    @Override
    public @Nullable String getLocation() {
        return location;
    }

    @Override
    public void setLocation(@Nullable String location) {
        this.location = location;
    }

    @Override
    public boolean isEnabled() {
        return ThingStatusDetail.DISABLED != getStatusInfo().getStatusDetail();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + uid.hashCode();
        return result;
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
        ThingImpl other = (ThingImpl) obj;
        return uid.equals(other.uid);
    }
}
