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
package org.eclipse.smarthome.core.library.types;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.smarthome.core.items.GroupFunction;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;

/**
 * This interface is only a container for functions that require the core type library
 * for its calculations.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Thomas Eichstädt-Engelen - Added "N" functions
 * @author Gaël L'hopital - Added count function
 *
 */
public interface ArithmeticGroupFunction extends GroupFunction {

    /**
     * This does a logical 'and' operation. Only if all items are of 'activeState' this
     * is returned, otherwise the 'passiveState' is returned.
     *
     * Through the getStateAs() method, it can be determined, how many
     * items actually are not in the 'activeState'.
     */
    static class And implements GroupFunction {

        protected final State activeState;
        protected final State passiveState;

        public And(State activeValue, State passiveValue) {
            if (activeValue == null || passiveValue == null) {
                throw new IllegalArgumentException("Parameters must not be null!");
            }
            this.activeState = activeValue;
            this.passiveState = passiveValue;
        }

        @Override
        public State calculate(Set<Item> items) {
            if (items != null && items.size() > 0) {
                for (Item item : items) {
                    if (!activeState.equals(item.getStateAs(activeState.getClass()))) {
                        return passiveState;
                    }
                }
                return activeState;
            } else {
                // if we do not have any items, we return the passive state
                return passiveState;
            }
        }

        @Override
        public <T extends State> T getStateAs(Set<Item> items, Class<T> stateClass) {
            State state = calculate(items);
            if (stateClass.isInstance(state)) {
                return stateClass.cast(state);
            } else {
                if (stateClass == DecimalType.class) {
                    if (items != null) {
                        return stateClass.cast(new DecimalType(items.size() - count(items, activeState)));
                    } else {
                        return stateClass.cast(DecimalType.ZERO);
                    }
                } else {
                    return null;
                }
            }
        }

        private int count(Set<Item> items, State state) {
            int count = 0;
            if (items != null && state != null) {
                for (Item item : items) {
                    if (state.equals(item.getStateAs(state.getClass()))) {
                        count++;
                    }
                }
            }
            return count;

        }

        @Override
        public State[] getParameters() {
            return new State[] { activeState, passiveState };
        }
    }

    /**
     * This does a logical 'or' operation. If at least one item is of 'activeState' this
     * is returned, otherwise the 'passiveState' is returned.
     *
     * Through the getStateAs() method, it can be determined, how many
     * items actually are in the 'activeState'.
     */
    static class Or implements GroupFunction {

        protected final State activeState;
        protected final State passiveState;

        public Or(State activeValue, State passiveValue) {
            if (activeValue == null || passiveValue == null) {
                throw new IllegalArgumentException("Parameters must not be null!");
            }
            this.activeState = activeValue;
            this.passiveState = passiveValue;
        }

        @Override
        public State calculate(Set<Item> items) {
            if (items != null) {
                for (Item item : items) {
                    if (activeState.equals(item.getStateAs(activeState.getClass()))) {
                        return activeState;
                    }
                }
            }
            return passiveState;
        }

        @Override
        public <T extends State> T getStateAs(Set<Item> items, Class<T> stateClass) {
            State state = calculate(items);
            if (stateClass.isInstance(state)) {
                return stateClass.cast(state);
            } else {
                if (stateClass == DecimalType.class) {
                    return stateClass.cast(new DecimalType(count(items, activeState)));
                } else {
                    return null;
                }
            }
        }

        private int count(Set<Item> items, State state) {
            int count = 0;
            if (items != null && state != null) {
                for (Item item : items) {
                    if (state.equals(item.getStateAs(state.getClass()))) {
                        count++;
                    }
                }
            }
            return count;
        }

        @Override
        public State[] getParameters() {
            return new State[] { activeState, passiveState };
        }
    }

    /**
     * This does a logical 'nand' operation. The state is 'calculated' by
     * the normal 'and' operation and than negated by returning the opposite
     * value. E.g. when the 'and' operation calculates the activeValue the
     * passiveValue will be returned and vice versa.
     */
    static class NAnd extends And {

        public NAnd(State activeValue, State passiveValue) {
            super(activeValue, passiveValue);
        }

        @Override
        public State calculate(Set<Item> items) {
            State result = super.calculate(items);
            State notResult = result.equals(activeState) ? passiveState : activeState;
            return notResult;
        }

    }

    /**
     * This does a logical 'nor' operation. The state is 'calculated' by
     * the normal 'or' operation and than negated by returning the opposite
     * value. E.g. when the 'or' operation calculates the activeValue the
     * passiveValue will be returned and vice versa.
     */
    static class NOr extends Or {

        public NOr(State activeValue, State passiveValue) {
            super(activeValue, passiveValue);
        }

        @Override
        public State calculate(Set<Item> items) {
            State result = super.calculate(items);
            State notResult = result.equals(activeState) ? passiveState : activeState;
            return notResult;
        }

    }

    /**
     * This calculates the numeric average over all item states of decimal type.
     */
    static class Avg implements GroupFunction {

        public Avg() {
        }

        @Override
        public State calculate(Set<Item> items) {
            BigDecimal sum = BigDecimal.ZERO;
            int count = 0;
            if (items != null) {
                for (Item item : items) {
                    DecimalType itemState = item.getStateAs(DecimalType.class);
                    if (itemState != null) {
                        sum = sum.add(itemState.toBigDecimal());
                        count++;
                    }
                }
            }
            if (count > 0) {
                return new DecimalType(sum.divide(BigDecimal.valueOf(count), RoundingMode.HALF_UP));
            } else {
                return UnDefType.UNDEF;
            }
        }

        @Override
        public <T extends State> T getStateAs(Set<Item> items, Class<T> stateClass) {
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
     * This calculates the numeric sum over all item states of decimal type.
     */
    static class Sum implements GroupFunction {

        public Sum() {
        }

        @Override
        public State calculate(Set<Item> items) {
            BigDecimal sum = BigDecimal.ZERO;
            if (items != null) {
                for (Item item : items) {
                    DecimalType itemState = item.getStateAs(DecimalType.class);
                    if (itemState != null) {
                        sum = sum.add(itemState.toBigDecimal());
                    }
                }
            }
            return new DecimalType(sum);
        }

        @Override
        public <T extends State> T getStateAs(Set<Item> items, Class<T> stateClass) {
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
     * This calculates the minimum value of all item states of decimal type.
     */
    static class Min implements GroupFunction {

        public Min() {
        }

        @Override
        public State calculate(Set<Item> items) {
            if (items != null && items.size() > 0) {
                BigDecimal min = null;
                for (Item item : items) {
                    DecimalType itemState = item.getStateAs(DecimalType.class);
                    if (itemState != null) {
                        if (min == null || min.compareTo(itemState.toBigDecimal()) > 0) {
                            min = itemState.toBigDecimal();
                        }
                    }
                }
                if (min != null) {
                    return new DecimalType(min);
                }
            }
            return UnDefType.UNDEF;
        }

        @Override
        public <T extends State> T getStateAs(Set<Item> items, Class<T> stateClass) {
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
     * This calculates the maximum value of all item states of decimal type.
     */
    static class Max implements GroupFunction {

        public Max() {
        }

        @Override
        public State calculate(Set<Item> items) {
            if (items != null && items.size() > 0) {
                BigDecimal max = null;
                for (Item item : items) {
                    DecimalType itemState = item.getStateAs(DecimalType.class);
                    if (itemState != null) {
                        if (max == null || max.compareTo(itemState.toBigDecimal()) < 0) {
                            max = itemState.toBigDecimal();
                        }
                    }
                }
                if (max != null) {
                    return new DecimalType(max);
                }
            }
            return UnDefType.UNDEF;
        }

        @Override
        public <T extends State> T getStateAs(Set<Item> items, Class<T> stateClass) {
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
     * This calculates the number of items in the group matching the
     * regular expression passed in parameter
     * Group:Number:COUNT(".") will count all items having a string state of one character
     * Group:Number:COUNT("[5-9]") will count all items having a string state between 5 and 9
     * ...
     */
    static class Count implements GroupFunction {

        protected final Pattern pattern;

        public Count(State regExpr) {
            if (regExpr == null) {
                throw new IllegalArgumentException("Parameter must not be null!");
            }
            this.pattern = Pattern.compile(regExpr.toString());
        }

        @Override
        public State calculate(Set<Item> items) {
            int count = 0;
            if (items != null) {
                for (Item item : items) {
                    Matcher matcher = pattern.matcher(item.getState().toString());
                    if (matcher.matches()) {
                        count++;
                    }
                }
            }

            return new DecimalType(count);
        }

        @Override
        public <T extends State> T getStateAs(Set<Item> items, Class<T> stateClass) {
            State state = calculate(items);
            if (stateClass.isInstance(state)) {
                return stateClass.cast(state);
            } else {
                return null;
            }
        }

        @Override
        public State[] getParameters() {
            return new State[] { new StringType(pattern.pattern()) };
        }
    }

}
