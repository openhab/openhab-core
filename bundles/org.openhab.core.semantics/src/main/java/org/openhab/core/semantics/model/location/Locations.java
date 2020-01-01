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
package org.openhab.core.semantics.model.location;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.semantics.model.Location;

/**
 * This class provides a stream of all defined locations.
 *
 * @author Generated from generateTagClasses.groovy - Initial contribution
 */
@NonNullByDefault
public class Locations {

    static final Set<Class<? extends Location>> LOCATIONS = new HashSet<>();

    static {
        LOCATIONS.add(Location.class);
        LOCATIONS.add(Attic.class);
        LOCATIONS.add(Basement.class);
        LOCATIONS.add(Bathroom.class);
        LOCATIONS.add(Bedroom.class);
        LOCATIONS.add(Building.class);
        LOCATIONS.add(Carport.class);
        LOCATIONS.add(Corridor.class);
        LOCATIONS.add(FirstFloor.class);
        LOCATIONS.add(Floor.class);
        LOCATIONS.add(Garage.class);
        LOCATIONS.add(Garden.class);
        LOCATIONS.add(GroundFloor.class);
        LOCATIONS.add(Indoor.class);
        LOCATIONS.add(Kitchen.class);
        LOCATIONS.add(LivingRoom.class);
        LOCATIONS.add(Outdoor.class);
        LOCATIONS.add(Room.class);
        LOCATIONS.add(Terrace.class);
    }

    public static Stream<Class<? extends Location>> stream() {
        return LOCATIONS.stream();
    }
}
