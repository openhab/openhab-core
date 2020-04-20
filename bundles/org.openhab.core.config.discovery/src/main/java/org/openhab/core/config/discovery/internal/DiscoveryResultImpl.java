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
package org.openhab.core.config.discovery.internal;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryResultFlag;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class DiscoveryResultImpl implements DiscoveryResult {

    private @Nullable ThingUID bridgeUID;
    private @NonNullByDefault({}) ThingUID thingUID;
    private @Nullable ThingTypeUID thingTypeUID;

    private Map<String, Object> properties = Collections.emptyMap();
    private @Nullable String representationProperty;
    private @NonNullByDefault({}) DiscoveryResultFlag flag;
    private @NonNullByDefault({}) String label;
    private long timestamp;
    private long timeToLive = TTL_UNLIMITED;

    /**
     * Package protected default constructor to allow reflective instantiation.
     */
    DiscoveryResultImpl() {
    }

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param thingTypeUID the {@link ThingTypeUID}
     * @param thingUID the {@link ThingUID} to be set. If a {@code Thing} disappears and is discovered again, the same
     *            {@code Thing} ID must be created. A typical {@code Thing} ID could be the serial number. It's usually
     *            <i>not</i> a product name.
     * @param bridgeUID the unique {@link Bridge} ID to be set
     * @param properties the properties to be set
     * @param representationProperty the representationProperty to be set
     * @param label the human readable label to set
     * @param timeToLive time to live in seconds
     *
     * @throws IllegalArgumentException if the {@link ThingUID} is null or the time to live is less than 1
     * @deprecated use {@link DiscoveryResultBuilder} instead.
     */
    @Deprecated
    public DiscoveryResultImpl(@Nullable ThingTypeUID thingTypeUID, ThingUID thingUID, @Nullable ThingUID bridgeUID,
            @Nullable Map<String, Object> properties, @Nullable String representationProperty, @Nullable String label,
            long timeToLive) throws IllegalArgumentException {
        if (thingUID == null) {
            throw new IllegalArgumentException("The thing UID must not be null!");
        }
        if (timeToLive < 1 && timeToLive != TTL_UNLIMITED) {
            throw new IllegalArgumentException("The ttl must not be 0 or negative!");
        }

        this.thingUID = thingUID;
        this.thingTypeUID = thingTypeUID;
        this.bridgeUID = bridgeUID;
        this.properties = properties == null ? Collections.emptyMap() : Collections.unmodifiableMap(properties);
        this.representationProperty = representationProperty;
        this.label = label == null ? "" : label;

        this.timestamp = new Date().getTime();
        this.timeToLive = timeToLive;

        this.flag = DiscoveryResultFlag.NEW;
    }

    @Override
    public ThingUID getThingUID() {
        return thingUID;
    }

    @Override
    public ThingTypeUID getThingTypeUID() {
        ThingTypeUID localThingTypeUID = thingTypeUID;
        if (localThingTypeUID != null) {
            return localThingTypeUID;
        } else {
            // fallback for discovery result which were created before the thingTypeUID field was added
            return thingUID.getThingTypeUID();
        }
    }

    @Override
    public String getBindingId() {
        return thingUID.getBindingId();
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public @Nullable String getRepresentationProperty() {
        return representationProperty;
    }

    @Override
    public DiscoveryResultFlag getFlag() {
        return flag;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public @Nullable ThingUID getBridgeUID() {
        return bridgeUID;
    }

    /**
     * Merges the content of the specified source {@link DiscoveryResult} into this object.
     * <p>
     * <i>Hint:</i> The {@link DiscoveryResultFlag} of this object keeps its state.
     * <p>
     * This method returns silently if the specified source {@link DiscoveryResult} is {@code null} or its {@code Thing}
     * type or ID does not fit to this object.
     *
     * @param sourceResult the discovery result which is used as source for the merge
     */
    public void synchronize(@Nullable DiscoveryResult sourceResult) {
        if (sourceResult != null && thingUID.equals(sourceResult.getThingUID())) {
            this.properties = sourceResult.getProperties();
            this.representationProperty = sourceResult.getRepresentationProperty();
            this.label = sourceResult.getLabel();
            this.timestamp = new Date().getTime();
            this.timeToLive = sourceResult.getTimeToLive();
        }
    }

    /**
     * Sets the flag of this result object.<br>
     * The flag signals e.g. if the result is {@link DiscoveryResultFlag#NEW} or has been marked as
     * {@link DiscoveryResultFlag#IGNORED}. In the latter
     * case the result object should be regarded as known by the system so that
     * further processing should be skipped.
     * <p>
     * If the specified flag is {@code null}, {@link DiscoveryResultFlag.NEW} is set by default.
     *
     * @param flag the flag of this result object to be set
     */
    public void setFlag(@Nullable DiscoveryResultFlag flag) {
        this.flag = flag == null ? DiscoveryResultFlag.NEW : flag;
    }

    @Override
    public int hashCode() {
        return 31 + thingUID.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DiscoveryResultImpl other = (DiscoveryResultImpl) obj;
        if (thingUID == null) {
            if (other.thingUID != null) {
                return false;
            }
        } else if (!thingUID.equals(other.thingUID)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "DiscoveryResult [thingUID=" + thingUID + ", properties=" + properties + ", representationProperty="
                + representationProperty + ", flag=" + flag + ", label=" + label + ", bridgeUID=" + bridgeUID + ", ttl="
                + timeToLive + ", timestamp=" + timestamp + "]";
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public long getTimeToLive() {
        return timeToLive;
    }
}
