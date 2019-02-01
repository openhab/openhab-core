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
package org.eclipse.smarthome.core.thing.internal;

import java.util.Collection;
import java.util.Locale;

import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.ThingTypeProvider;
import org.eclipse.smarthome.core.thing.type.ThingType;

public class SimpleThingTypeProvider implements ThingTypeProvider {
    private final Collection<ThingType> thingTypes;

    SimpleThingTypeProvider(final Collection<ThingType> thingTypes) {
        this.thingTypes = thingTypes;
    }

    @Override
    public Collection<ThingType> getThingTypes(final Locale locale) {
        return this.thingTypes;
    }

    @Override
    public ThingType getThingType(final ThingTypeUID thingTypeUID, final Locale locale) {
        for (final ThingType thingType : thingTypes) {
            if (thingType.getUID().equals(thingTypeUID)) {
                return thingType;
            }
        }
        return null;
    }
}
