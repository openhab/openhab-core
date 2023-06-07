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
 * This class provides a stream of all defined equipments.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class Equipments {

    static final Set<Class<? extends Equipment>> EQUIPMENTS = ConcurrentHashMap.newKeySet();

    static {
        EQUIPMENTS.add(Equipment.class);
    }

    public static Stream<Class<? extends Equipment>> stream() {
        return EQUIPMENTS.stream();
    }

    public static boolean add(Class<? extends Equipment> tag) {
        return EQUIPMENTS.add(tag);
    }

    public static boolean remove(Class<? extends Equipment> tag) {
        return EQUIPMENTS.remove(tag);
    }
}
