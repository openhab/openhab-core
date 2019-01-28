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
package org.eclipse.smarthome.core.semantics;

import java.util.function.Predicate;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.semantics.model.Equipment;
import org.eclipse.smarthome.core.semantics.model.Location;
import org.eclipse.smarthome.core.semantics.model.Point;
import org.eclipse.smarthome.core.semantics.model.Property;
import org.eclipse.smarthome.core.semantics.model.Tag;

/**
 * This class provides predicates that allow filtering item streams with regards to their semantics.
 *
 * @author Kai Kreuzer - Initial contribution
 *
 */
@NonNullByDefault
public class SemanticsPredicates {

    /**
     * Creates a {@link Predicate} which can be used to filter {@link Item}s that represent a Location.
     *
     * @return created {@link Predicate}
     */
    public static Predicate<Item> isLocation() {
        return i -> isA(Location.class).test(i);
    }

    /**
     * Creates a {@link Predicate} which can be used to filter {@link Item}s that represent an Equipment.
     *
     * @return created {@link Predicate}
     */
    public static Predicate<Item> isEquipment() {
        return i -> isA(Equipment.class).test(i);
    }

    /**
     * Creates a {@link Predicate} which can be used to filter {@link Item}s that represent a Point.
     *
     * @return created {@link Predicate}
     */
    public static Predicate<Item> isPoint() {
        return i -> isA(Point.class).test(i);
    }

    /**
     * Creates a {@link Predicate} which can be used to filter {@link Item}s that represent a given semantic type.
     *
     * @param type the semantic type to filter for
     * @return created {@link Predicate}
     */
    public static Predicate<Item> isA(Class<? extends Tag> type) {
        return i -> {
            Class<? extends Tag> semanticType = SemanticTags.getSemanticType(i);
            return semanticType != null && type.isAssignableFrom(semanticType);
        };
    }

    /**
     * Creates a {@link Predicate} which can be used to filter {@link Item}s that relates to a given property.
     *
     * @param type the semantic property to filter for
     * @return created {@link Predicate}
     */
    public static Predicate<Item> relatesTo(Class<? extends Property> property) {
        return i -> {
            Class<? extends Tag> semanticProperty = SemanticTags.getProperty(i);
            return semanticProperty != null && property.isAssignableFrom(semanticProperty);
        };
    }

}
