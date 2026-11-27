/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Provide a {@link StateDescriptionFragment} for the current {@link StateDescription}. Use the
 * {@link StateDescriptionFragmentBuilder} to create a {@link StateDescriptionFragment} with only the parts known.
 *
 * @author Henning Treu - Initial contribution
 * @author Cody Cutrer - Default provider rank
 */
@NonNullByDefault
public interface StateDescriptionFragmentProvider {

    /**
     * Returns a {@link StateDescriptionFragment} with only the parts known by this
     * {@link StateDescriptionFragmentProvider}.
     *
     * @param itemName item name (must not be null)
     * @param locale locale (can be null)
     * @return a {@link StateDescriptionFragment} with only the parts known by this
     *         {@link StateDescriptionFragmentProvider}.
     */
    @Nullable
    StateDescriptionFragment getStateDescriptionFragment(String itemName, @Nullable Locale locale);

    /**
     * Returns the provider rank. Higher-ranked providers are queried first; providers with equal ranks retain their
     * registration order. Implementations may use the {@code service.ranking} OSGi component property.
     *
     * @return the provider rank, defaulting to zero
     */
    default Integer getRank() {
        return 0;
    }
}
