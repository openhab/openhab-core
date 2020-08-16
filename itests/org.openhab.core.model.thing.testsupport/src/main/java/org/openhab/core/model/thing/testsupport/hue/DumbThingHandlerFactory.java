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
package org.openhab.core.model.thing.testsupport.hue;

import java.util.Set;

import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.ComponentContext;

/**
 * {@link ThingHandlerFactory} that can be switched into <code>dumb</code> mode
 *
 * In <code>dumb</code> mode, it behaves as if the XML configuration files have not been processed yet,
 * i.e. it returns <code>null</code> on {@link #createThing(ThingTypeUID, Configuration, ThingUID, ThingUID)}
 *
 * @author Simon Kaufmann - Initial contribution
 */
public class DumbThingHandlerFactory extends BaseThingHandlerFactory implements ThingHandlerFactory {

    public static final String BINDING_ID = "dumb";

    public static final ThingTypeUID THING_TYPE_TEST = new ThingTypeUID(BINDING_ID, "DUMB");

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Set.of(THING_TYPE_TEST);

    private boolean dumb;

    public DumbThingHandlerFactory(ComponentContext componentContext, boolean dumb) {
        this.dumb = dumb;
        super.activate(componentContext);
    }

    public void setDumb(boolean dumb) {
        this.dumb = dumb;
    }

    @Override
    public Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration, ThingUID thingUID,
            ThingUID bridgeUID) {
        if (dumb) {
            return null;
        } else {
            if (SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
                return super.createThing(thingTypeUID, configuration, thingUID, null);
            }
            throw new IllegalArgumentException("The thing type " + thingTypeUID + " is not supported by the mock.");
        }
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        return null;
    }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
    }
}
