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
package org.openhab.core.thing.internal;

import java.util.Collection;
import java.util.Locale;

import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.type.ThingType;

/**
 * @author Markus Rathgeb - Initial contribution
 */
public class SimpleThingTypeProvider implements ThingTypeProvider {
    private final Collection<ThingType> thingTypes;

    public SimpleThingTypeProvider(final Collection<ThingType> thingTypes) {
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
