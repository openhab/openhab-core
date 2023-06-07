/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.semantics;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This class provides a stream of all defined locations.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class Locations {

    static final Set<Class<? extends Location>> LOCATIONS = ConcurrentHashMap.newKeySet();

    static {
        LOCATIONS.add(Location.class);
    }

    public static Stream<Class<? extends Location>> stream() {
        return LOCATIONS.stream();
    }

    public static boolean add(Class<? extends Location> tag) {
        return LOCATIONS.add(tag);
    }

    public static boolean remove(Class<? extends Location> tag) {
        return LOCATIONS.remove(tag);
    }
}
