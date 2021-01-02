/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.AbstractUID;
import org.openhab.core.config.discovery.internal.DiscoveryResultImpl;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private Logger logger = LoggerFactory.getLogger(DiscoveryResultBuilder.class);

    private final ThingUID thingUID;

    private @Nullable ThingUID bridgeUID;
    private final Map<String, Object> properties = new HashMap<>();
    private @Nullable String representationProperty;
    private @Nullable String label;
    private long ttl = DiscoveryResult.TTL_UNLIMITED;
    private @Nullable ThingTypeUID thingTypeUID;

    private DiscoveryResultBuilder(ThingUID thingUID) {
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
        properties.put(key, value);
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
        validateThingUID(bridgeUID);
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
    @SuppressWarnings("deprecation")
    public DiscoveryResult build() {
        if (representationProperty != null && !properties.containsKey(representationProperty)) {
            logger.warn(
                    "Representation property '{}' of discovery result for thing '{}' is missing in properties map. It has to be fixed by the bindings developer.\n{}",
                    representationProperty, thingUID, getStackTrace(Thread.currentThread()));
        }
        if (thingTypeUID == null) {
            String[] segments = thingUID.getAsString().split(AbstractUID.SEPARATOR);
            if (!segments[1].isEmpty()) {
                thingTypeUID = new ThingTypeUID(thingUID.getBindingId(), segments[1]);
            }
        }
        return new DiscoveryResultImpl(thingTypeUID, thingUID, bridgeUID, properties, representationProperty, label,
                ttl);
    }

    private void validateThingUID(@Nullable ThingUID bridgeUID) {
        if (bridgeUID != null && (!thingUID.getBindingId().equals(bridgeUID.getBindingId())
                || !thingUID.getBridgeIds().contains(bridgeUID.getId()))) {
            throw new IllegalArgumentException(
                    "Thing UID '" + thingUID + "' does not match bridge UID '" + bridgeUID + "'");
        }
    }

    private String getStackTrace(final Thread thread) {
        StackTraceElement[] elements = AccessController.doPrivileged(new PrivilegedAction<StackTraceElement[]>() {
            @Override
            public StackTraceElement[] run() {
                return thread.getStackTrace();
            }
        });
        return Arrays.stream(elements).map(element -> "\tat " + element.toString()).collect(Collectors.joining("\n"));
    }
}
