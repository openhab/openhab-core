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
package org.openhab.core.internal.types;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateOption;

/**
 * Data holder for StateDescriptionFragment creation.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public class StateDescriptionFragmentImpl implements StateDescriptionFragment {

    private @Nullable BigDecimal minimum;
    private @Nullable BigDecimal maximum;
    private @Nullable BigDecimal step;
    private @Nullable String pattern;
    private @Nullable Boolean readOnly;
    private @Nullable List<StateOption> options;

    /**
     * Create an empty {@link StateDescriptionFragmentImpl}.
     */
    public StateDescriptionFragmentImpl() {
        //
    }

    /**
     * Create a {@link StateDescriptionFragmentImpl} and initialize from the given values.
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
    public StateDescriptionFragmentImpl(@Nullable BigDecimal minimum, @Nullable BigDecimal maximum,
            @Nullable BigDecimal step, @Nullable String pattern, @Nullable Boolean readOnly,
            @Nullable List<StateOption> options) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
        this.pattern = pattern;
        this.readOnly = readOnly;
        this.options = options == null ? Collections.emptyList() : Collections.unmodifiableList(options);
    }

    /**
     * Create a {@link StateDescriptionFragmentImpl} and initialize from the given {@link StateDescription}.
     * Note: State options will only be set if not empty.
     *
     * @param legacy the {@link StateDescription} to initialize from.
     */
    public StateDescriptionFragmentImpl(StateDescription legacy) {
        this.minimum = legacy.getMinimum();
        this.maximum = legacy.getMaximum();
        this.step = legacy.getStep();
        this.pattern = legacy.getPattern();
        this.readOnly = Boolean.valueOf(legacy.isReadOnly());
        if (!legacy.getOptions().isEmpty()) {
            this.options = legacy.getOptions();
        }
    }

    /**
     * Copy constructor.
     *
     * @param source the source to copy from.
     */
    public StateDescriptionFragmentImpl(StateDescriptionFragmentImpl source) {
        this.minimum = source.getMinimum();
        this.maximum = source.getMaximum();
        this.step = source.getStep();
        this.pattern = source.getPattern();
        this.readOnly = source.isReadOnly();
        this.options = source.getOptions();
    }

    @Override
    public @Nullable BigDecimal getMinimum() {
        return minimum;
    }

    public void setMinimum(BigDecimal minimum) {
        this.minimum = minimum;
    }

    @Override
    public @Nullable BigDecimal getMaximum() {
        return maximum;
    }

    public void setMaximum(BigDecimal maximum) {
        this.maximum = maximum;
    }

    @Override
    public @Nullable BigDecimal getStep() {
        return step;
    }

    public void setStep(BigDecimal step) {
        this.step = step;
    }

    @Override
    public @Nullable String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public @Nullable Boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public @Nullable List<StateOption> getOptions() {
        return options;
    }

    public void setOptions(List<StateOption> options) {
        this.options = options;
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nullable StateDescription toStateDescription() {
        if (minimum == null && maximum == null && step == null && readOnly == null && pattern == null
                && options == null) {
            return null;
        }
        final Boolean ro = readOnly;
        return new StateDescription(minimum, maximum, step, pattern, ro == null ? false : ro.booleanValue(), options);
    }

    /**
     * Merge the given {@link StateDescriptionFragment}. Set all unset ({@code null}) fields of this instance to the
     * values from the given {@link StateDescriptionFragment}.
     *
     * @param fragment a {@link StateDescriptionFragment} this instance should merge in.
     * @return this instance with the fields merged.
     */
    public StateDescriptionFragment merge(StateDescriptionFragment fragment) {
        if (this.minimum == null) {
            this.minimum = fragment.getMinimum();
        }
        if (this.maximum == null) {
            this.maximum = fragment.getMaximum();
        }
        if (this.step == null) {
            this.step = fragment.getStep();
        }
        if (this.pattern == null) {
            this.pattern = fragment.getPattern();
        }
        if (this.readOnly == null) {
            this.readOnly = fragment.isReadOnly();
        }
        if (this.options == null || this.options.isEmpty()) {
            this.options = fragment.getOptions();
        }
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (minimum != null ? minimum.hashCode() : 0);
        result = prime * result + (maximum != null ? maximum.hashCode() : 0);
        result = prime * result + (step != null ? step.hashCode() : 0);
        result = prime * result + (pattern != null ? pattern.hashCode() : 0);
        result = prime * result + (readOnly ? 1231 : 1237);
        result = prime * result + (options != null ? options.hashCode() : 0);
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
        StateDescriptionFragmentImpl other = (StateDescriptionFragmentImpl) obj;
        return (minimum != null ? minimum.equals(other.minimum) : other.minimum == null)
                && (maximum != null ? maximum.equals(other.maximum) : other.maximum == null)
                && (step != null ? step.equals(other.step) : other.step == null)
                && (pattern != null ? pattern.equals(other.pattern) : other.pattern == null)
                && readOnly == other.readOnly //
                && (options != null ? options.equals(other.options) : other.options == null);
    }

    @Override
    public String toString() {
        return "StateDescription [minimum=" + minimum + ", maximum=" + maximum + ", step=" + step + ", pattern="
                + pattern + ", readOnly=" + readOnly + ", channelStateOptions=" + options + "]";
    }
}
