/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateOption;
import org.openhab.core.types.util.UnitUtils;

/**
 * Data holder for StateDescriptionFragment creation.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public class StateDescriptionFragmentImpl implements StateDescriptionFragment {
    private static final Pattern PATTERN_PRECISION_PATTERN = Pattern.compile("%[-#+ ,\\(<0-9$]*.(\\d+)[eEf]");
    private static final String INTEGER_FORMAT_TYPE = "d";

    private static class StateDescriptionImpl extends StateDescription {
        StateDescriptionImpl(@Nullable BigDecimal minimum, @Nullable BigDecimal maximum, @Nullable BigDecimal step,
                @Nullable String pattern, boolean readOnly, @Nullable List<StateOption> options) {
            super(minimum, maximum, step, pattern, readOnly, options);
        }
    }

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
        this.options = options == null || options.isEmpty() ? List.of() : Collections.unmodifiableList(options);
    }

    /**
     * Create a {@link StateDescriptionFragmentImpl} and initialize from the given {@link StateDescription}.
     * Note: State options will only be set if not empty.
     *
     * @param legacy the {@link StateDescription} to initialize from.
     * @deprecated use {@link StateDescriptionFragmentBuilder} instead.
     */
    @Deprecated
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

    @Override
    public @Nullable StateDescription toStateDescription() {
        if (minimum == null && maximum == null && step == null && readOnly == null && pattern == null
                && options == null) {
            return null;
        }
        final Boolean ro = readOnly;
        return new StateDescriptionImpl(minimum, maximum, step, pattern, ro != null && ro.booleanValue(), options);
    }

    /**
     * Merge the given {@link StateDescriptionFragment}. Set all unset ({@code null}) fields of this instance to the
     * values from the given {@link StateDescriptionFragment}.
     *
     * @param fragment a {@link StateDescriptionFragment} this instance should merge in.
     * @return this instance with the fields merged.
     */
    public StateDescriptionFragment merge(StateDescriptionFragment fragment) {
        String newPattern = fragment.getPattern();
        // Do unit conversions if possible.
        // Example:
        // The GenericItemProvider sets a pattern of ° F, but no min, max, or step.
        // The ChannelStateDescriptionProvider sets a pattern of ° C, min of 0, max of 100, step of 0.5
        // The latter is lower priority, so gets merged into the former.
        // We want to construct a final description with a pattern of ° F, min of 32, max of 212, and step of 0.9
        //
        // In other words, we keep the user's overridden unit, but convert the bounds provided by the
        // channel (that is describing the bounds in terms of its unit) to the user's preferred unit.
        boolean skipStep = false;
        if (pattern != null && newPattern != null) {
            Unit<?> oldUnit = UnitUtils.parseUnit(pattern);
            Unit<?> newUnit = UnitUtils.parseUnit(newPattern);
            if (oldUnit != null && newUnit != null && !oldUnit.equals(newUnit)
                    && (oldUnit.isCompatible(newUnit) || oldUnit.inverse().isCompatible(newUnit))) {
                BigDecimal newValue;
                // when inverting, min and max will swap
                if (oldUnit.inverse().isCompatible(newUnit)) {
                    // It's highly likely that an invertible unit conversion will end up with a very long decimal
                    // So use the format to round min/max to what we're going to display.
                    Integer scale = null;
                    var m = PATTERN_PRECISION_PATTERN.matcher(pattern);
                    if (m.find()) {
                        String precision = m.group(1);
                        if (precision != null) {
                            scale = Integer.valueOf(precision);
                        } else if (m.group(2).equals(INTEGER_FORMAT_TYPE)) {
                            scale = 0;
                        }
                    }

                    if (minimum == null && (newValue = fragment.getMaximum()) != null) {
                        minimum = new QuantityType(newValue, newUnit).toInvertibleUnit(oldUnit).toBigDecimal();
                        if (minimum.scale() > 0) {
                            minimum = minimum.stripTrailingZeros();
                        }
                        if (scale != null && minimum.scale() > scale) {
                            minimum = minimum.setScale(scale, RoundingMode.FLOOR);
                        }
                    }
                    if (maximum == null && (newValue = fragment.getMinimum()) != null) {
                        maximum = new QuantityType(newValue, newUnit).toInvertibleUnit(oldUnit).toBigDecimal();
                        if (maximum.scale() > 0) {
                            maximum = maximum.stripTrailingZeros();
                        }
                        if (scale != null && maximum.scale() > scale) {
                            maximum = maximum.setScale(scale, RoundingMode.CEILING);
                        }
                    }

                    // Invertible units cannot have a linear relationship, so just leave step blank.
                    // Make sure it doesn't get overwritten below with a non-sensical value
                    skipStep = true;
                } else {
                    if (minimum == null && (newValue = fragment.getMinimum()) != null) {
                        minimum = new QuantityType(newValue, newUnit).toInvertibleUnit(oldUnit).toBigDecimal();
                        if (minimum.scale() > 0) {
                            minimum = minimum.stripTrailingZeros();
                        }
                    }
                    if (maximum == null && (newValue = fragment.getMaximum()) != null) {
                        maximum = new QuantityType(newValue, newUnit).toInvertibleUnit(oldUnit).toBigDecimal();
                        if (maximum.scale() > 0) {
                            maximum = maximum.stripTrailingZeros();
                        }
                    }
                    if (step == null && (newValue = fragment.getStep()) != null) {
                        step = new QuantityType(newValue, newUnit).toUnitRelative(oldUnit).toBigDecimal();
                        if (step.scale() > 0) {
                            step = step.stripTrailingZeros();
                        }
                    }
                }
            }
        }

        if (minimum == null) {
            minimum = fragment.getMinimum();
        }
        if (maximum == null) {
            maximum = fragment.getMaximum();
        }
        if (step == null && !skipStep) {
            step = fragment.getStep();
        }
        if (pattern == null) {
            pattern = fragment.getPattern();
        }
        if (readOnly == null) {
            readOnly = fragment.isReadOnly();
        }
        if (options == null || options.isEmpty()) {
            options = fragment.getOptions();
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
                && (readOnly != null ? readOnly.equals(other.readOnly) : other.readOnly == null)
                && (options != null ? options.equals(other.options) : other.options == null);
    }

    @Override
    public String toString() {
        return "StateDescription [minimum=" + minimum + ", maximum=" + maximum + ", step=" + step + ", pattern="
                + pattern + ", readOnly=" + readOnly + ", channelStateOptions=" + options + "]";
    }
}
