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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.internal.types.StateDescriptionFragmentImpl;

/**
 * Builds a {@link StateDescriptionFragment} with the relevant parts only.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public class StateDescriptionFragmentBuilder {

    private @Nullable BigDecimal minimum;
    private @Nullable BigDecimal maximum;
    private @Nullable BigDecimal step;
    private @Nullable String pattern;
    private @Nullable Boolean readOnly;
    private @Nullable List<StateOption> options;

    private StateDescriptionFragmentBuilder() {
        //
    }

    private StateDescriptionFragmentBuilder(StateDescription legacy) {
        this.minimum = legacy.getMinimum();
        this.maximum = legacy.getMaximum();
        this.step = legacy.getStep();
        this.pattern = legacy.getPattern();
        this.readOnly = Boolean.valueOf(legacy.isReadOnly());
        if (!legacy.getOptions().isEmpty()) {
            this.options = new ArrayList<>(legacy.getOptions());
        }
    }

    /**
     * Create and return a fresh builder instance.
     *
     * @return a fresh {@link StateDescriptionFragmentBuilder} instance.
     */
    public static StateDescriptionFragmentBuilder create() {
        return new StateDescriptionFragmentBuilder();
    }

    /**
     * Create a builder instance and initialize all fields from the given {@link StateDescription}.
     * Note: State options will only be taken into account if the list is not empty.
     *
     * @param legacy the {@link StateDescription} this builder be initialized from.
     * @return the builder.
     */
    public static StateDescriptionFragmentBuilder create(StateDescription legacy) {
        return new StateDescriptionFragmentBuilder(legacy);
    }

    /**
     * Build a {@link StateDescriptionFragment} from the values of this builder.
     *
     * @return a {@link StateDescriptionFragment} from the values of this builder.
     */
    @SuppressWarnings("deprecation")
    public StateDescriptionFragment build() {
        return new StateDescriptionFragmentImpl(minimum, maximum, step, pattern, readOnly, options);
    }

    /**
     * Set the maximum for the resulting {@link StateDescriptionFragment}.
     *
     * @param maximum the maximum for the resulting {@link StateDescriptionFragment}.
     * @return this builder.
     */
    public StateDescriptionFragmentBuilder withMaximum(BigDecimal maximum) {
        this.maximum = maximum;
        return this;
    }

    /**
     * Set the minimum for the resulting {@link StateDescriptionFragment}.
     *
     * @param minimum the minimum for the resulting {@link StateDescriptionFragment}.
     * @return this builder.
     */
    public StateDescriptionFragmentBuilder withMinimum(BigDecimal minimum) {
        this.minimum = minimum;
        return this;
    }

    /**
     * Set the step for the resulting {@link StateDescriptionFragment}.
     *
     * @param step the step for the resulting {@link StateDescriptionFragment}.
     * @return this builder.
     */
    public StateDescriptionFragmentBuilder withStep(BigDecimal step) {
        this.step = step;
        return this;
    }

    /**
     * Set the pattern for the resulting {@link StateDescriptionFragment}.
     *
     * @param pattern the pattern for the resulting {@link StateDescriptionFragment}.
     * @return this builder.
     */
    public StateDescriptionFragmentBuilder withPattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    /**
     * Set readOnly for the resulting {@link StateDescriptionFragment}.
     *
     * @param readOnly readOnly for the resulting {@link StateDescriptionFragment}.
     * @return this builder.
     */
    public StateDescriptionFragmentBuilder withReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    /**
     * Ass a {@link StateOption} for the resulting {@link StateDescriptionFragment}.
     *
     * @param option a {@link StateOption} for the resulting {@link StateDescriptionFragment}.
     * @return this builder.
     */
    @SuppressWarnings("null")
    public StateDescriptionFragmentBuilder withOption(StateOption option) {
        if (options == null) {
            options = new ArrayList<>();
        }
        options.add(option);
        return this;
    }

    /**
     * Set the {@link StateOption}s for the resulting {@link StateDescriptionFragment}.
     *
     * @param options the {@link StateOption}s for the resulting {@link StateDescriptionFragment}.
     * @return this builder.
     */
    public StateDescriptionFragmentBuilder withOptions(List<StateOption> options) {
        this.options = options;
        return this;
    }
}
