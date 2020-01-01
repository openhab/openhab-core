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

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Registry;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.core.validation.ConfigValidationException;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.internal.ThingTracker;

/**
 * {@link ThingRegistry} tracks all {@link Thing}s from different {@link ThingProvider}s and provides access to them.
 * The {@link ThingRegistry} supports adding of listeners (see {@link ThingsChangeListener}) and trackers
 * (see {@link ThingTracker}).
 *
 * @author Dennis Nobel - Initial contribution
 * @author Oliver Libutzki - Extracted ManagedThingProvider
 * @auther Thomas Höfer - Added config description validation exception to updateConfiguration operation
 */
@NonNullByDefault
public interface ThingRegistry extends Registry<Thing, ThingUID> {

    /**
     * Returns a thing for a given UID or null if no thing was found.
     *
     * @param uid thing UID
     * @return thing for a given UID or null if no thing was found
     */
    @Override
    @Nullable
    Thing get(ThingUID uid);

    /**
     * Returns a channel for the given channel UID or null if no channel was found
     *
     * @param channelUID channel UID
     * @return channel for the given channel UID or null of no channel was found
     */
    @Nullable
    Channel getChannel(ChannelUID channelUID);

    /**
     * Updates the configuration of a thing for the given UID.
     *
     * @param thingUID thing UID
     * @param configurationParameters configuration parameters
     * @throws ConfigValidationException if one or more of the given configuration parameters do not match
     *             their declarations in the configuration description
     * @throws IllegalArgumentException if no thing with the given UID exists
     * @throws IllegalStateException if no handler is attached to the thing
     */
    void updateConfiguration(ThingUID thingUID, Map<String, Object> configurationParameters);

    /**
     * Initiates the removal process for the {@link Thing} specified by the given {@link ThingUID}.
     *
     * Unlike in other {@link Registry}s, {@link Thing}s don't get removed immediately.
     * Instead, the corresponding {@link ThingHandler} is given the chance to perform
     * any required removal handling before it actually gets removed.
     * <p>
     * If for any reasons the {@link Thing} should be removed immediately without any prior processing, use
     * {@link #forceRemove(ThingUID)} instead.
     *
     * @param thingUID Identificator of the {@link Thing} to be removed
     * @return the {@link Thing} that was removed, or null if no {@link Thing} with the given {@link ThingUID} exists
     */
    @Override
    @Nullable
    Thing remove(ThingUID thingUID);

    /**
     * Removes the {@link Thing} specified by the given {@link ThingUID}.
     *
     * If the corresponding {@link ThingHandler} should be given the chance to perform
     * any removal operations, use {@link #remove(ThingUID)} instead.
     *
     * @param thingUID Identificator of the {@link Thing} to be removed
     * @return the {@link Thing} that was removed, or null if no {@link Thing} with the given {@link ThingUID} exists
     */
    @Nullable
    Thing forceRemove(ThingUID thingUID);

    /**
     * Creates a thing based on the given configuration properties
     *
     * @param thingTypeUID thing type unique id
     * @param thingUID thing unique id which should be created. This id might be
     *            null.
     * @param bridgeUID the thing's bridge. Null if there is no bridge or if the thing
     *            is a bridge by itself.
     * @param configuration the configuration
     * @return the created thing
     */
    @Nullable
    Thing createThingOfType(ThingTypeUID thingTypeUID, @Nullable ThingUID thingUID, @Nullable ThingUID bridgeUID,
            @Nullable String label, Configuration configuration);
}
