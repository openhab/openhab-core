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
package org.openhab.core.config.discovery;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;

/**
 * The {@link DiscoveryResult} is a container for one result of a discovery process.
 * The discovery process can lead to <i>0..N</i> {@link DiscoveryResult} objects
 * which are fired as an event to registered {@link DiscoveryListener}s.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Andre Fuechsel - added support for time to live
 * @author Thomas Höfer - Added representation
 *
 * @see DiscoveryService
 * @see DiscoveryListener
 */
@NonNullByDefault
public interface DiscoveryResult {

    /**
     * Specifies that the {@link DiscoveryResult} has no given time to live.
     */
    long TTL_UNLIMITED = -1;

    /**
     * Returns the unique {@code Thing} ID of this result object.
     * <p>
     * A {@link ThingUID} must be a unique identifier of a concrete {@code Thing} which <i>must not</i> consist of data
     * which could change (e.g. configuration data such as an IP address). If a {@code Thing} disappears and is
     * discovered again, the same {@code Thing} ID must be created. A typical {@code Thing} ID could be the serial
     * number. It's usually <i>not</i> a product number.
     *
     * @return the Thing ID
     */
    ThingUID getThingUID();

    /**
     * Returns the unique {@code Thing} type ID of this result object.
     * <p>
     * A {@code Thing} type ID could be a product number which identifies the same type of {@link Thing}s. It's usually
     * <i>not</i> a serial number.
     *
     * @return the unique Thing type
     */
    ThingTypeUID getThingTypeUID();

    /**
     * Returns the binding ID of this result object.
     * <p>
     * The binding ID is extracted from the unique {@code Thing} ID.
     *
     * @return the binding ID
     */
    String getBindingId();

    /**
     * Returns the properties of this result object.<br>
     * The properties contain information which become part of a {@code Thing}.
     * <p>
     * <b>Hint:</b> The returned properties are immutable.
     *
     * @return the properties (could be empty)
     */
    Map<String, Object> getProperties();

    /**
     * Returns the representation property of this result object.
     * <p>
     * The representation property represents a unique human and/or machine readable identifier of the thing that was
     * discovered. Its actual value can be retrieved from the {@link DiscoveryResult#getProperties()} map. Such unique
     * identifiers are typically the <code>ipAddress</code>, the <code>macAddress</code> or the
     * <code>serialNumber</code> of the discovered thing.
     *
     * @return the representation property
     */
    @Nullable
    String getRepresentationProperty();

    /**
     * Returns the flag of this result object.<br>
     * The flag signals e.g. if the result is {@link DiscoveryResultFlag#NEW} or has been marked as
     * {@link DiscoveryResultFlag#IGNORED}. In the latter
     * case the result object should be regarded as known by the system so that
     * further processing should be skipped.
     *
     * @return the flag
     */
    DiscoveryResultFlag getFlag();

    /**
     * Returns the human readable label for this result object.
     *
     * @return the human readable label (could be empty)
     */
    String getLabel();

    /**
     * Returns the unique {@link Bridge} ID of this result object.
     *
     * @return the unique Bridge ID
     */
    @Nullable
    ThingUID getBridgeUID();

    /**
     * Returns the timestamp of this result object.
     *
     * @return timestamp as long
     */
    long getTimestamp();

    /**
     * Returns the time to live in seconds for this entry.
     *
     * @return time to live in seconds
     */
    long getTimeToLive();

    /**
     * Normalizes non-configuration properties by converting them to a String.
     * Properties in the list passed to this method remain unchanged.
     *
     * @param configurationParameters a {@link List} containing the names of configuration parameters
     */
    default void normalizePropertiesOnConfigDescription(List<String> configurationParameters) {
        // do nothing - optional
    }
}
