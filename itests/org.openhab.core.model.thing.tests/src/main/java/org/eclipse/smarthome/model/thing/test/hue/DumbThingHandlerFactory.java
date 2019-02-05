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
package org.eclipse.smarthome.model.thing.test.hue;

import java.util.Collections;
import java.util.Set;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.ComponentContext;

/**
 * {@link ThingHandlerFactory} that can be switched into <code>dumb</code> mode
 *
 * In <code>dumb</code> mode, it behaves as if the XML configuration files have not been processed yet,
 * i.e. it returns <code>null</code> on {@link #createThing(ThingTypeUID, Configuration, ThingUID, ThingUID)}
 *
 * @author Simon Kaufmann - Initial contribution and API
 */
public class DumbThingHandlerFactory extends BaseThingHandlerFactory implements ThingHandlerFactory {

    public static final String BINDING_ID = "dumb";

    public final static ThingTypeUID THING_TYPE_TEST = new ThingTypeUID(BINDING_ID, "DUMB");

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_TEST);

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
