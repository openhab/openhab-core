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
import org.eclipse.jdt.annotation.Nullable;

/**
 * A {@link StateDescriptionFragment} will deliver only the parts of a {@link StateDescription} it knows of.
 * All other methods should return {@code null} to indicate an unknown value.
 *
 * @author Henning Treu - initial contribution and API.
 *
 */
@NonNullByDefault
public interface StateDescriptionFragment {

    /**
     * Returns the minimum value of an item state.
     *
     * @return minimum value of an item state
     */
    @Nullable
    BigDecimal getMinimum();

    /**
     * Returns the maximum value of an item state.
     *
     * @return maximum value of an item state
     */
    @Nullable
    BigDecimal getMaximum();

    /**
     * Returns the step size.
     *
     * @return step size
     */
    @Nullable
    BigDecimal getStep();

    /**
     * Returns the pattern to render the state to a string.
     *
     * @return pattern
     */
    @Nullable
    String getPattern();

    /**
     * Returns true, if the state can only be read but not written. Typically a
     * sensor can be read only.
     *
     * @return true, if the state can only be read but not written
     */
    @Nullable
    Boolean isReadOnly();

    /**
     * Returns a list of predefined states with their label.
     *
     * @return a list of predefined states with their label
     */
    @Nullable
    List<StateOption> getOptions();

    /**
     * Create and return a {@link StateDescription} from this fragment. The resulting {@link StateDescription} should be
     * null if the fragment does not define any values.
     *
     * @return a {@link StateDescription} from this fragment.
     */
    @Nullable
    StateDescription toStateDescription();

}
