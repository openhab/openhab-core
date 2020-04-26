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

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Provide a {@link StateDescriptionFragment} for the current {@link StateDescription}. Use the
 * {@link StateDescriptionFragmentBuilder} to create a {@link StateDescriptionFragment} with only the parts known.
 *
 * @author Henning Treu - Initial contribution
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
     * Return the service rank.
     *
     * Usually an implementation should piggy-back on the <code>service.ranking</code> OSGi component property.
     * The default is 0 - the higher, the more like it is going to win.
     *
     * @return an integer value
     */
    Integer getRank();
}
