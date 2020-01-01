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

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
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
     * @return the Thing ID of this result object (not null, not empty)
     */
    public ThingUID getThingUID();

    /**
     * Returns the unique {@code Thing} type ID of this result object.
     * <p>
     * A {@code Thing} type ID could be a product number which identifies the same type of {@link Thing}s. It's usually
     * <i>not</i> a serial number.
     *
     * @return the unique Thing type of this result object (not null, not empty)
     */
    public ThingTypeUID getThingTypeUID();

    /**
     * Returns the binding ID of this result object.
     * <p>
     * The binding ID is extracted from the unique {@code Thing} ID.
     *
     * @return the binding ID of this result object (not null, not empty)
     */
    public String getBindingId();

    /**
     * Returns the properties of this result object.<br>
     * The properties contain information which become part of a {@code Thing}.
     * <p>
     * <b>Hint:</b> The returned properties are immutable.
     *
     * @return the properties of this result object (not null, could be empty)
     */
    public Map<String, Object> getProperties();

    /**
     * Returns the representation property of this result object.
     * <p>
     * The representation property represents an unique human and/or machine readable identifier of the thing that was
     * discovered. Its actual value can be retrieved from the {@link DiscoveryResult#getProperties()} map. Such unique
     * identifiers are typically the <code>ipAddress</code>, the <code>macAddress</code> or the
     * <code>serialNumber</code> of the discovered thing.
     *
     * @return the representation property of this result object (could be null)
     */
    public @Nullable String getRepresentationProperty();

    /**
     * Returns the flag of this result object.<br>
     * The flag signals e.g. if the result is {@link DiscoveryResultFlag#NEW} or has been marked as
     * {@link DiscoveryResultFlag#IGNORED}. In the latter
     * case the result object should be regarded as known by the system so that
     * further processing should be skipped.
     *
     * @return the flag of this result object (not null)
     */
    public DiscoveryResultFlag getFlag();

    /**
     * Returns the human readable label for this result object.
     *
     * @return the human readable label for this result object (not null, could be empty)
     */
    public String getLabel();

    /**
     * Returns the unique bridge ID of the {@link DiscoveryResult}.
     *
     * @return the unique bridge ID (could be null)
     */
    public @Nullable ThingUID getBridgeUID();

    /**
     * Get the timestamp of this {@link DiscoveryResult}.
     *
     * @return timestamp as long
     */
    public long getTimestamp();

    /**
     * Get the time to live in seconds for this entry.
     *
     * @return time to live in seconds
     */
    public long getTimeToLive();
}
