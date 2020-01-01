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
package org.openhab.core.types;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link StateDescription} describes restrictions of an item state and gives information how to interpret
 * it.
 *
 * @author Dennis Nobel - Initial contribution
 */
@NonNullByDefault
public class StateDescription {

    protected @Nullable final BigDecimal minimum;
    protected @Nullable final BigDecimal maximum;
    protected @Nullable final BigDecimal step;
    protected @Nullable final String pattern;
    protected final boolean readOnly;
    protected final List<StateOption> options;

    /**
     * Creates a state description object.
     *
     * @param minimum minimum value of the state
     * @param maximum maximum value of the state
     * @param step step size
     * @param pattern pattern to render the state
     * @param readOnly if the state can be changed by the system
     * @param options predefined list of options
     * @deprecated use {@link StateDescriptionFragmentBuilder} instead.
     */
    @Deprecated
    public StateDescription(@Nullable BigDecimal minimum, @Nullable BigDecimal maximum, @Nullable BigDecimal step,
            @Nullable String pattern, boolean readOnly, @Nullable List<StateOption> options) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
        this.pattern = pattern;
        this.readOnly = readOnly;
        this.options = options == null ? Collections.emptyList() : Collections.unmodifiableList(options);
    }

    /**
     * Returns the minimum value of an item state.
     *
     * @return minimum value of an item state
     */
    public @Nullable BigDecimal getMinimum() {
        return minimum;
    }

    /**
     * Returns the maximum value of an item state.
     *
     * @return maximum value of an item state
     */
    public @Nullable BigDecimal getMaximum() {
        return maximum;
    }

    /**
     * Returns the step size.
     *
     * @return step size
     */
    public @Nullable BigDecimal getStep() {
        return step;
    }

    /**
     * Returns the pattern to render the state to a string.
     *
     * @return pattern
     */
    public @Nullable String getPattern() {
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
