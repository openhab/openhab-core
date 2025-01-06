/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link StateDescription} describes restrictions of an item state and gives information how to interpret it.
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
     */
    protected StateDescription(@Nullable BigDecimal minimum, @Nullable BigDecimal maximum, @Nullable BigDecimal step,
            @Nullable String pattern, boolean readOnly, @Nullable List<StateOption> options) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
        this.pattern = pattern;
        this.readOnly = readOnly;
        this.options = options == null ? List.of() : Collections.unmodifiableList(options);
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Objects.hashCode(minimum);
        result = prime * result + Objects.hashCode(maximum);
        result = prime * result + Objects.hashCode(step);
        result = prime * result + Objects.hashCode(pattern);
        result = prime * result + (readOnly ? 1231 : 1237);
        result = prime * result + options.hashCode();
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        StateDescription other = (StateDescription) obj;
        return Objects.equals(minimum, other.minimum) && Objects.equals(maximum, other.maximum)
                && Objects.equals(step, other.step) && Objects.equals(pattern, other.pattern)
                && readOnly == other.readOnly //
                && options.equals(other.options);
    }

    @Override
    public String toString() {
        return "StateDescription [minimum=" + minimum + ", maximum=" + maximum + ", step=" + step + ", pattern="
                + pattern + ", readOnly=" + readOnly + ", channelStateOptions=" + options + "]";
    }
}
