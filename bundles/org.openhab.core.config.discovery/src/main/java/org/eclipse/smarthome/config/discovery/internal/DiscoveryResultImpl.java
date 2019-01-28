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
package org.eclipse.smarthome.config.discovery.internal;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultFlag;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class DiscoveryResultImpl implements DiscoveryResult {

    private ThingUID bridgeUID;
    private ThingUID thingUID;
    private ThingTypeUID thingTypeUID;

    private Map<String, Object> properties;
    private String representationProperty;
    private DiscoveryResultFlag flag;
    private String label;
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
     * @param thingUID
     *            the Thing UID to be set (must not be null). If a {@code Thing} disappears and is discovered again, the
     *            same {@code Thing} ID
     *            must be created. A typical {@code Thing} ID could be the
     *            serial number. It's usually <i>not</i> a product name.
     * @param properties the properties to be set (could be null or empty)
     * @param representationProperty the representationProperty to be set (could be null or empty)
     * @param label the human readable label to set (could be null or empty)
     * @param bridgeUID the unique bridge ID to be set
     * @param timeToLive time to live in seconds
     *
     * @throws IllegalArgumentException
     *             if the Thing type UID or the Thing UID is null
     * @deprecated use {@link #DiscoveryResultImpl(ThingUID, ThingTypeUID, ThingUID, Map, String, String, long)}
     *             instead.
     */
    @Deprecated
    public DiscoveryResultImpl(ThingUID thingUID, ThingUID bridgeUID, Map<String, Object> properties,
            String representationProperty, String label, long timeToLive) throws IllegalArgumentException {
        this(thingUID.getThingTypeUID(), thingUID, bridgeUID, properties, representationProperty, label, timeToLive);
    }

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param thingTypeUID the {@link ThingTypeUID}
     * @param thingUID the Thing UID to be set (must not be null). If a {@code Thing} disappears and is discovered
     *            again, the same {@code Thing} ID must be created. A typical {@code Thing} ID could be the serial
     *            number. It's usually <i>not</i> a product name.
     * @param properties the properties to be set (could be null or empty)
     * @param representationProperty the representationProperty to be set (could be null or empty)
     * @param label the human readable label to set (could be null or empty)
     * @param bridgeUID the unique bridge ID to be set
     * @param timeToLive time to live in seconds
     *
     * @throws IllegalArgumentException
     *             if the Thing type UID or the Thing UID is null
     */
    public DiscoveryResultImpl(ThingTypeUID thingTypeUID, ThingUID thingUID, ThingUID bridgeUID,
            Map<String, Object> properties, String representationProperty, String label, long timeToLive)
            throws IllegalArgumentException {
        if (thingUID == null) {
            throw new IllegalArgumentException("The thing UID must not be null!");
        }
        if (timeToLive < 1 && timeToLive != TTL_UNLIMITED) {
            throw new IllegalArgumentException("The ttl must not be 0 or negative!");
        }

        this.thingUID = thingUID;
        this.thingTypeUID = thingTypeUID;
        this.bridgeUID = bridgeUID;
        this.properties = Collections
                .unmodifiableMap((properties != null) ? new HashMap<>(properties) : new HashMap<String, Object>());
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
        if (this.thingTypeUID != null) {
            return this.thingTypeUID;
        } else {
            // fallback for discovery result which were created before the thingTypeUID field was added
            return this.thingUID.getThingTypeUID();
        }
    }

    @Override
    public String getBindingId() {
        ThingUID thingId = this.thingUID;
        if (thingId != null) {
            return thingId.getBindingId();
        }
        return "";
    }

    @Override
    public Map<String, Object> getProperties() {
        return this.properties;
    }

    @Override
    public String getRepresentationProperty() {
        return this.representationProperty;
    }

    @Override
    public DiscoveryResultFlag getFlag() {
        return this.flag;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public ThingUID getBridgeUID() {
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
    public void synchronize(DiscoveryResult sourceResult) {
        if ((sourceResult != null) && (sourceResult.getThingUID().equals(this.thingUID))) {
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
     * @param flag the flag of this result object to be set (could be null)
     */
    public void setFlag(DiscoveryResultFlag flag) {
        this.flag = (flag == null) ? DiscoveryResultFlag.NEW : flag;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((thingUID == null) ? 0 : thingUID.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
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
