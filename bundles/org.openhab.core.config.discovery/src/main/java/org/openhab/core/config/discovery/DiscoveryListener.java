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

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;

/**
 * The {@link DiscoveryListener} interface for receiving discovery events.
 * <p>
 * A class that is interested in processing discovery events fired synchronously by a {@link DiscoveryService} has to
 * implement this interface.
 *
 * @author Michael Grammling - Initial contribution
 * @author Andre Fuechsel - Added removeOlderThings
 *
 * @see DiscoveryService
 */
@NonNullByDefault
public interface DiscoveryListener {

    /**
     * Invoked synchronously when a {@link DiscoveryResult} has been created
     * by the according {@link DiscoveryService}.
     * <p>
     * <i>Hint:</i> This method could even be invoked for {@link DiscoveryResult}s, whose existence has already been
     * informed about.
     *
     * @param source the discovery service which is the source of this event (not null)
     * @param result the discovery result (not null)
     */
    void thingDiscovered(DiscoveryService source, DiscoveryResult result);

    /**
     * Invoked synchronously when an already existing {@code Thing} has been
     * marked to be deleted by the according {@link DiscoveryService}.
     * <p>
     * <i>Hint:</i> This method could even be invoked for {@link DiscoveryResult}s, whose removal has already been
     * informed about.
     *
     * @param source the discovery service which is the source of this event (not null)
     * @param thingUID the Thing UID to be removed (not null)
     */
    void thingRemoved(DiscoveryService source, ThingUID thingUID);

    /**
     * Removes all results belonging to one of the given types that are older
     * than the given timestamp.
     *
     * @param source the discovery service which is the source of this event (not
     *            null)
     * @param timestamp timestamp, all <b>older</b> results will be removed
     * @param thingTypeUIDs collection of {@code ThingType}s, only results of these
     *            {@code ThingType}s will be removed; if {@code null} then
     *            {@link DiscoveryService#getSupportedThingTypes()} will be used
     *            instead
     * @return collection of thing UIDs of all removed things
     * @deprecated use {@link #removeOlderResults(DiscoveryService, long, Collection, ThingUID)} instead
     */
    @Deprecated
    default @Nullable Collection<ThingUID> removeOlderResults(DiscoveryService source, long timestamp,
            @Nullable Collection<ThingTypeUID> thingTypeUIDs) {
        return removeOlderResults(source, timestamp, thingTypeUIDs, null);
    }

    /**
     * Removes all results belonging to one of the given types that are older
     * than the given timestamp.
     *
     * @param source the discovery service which is the source of this event (not
     *            null)
     * @param timestamp timestamp, all <b>older</b> results will be removed
     * @param thingTypeUIDs collection of {@code ThingType}s, only results of these
     *            {@code ThingType}s will be removed; if {@code null} then
     *            {@link DiscoveryService#getSupportedThingTypes()} will be used
     *            instead
     * @param bridgeUID if not {@code null} only results of that bridge are being removed
     * @return collection of thing UIDs of all removed things
     */
    @Nullable
    Collection<ThingUID> removeOlderResults(DiscoveryService source, long timestamp,
            @Nullable Collection<ThingTypeUID> thingTypeUIDs, @Nullable ThingUID bridgeUID);

}
