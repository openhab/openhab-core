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
package org.openhab.core.library.types;

import java.time.ZonedDateTime;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.GroupFunction;
import org.openhab.core.items.Item;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * This interface is a container for group functions that require {@link DateTimeType}s for its calculations.
 *
 * @author Robert Michalak - Initial contribution
 */
@NonNullByDefault
public interface DateTimeGroupFunction extends GroupFunction {

    /**
     * This calculates the maximum value of all item states of DateType type.
     */
    static class Latest implements GroupFunction {

        public Latest() {
        }

        @Override
        public State calculate(@Nullable Set<Item> items) {
            if (items != null && !items.isEmpty()) {
                ZonedDateTime max = null;
                for (Item item : items) {
                    DateTimeType itemState = item.getStateAs(DateTimeType.class);
                    if (itemState != null) {
                        if (max == null || max.isBefore(itemState.getZonedDateTime())) {
                            max = itemState.getZonedDateTime();
                        }
                    }
                }
                if (max != null) {
                    return new DateTimeType(max);
                }
            }
            return UnDefType.UNDEF;
        }

        @Override
        public @Nullable <T extends State> T getStateAs(@Nullable Set<Item> items, Class<T> stateClass) {
            State state = calculate(items);
            if (stateClass.isInstance(state)) {
                return stateClass.cast(state);
            } else {
                return null;
            }
        }

        @Override
        public State[] getParameters() {
            return new State[0];
        }
    }

    /**
     * This calculates the minimum value of all item states of DateType type.
     */
    static class Earliest implements GroupFunction {

        public Earliest() {
        }

        @Override
        public State calculate(@Nullable Set<Item> items) {
            if (items != null && !items.isEmpty()) {
                ZonedDateTime max = null;
                for (Item item : items) {
                    DateTimeType itemState = item.getStateAs(DateTimeType.class);
                    if (itemState != null) {
                        if (max == null || max.isAfter(itemState.getZonedDateTime())) {
                            max = itemState.getZonedDateTime();
                        }
                    }
                }
                if (max != null) {
                    return new DateTimeType(max);
                }
            }
            return UnDefType.UNDEF;
        }

        @Override
        public @Nullable <T extends State> T getStateAs(@Nullable Set<Item> items, Class<T> stateClass) {
            State state = calculate(items);
            if (stateClass.isInstance(state)) {
                return stateClass.cast(state);
            } else {
                return null;
            }
        }

        @Override
        public State[] getParameters() {
            return new State[0];
        }
    }

}
