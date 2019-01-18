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
package org.eclipse.smarthome.core.thing.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ThingStatusInfoBuilder;

/**
 * The {@link ThingImpl} class is a concrete implementation of the {@link Thing}.
 * <p>
 * This class is mutable.
 *
 * @author Michael Grammling - Configuration could never be null but may be empty
 * @author Benedikt Niehues - Fix ESH Bug 450236
 *         https://bugs.eclipse.org/bugs/show_bug.cgi?id=450236 - Considering
 *         ThingType Description
 * @author Thomas Höfer - Added thing and thing type properties
 * @author Simon Kaufmann - Added label
 *
 */
@NonNullByDefault
public class ThingImpl implements Thing {

    private @Nullable String label;

    private @Nullable ThingUID bridgeUID;

    private List<Channel> channels = new ArrayList<>(0);

    private Configuration configuration = new Configuration();

    private Map<String, String> properties = new HashMap<>();

    @NonNullByDefault({})
    private ThingUID uid;

    @NonNullByDefault({})
    private ThingTypeUID thingTypeUID;

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
     * @throws IllegalArgumentException
     */
    public ThingImpl(ThingTypeUID thingTypeUID, String thingId) throws IllegalArgumentException {
        this.uid = new ThingUID(thingTypeUID.getBindingId(), thingTypeUID.getId(), thingId);
        this.thingTypeUID = thingTypeUID;
    }

    /**
     * @param thingUID
     * @throws IllegalArgumentException
     * @deprecated use {@link #ThingImpl(ThingTypeUID, ThingUID)} instead.
     */
    @Deprecated
    public ThingImpl(ThingUID thingUID) throws IllegalArgumentException {
        if ("".equals(thingUID.getThingTypeId())) {
            throw new IllegalArgumentException(
                    "The given ThingUID does not specify a ThingType. You might want to use ThingImpl(ThingTypeUID, ThingUID) instead.");
        }
        this.uid = thingUID;
        this.thingTypeUID = new ThingTypeUID(thingUID.getBindingId(), thingUID.getThingTypeId());
    }

    /**
     * @param thingTypeUID thing type UID
     * @param thingUID
     * @throws IllegalArgumentException
     */
    public ThingImpl(ThingTypeUID thingTypeUID, ThingUID thingUID) throws IllegalArgumentException {
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
        return Collections.unmodifiableList(new ArrayList<>(this.channels));
    }

    @Override
    public List<Channel> getChannelsOfGroup(String channelGroupId) {
        List<Channel> channels = new ArrayList<>();
        for (Channel channel : this.channels) {
            if (channelGroupId.equals(channel.getUID().getGroupId())) {
                channels.add(channel);
            }
        }
        return Collections.unmodifiableList(channels);
    }

    @Override
    public @Nullable Channel getChannel(String channelId) {
        for (Channel channel : this.channels) {
            if (channel.getUID().getId().equals(channelId)) {
                return channel;
            }
        }
        return null;
    }

    public List<Channel> getChannelsMutable() {
        return this.channels;
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

    public void setChannels(List<Channel> channels) {
        this.channels = channels;
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
            return Collections.unmodifiableMap(new HashMap<>(properties));
        }
    }

    @Override
    public @Nullable String setProperty(String name, @Nullable String value) {
        if (StringUtils.isEmpty(name)) {
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
        result = prime * result + ((uid == null) ? 0 : uid.hashCode());
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
        if (uid == null) {
            if (other.uid != null) {
                return false;
            }
        } else if (!uid.equals(other.uid)) {
            return false;
        }
        return true;
    }

}
