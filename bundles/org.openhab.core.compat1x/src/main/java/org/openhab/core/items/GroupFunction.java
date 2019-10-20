/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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

import java.util.List;

import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * Group functions are used by active group items to calculate a state for the group
 * out of the states of all its member items.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public abstract interface GroupFunction {

    /**
     * Determines the current state of a group based on a list of items
     *
     * @param items the items to calculate a group state for
     * @return the calculated group state
     */
    public State calculate(List<Item> items);

    /**
     * Calculates the group state and returns it as a state of the requested type.
     *
     * @param items the items to calculate a group state for
     * @param stateClass the type in which the state should be returned
     * @return the calculated group state of the requested type or null, if type is not supported
     */
    public State getStateAs(List<Item> items, Class<? extends State> stateClass);

    /**
     * This is the default group function that does nothing else than to check if all member items
     * have the same state. If this is the case, this state is returned, otherwise UNDEF is returned.
     *
     * @author Kai Kreuzer - Initial contribution
     *
     */
    static class Equality implements GroupFunction {

        /**
         * @{inheritDoc
         */
        @Override
        public State calculate(List<Item> items) {
            if (!items.isEmpty()) {
                State state = items.get(0).getState();
                for (int i = 1; i < items.size(); i++) {
                    if (!state.equals(items.get(i).getState())) {
                        return UnDefType.UNDEF;
                    }
                }
                return state;
            } else {
                return UnDefType.UNDEF;
            }
        }

        /**
         * @{inheritDoc
         */
        @Override
        public State getStateAs(List<Item> items, Class<? extends State> stateClass) {
            State state = calculate(items);
            if (stateClass.isInstance(state)) {
                return state;
            } else {
                return null;
            }
        }
    }

}
