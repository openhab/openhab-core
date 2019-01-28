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
package org.eclipse.smarthome.core.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Describes restrictions of an item state and gives information how to interpret
 * it.
 *
 * @author Dennis Nobel - Initial contribution
 */
public class StateDescription {

    private final BigDecimal minimum;
    private final BigDecimal maximum;
    private final BigDecimal step;
    private final String pattern;
    private final boolean readOnly;
    private final List<StateOption> options;

    /**
     * Creates a state description object.
     *
     * @param minimum minimum value of the state
     * @param maximum maximum value of the state
     * @param step step size
     * @param pattern pattern to render the state
     * @param readOnly if the state can be changed by the system
     * @param options predefined list of options
     */
    public StateDescription(BigDecimal minimum, BigDecimal maximum, BigDecimal step, String pattern, boolean readOnly,
            List<StateOption> options) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
        this.pattern = pattern;
        this.readOnly = readOnly;
        if (options != null) {
            this.options = Collections.unmodifiableList(new ArrayList<StateOption>(options));
        } else {
            this.options = Collections.emptyList();
        }
    }

    /**
     * Returns the minimum value of an item state.
     *
     * @return minimum value of an item state
     */
    public BigDecimal getMinimum() {
        return minimum;
    }

    /**
     * Returns the maximum value of an item state.
     *
     * @return maximum value of an item state
     */
    public BigDecimal getMaximum() {
        return maximum;
    }

    /**
     * Returns the step size.
     *
     * @return step size
     */
    public BigDecimal getStep() {
        return step;
    }

    /**
     * Returns the pattern to render the state to a string.
     *
     * @return pattern
     */
    public String getPattern() {
        return pattern;
    }

    /**
     *
     * Returns {@code true} if the state can only be read but not written or {@code false} if the state can also be
     * written.
     * Typically a sensor can only be read.
     *
     * @return {@code true} for readOnly, {@code false} otherwise.
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Returns a list of predefined states with their label.
     *
     * @return a list of predefined states with their label
     */
    public List<StateOption> getOptions() {
        return options;
    }

    @Override
    public String toString() {
        return "StateDescription [minimum=" + minimum + ", maximum=" + maximum + ", step=" + step + ", pattern="
                + pattern + ", readOnly=" + readOnly + ", channelStateOptions=" + options + "]";
    }

}
