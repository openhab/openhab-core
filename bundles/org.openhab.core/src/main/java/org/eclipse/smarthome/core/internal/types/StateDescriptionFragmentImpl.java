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
package org.eclipse.smarthome.core.internal.types;

import java.math.BigDecimal;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateDescriptionFragment;
import org.eclipse.smarthome.core.types.StateOption;

/**
 * Data holder for StateDescriptionFragment creation.
 *
 * @author Henning Treu - initial contribution
 *
 */
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
     * Create a {@link StateDescriptionFragmentImpl} and initialise from the given {@link StateDescription}.
     * Note: State options will only be set if not empty.
     *
     * @param legacy the {@link StateDescription} to initialise from.
     */
    public StateDescriptionFragmentImpl(StateDescription legacy) {
        this.minimum = legacy.getMinimum();
        this.maximum = legacy.getMaximum();
        this.step = legacy.getStep();
        this.pattern = legacy.getPattern();
        this.readOnly = Boolean.valueOf(legacy.isReadOnly());
        if (legacy.getOptions() != null && !legacy.getOptions().isEmpty()) {
            this.options = legacy.getOptions();
        }
    }

    /**
     * Copy constructor.
     *
     * @param source the source to copy from.
     */
    public StateDescriptionFragmentImpl(@NonNull StateDescriptionFragmentImpl source) {
        this.minimum = source.getMinimum();
        this.maximum = source.getMaximum();
        this.step = source.getStep();
        this.pattern = source.getPattern();
        this.readOnly = source.isReadOnly();
        this.options = source.getOptions();
    }

    @Override
    public BigDecimal getMinimum() {
        return minimum;
    }

    public void setMinimum(BigDecimal minimum) {
        this.minimum = minimum;
    }

    @Override
    public BigDecimal getMaximum() {
        return maximum;
    }

    public void setMaximum(BigDecimal maximum) {
        this.maximum = maximum;
    }

    @Override
    public BigDecimal getStep() {
        return step;
    }

    public void setStep(BigDecimal step) {
        this.step = step;
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public Boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public List<StateOption> getOptions() {
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
        if (this.options == null) {
            this.options = fragment.getOptions();
        }

        return this;
    }

}
