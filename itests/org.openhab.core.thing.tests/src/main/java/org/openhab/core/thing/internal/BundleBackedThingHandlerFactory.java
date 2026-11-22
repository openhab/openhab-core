/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.thing.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;

/**
 * Delegates to a mock while giving the update instruction reader a factory class in the test bundle.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
record BundleBackedThingHandlerFactory(ThingHandlerFactory delegate) implements ThingHandlerFactory {

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return delegate.supportsThingType(thingTypeUID);
    }

    @Override
    public ThingHandler registerHandler(Thing thing) {
        return delegate.registerHandler(thing);
    }

    @Override
    public void unregisterHandler(Thing thing) {
        delegate.unregisterHandler(thing);
    }

    @Override
    public @Nullable Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration,
            @Nullable ThingUID thingUID, @Nullable ThingUID bridgeUID) {
        return delegate.createThing(thingTypeUID, configuration, thingUID, bridgeUID);
    }

    @Override
    public void removeThing(ThingUID thingUID) {
        delegate.removeThing(thingUID);
    }
}
