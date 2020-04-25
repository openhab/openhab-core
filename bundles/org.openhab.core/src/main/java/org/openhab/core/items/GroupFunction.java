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
package org.openhab.core.items;

import java.util.Iterator;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * Group functions are used by active group items to calculate a state for the group
 * out of the states of all its member items.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public interface GroupFunction {

    /**
     * Determines the current state of a group based on a list of items
     *
     * @param items the items to calculate a group state for
     * @return the calculated group state
     */
    State calculate(@Nullable Set<Item> items);

    /**
     * Calculates the group state and returns it as a state of the requested type.
     *
     * @param items the items to calculate a group state for
     * @param stateClass the type in which the state should be returned
     * @return the calculated group state of the requested type or null, if type is not supported
     */
    @Nullable
    <T extends State> T getStateAs(@Nullable Set<Item> items, Class<T> stateClass);

    /**
     * Returns the parameters of the function as an array.
     *
     * @return the parameters of this function
     */
    State[] getParameters();

    /**
     * This is the default group function that does nothing else than to check if all member items
     * have the same state. If this is the case, this state is returned, otherwise UNDEF is returned.
     *
     * @author Kai Kreuzer - Initial contribution
     */
    static class Equality implements GroupFunction {

        @Override
        public State calculate(@Nullable Set<Item> items) {
            if (items == null || items.isEmpty()) {
                return UnDefType.UNDEF;
            } else {
                Iterator<Item> it = items.iterator();
                State state = it.next().getState();
                while (it.hasNext()) {
                    if (!state.equals(it.next().getState())) {
                        return UnDefType.UNDEF;
                    }
                }
                return state;
            }
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
