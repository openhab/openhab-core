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
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.internal.types.StateDescriptionFragmentImpl;

/**
 * Builds a {@link StateDescriptionFragment} with the relevant parts only.
 *
 * @author Henning Treu - initial contribution and API.
 *
 */
@NonNullByDefault
public class StateDescriptionFragmentBuilder {

    private final StateDescriptionFragmentImpl fragment;

    private StateDescriptionFragmentBuilder() {
        fragment = new StateDescriptionFragmentImpl();
    }

    private StateDescriptionFragmentBuilder(StateDescription legacy) {
        fragment = new StateDescriptionFragmentImpl(legacy);
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
     * Create a builder instance and initialise all fields from the given {@link StateDescription}.
     * Note: State options will only be taken into account if the list is not empty.
     *
     * @param legacy the {@link StateDescription} this builder be initialised from.
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
    public StateDescriptionFragment build() {
        return new StateDescriptionFragmentImpl(fragment);
    }

    /**
     * Set the maximum for the resulting {@link StateDescriptionFragment}.
     *
     * @param maximum the maximum for the resulting {@link StateDescriptionFragment}.
     * @return this builder.
     */
    public StateDescriptionFragmentBuilder withMaximum(BigDecimal maximum) {
        fragment.setMaximum(maximum);
        return this;
    }

    /**
     * Set the minimum for the resulting {@link StateDescriptionFragment}.
     *
     * @param minimum the minimum for the resulting {@link StateDescriptionFragment}.
     * @return this builder.
     */
    public StateDescriptionFragmentBuilder withMinimum(BigDecimal minimum) {
        fragment.setMinimum(minimum);
        return this;
    }

    /**
     * Set the step for the resulting {@link StateDescriptionFragment}.
     *
     * @param step the step for the resulting {@link StateDescriptionFragment}.
     * @return this builder.
     */
    public StateDescriptionFragmentBuilder withStep(BigDecimal step) {
        fragment.setStep(step);
        return this;
    }

    /**
     * Set the pattern for the resulting {@link StateDescriptionFragment}.
     *
     * @param pattern the pattern for the resulting {@link StateDescriptionFragment}.
     * @return this builder.
     */
    public StateDescriptionFragmentBuilder withPattern(String pattern) {
        fragment.setPattern(pattern);
        return this;
    }

    /**
     * Set readOnly for the resulting {@link StateDescriptionFragment}.
     *
     * @param readOnly readOnly for the resulting {@link StateDescriptionFragment}.
     * @return this builder.
     */
    public StateDescriptionFragmentBuilder withReadOnly(Boolean readOnly) {
        fragment.setReadOnly(readOnly);
        return this;
    }

    /**
     * Set the {@link StateOption}s for the resulting {@link StateDescriptionFragment}.
     *
     * @param options the {@link StateOption}s for the resulting {@link StateDescriptionFragment}.
     * @return this builder.
     */
    public StateDescriptionFragmentBuilder withOptions(List<StateOption> options) {
        fragment.setOptions(options);
        return this;
    }

}
