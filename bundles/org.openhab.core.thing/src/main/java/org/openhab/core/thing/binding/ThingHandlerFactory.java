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
package org.openhab.core.thing.binding;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.core.status.ConfigStatusProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.firmware.FirmwareUpdateHandler;

/**
 * The {@link ThingHandlerFactory} is responsible for creating {@link Thing}s and {@link ThingHandler}s. Therefore the
 * factory must be registered as OSGi service.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Stefan Bußweiler - API changes due to bridge/thing life cycle refactoring
 */
@NonNullByDefault
public interface ThingHandlerFactory {

    /**
     * Returns whether the handler is able to create a thing or register a thing handler for the given type.
     *
     * @param thingTypeUID the thing type UID
     * @return true, if the handler supports the thing type, false otherwise
     */
    boolean supportsThingType(ThingTypeUID thingTypeUID);

    /**
     * Creates a new {@link ThingHandler} instance. In addition, the handler can be registered as a service if it is
     * required, e.g. as {@link FirmwareUpdateHandler}, {@link ConfigStatusProvider}.
     * <p>
     * This method is only called if the {@link ThingHandlerFactory} supports the type of the given thing.
     * <p>
     * The framework expects this method to be non-blocking and return quickly.
     * <p>
     *
     * @param thing the thing for which a new handler must be registered
     * @return the created thing handler instance, not null
     * @throws IllegalStateException if the handler instance could not be created
     */
    ThingHandler registerHandler(Thing thing);

    /**
     * Unregisters a {@link ThingHandler} instance.
     * <p>
     * The framework expects this method to be non-blocking and return quickly.
     * <p>
     *
     * @param thing the thing for which the handler must be unregistered
     */
    void unregisterHandler(Thing thing);

    /**
     * Creates a thing for given arguments.
     *
     * @param thingTypeUID thing type uid (not null)
     * @param configuration configuration
     * @param thingUID thing uid, which can be null
     * @param bridgeUID bridge uid, which can be null
     * @return created thing
     */
    @Nullable
    Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration, @Nullable ThingUID thingUID,
            @Nullable ThingUID bridgeUID);

    /**
     * A thing with the given {@link Thing} UID was removed.
     *
     * @param thingUID thing UID of the removed object
     */
    void removeThing(ThingUID thingUID);
}
