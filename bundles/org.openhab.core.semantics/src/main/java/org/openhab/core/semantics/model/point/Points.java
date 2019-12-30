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
package org.openhab.core.semantics.model.point;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.semantics.model.Point;

/**
 * This class provides a stream of all defined points.
 *
 * @author Generated from generateTagClasses.groovy - Initial contribution
 */
@NonNullByDefault
public class Points {

    static final Set<Class<? extends Point>> POINTS = new HashSet<>();

    static {
        POINTS.add(Point.class);
        POINTS.add(Alarm.class);
        POINTS.add(Control.class);
        POINTS.add(LowBattery.class);
        POINTS.add(Measurement.class);
        POINTS.add(OpenLevel.class);
        POINTS.add(OpenState.class);
        POINTS.add(Setpoint.class);
        POINTS.add(Status.class);
        POINTS.add(Switch.class);
        POINTS.add(Tampered.class);
        POINTS.add(Tilt.class);
    }

    public static Stream<Class<? extends Point>> stream() {
        return POINTS.stream();
    }
}
