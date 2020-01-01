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

/**
 * A {@link StateDescriptionProvider} provides localized {@link StateDescription}s for items.
 *
 * @author Dennis Nobel - Initial contribution
 *
 * @deprecated Use {@link StateDescriptionFragmentProvider} instead and provide only the known fields.
 */
@Deprecated
public interface StateDescriptionProvider {

    /**
     * Returns the state description for an item name
     *
     * @param itemName item name (must not be null)
     * @param locale locale (can be null)
     * @return state description or null if no state description could be found
     */
    StateDescription getStateDescription(String itemName, Locale locale);

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
