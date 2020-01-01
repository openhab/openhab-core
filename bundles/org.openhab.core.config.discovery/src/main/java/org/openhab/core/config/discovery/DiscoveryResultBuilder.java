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
package org.openhab.core.config.discovery;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.internal.DiscoveryResultImpl;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;

/**
 * The {@link DiscoveryResultBuilder} helps creating a {@link DiscoveryResult} through the builder pattern.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Andre Fuechsel - added support for time to live
 * @author Thomas Höfer - Added representation
 *
 * @see DiscoveryResult
 */
@NonNullByDefault
public class DiscoveryResultBuilder {

    private final ThingUID thingUID;

    private @Nullable ThingUID bridgeUID;
    private final Map<String, Object> properties = new HashMap<>();
    private @Nullable String representationProperty;
    private @Nullable String label;
    private long ttl = DiscoveryResult.TTL_UNLIMITED;
    private @Nullable ThingTypeUID thingTypeUID;

    private DiscoveryResultBuilder(ThingUID thingUID) {
        this.thingTypeUID = thingUID.getThingTypeUID();
        this.thingUID = thingUID;
    };

    /**
     * Creates a new builder for a given thing UID.
     *
     * @param thingUID the thing UID for which the builder should be created-
     * @return a new instance of a {@link DiscoveryResultBuilder}
     */
    public static DiscoveryResultBuilder create(ThingUID thingUID) {
        return new DiscoveryResultBuilder(thingUID);
    }

    /**
     * Explicitly sets the thing type.
     *
     * @param thingTypeUID the {@link ThingTypeUID}
     * @return the updated builder
     */
    public DiscoveryResultBuilder withThingType(@Nullable ThingTypeUID thingTypeUID) {
        this.thingTypeUID = thingTypeUID;
        return this;
    }

    /**
     * Adds properties to the desired result.
     *
     * @param properties of the desired result
     * @return the updated builder
     */
    public DiscoveryResultBuilder withProperties(@Nullable Map<String, Object> properties) {
        if (properties != null) {
            this.properties.putAll(properties);
        }
        return this;
    }

    /**
     * Adds a property to the desired result.
     *
     * @param property of the desired result
     * @return the updated builder
     */
    public DiscoveryResultBuilder withProperty(String key, Object value) {
        this.properties.put(key, value);
        return this;
    }

    /**
     * Sets the representation Property of the desired result.
     *
     * @param representationProperty the representation property of the desired result
     * @return the updated builder
     */
    public DiscoveryResultBuilder withRepresentationProperty(@Nullable String representationProperty) {
        this.representationProperty = representationProperty;
        return this;
    }

    /**
     * Sets the bridgeUID of the desired result.
     *
     * @param bridgeUID of the desired result
     * @return the updated builder
     */
    public DiscoveryResultBuilder withBridge(@Nullable ThingUID bridgeUID) {
        this.bridgeUID = bridgeUID;
        return this;
    }

    /**
     * Sets the label of the desired result.
     *
     * @param label of the desired result
     * @return the updated builder
     */
    public DiscoveryResultBuilder withLabel(@Nullable String label) {
        this.label = label;
        return this;
    }

    /**
     * Sets the time to live for the result in seconds.
     *
     * @param ttl time to live in seconds
     * @return the updated builder
     */
    public DiscoveryResultBuilder withTTL(long ttl) {
        this.ttl = ttl;
        return this;
    }

    /**
     * Builds a result with the settings of this builder.
     *
     * @return the desired result
     */
    public DiscoveryResult build() {
        return new DiscoveryResultImpl(thingTypeUID, thingUID, bridgeUID, properties, representationProperty, label,
                ttl);
    }

}
