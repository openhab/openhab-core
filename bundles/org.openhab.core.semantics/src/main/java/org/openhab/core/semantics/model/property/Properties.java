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
package org.openhab.core.semantics.model.property;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.semantics.model.Property;

/**
 * This class provides a stream of all defined properties.
 *
 * @author Generated from generateTagClasses.groovy - Initial contribution
 */
@NonNullByDefault
public class Properties {

    static final Set<Class<? extends Property>> PROPERTIES = new HashSet<>();

    static {
        PROPERTIES.add(Property.class);
        PROPERTIES.add(CO.class);
        PROPERTIES.add(CO2.class);
        PROPERTIES.add(ColorTemperature.class);
        PROPERTIES.add(Current.class);
        PROPERTIES.add(Duration.class);
        PROPERTIES.add(Energy.class);
        PROPERTIES.add(Frequency.class);
        PROPERTIES.add(Gas.class);
        PROPERTIES.add(Humidity.class);
        PROPERTIES.add(Level.class);
        PROPERTIES.add(Light.class);
        PROPERTIES.add(Noise.class);
        PROPERTIES.add(Oil.class);
        PROPERTIES.add(Opening.class);
        PROPERTIES.add(Power.class);
        PROPERTIES.add(Presence.class);
        PROPERTIES.add(Pressure.class);
        PROPERTIES.add(Rain.class);
        PROPERTIES.add(Smoke.class);
        PROPERTIES.add(SoundVolume.class);
        PROPERTIES.add(Temperature.class);
        PROPERTIES.add(Timestamp.class);
        PROPERTIES.add(Ultraviolet.class);
        PROPERTIES.add(Vibration.class);
        PROPERTIES.add(Voltage.class);
        PROPERTIES.add(Water.class);
        PROPERTIES.add(Wind.class);
    }

    public static Stream<Class<? extends Property>> stream() {
        return PROPERTIES.stream();
    }
}
