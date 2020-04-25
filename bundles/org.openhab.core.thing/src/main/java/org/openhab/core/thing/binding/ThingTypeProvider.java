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
package org.openhab.core.thing.binding;

import java.util.Collection;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.ThingType;

/**
 * The {@link ThingTypeProvider} is responsible for providing thing types.
 *
 * @author Dennis Nobel - Initial contribution
 */
@NonNullByDefault
public interface ThingTypeProvider {

    /**
     * Provides a collection of thing types
     *
     * @param locale locale (can be null)
     * @return the thing types provided by the {@link ThingTypeProvider}
     */
    Collection<ThingType> getThingTypes(@Nullable Locale locale);

    /**
     * Provides a thing type for the given UID or null if no type for the
     * given UID exists.
     *
     * @param locale locale (can be null)
     * @return thing type for the given UID or null if no type for the given
     *         UID exists
     */
    @Nullable
    ThingType getThingType(ThingTypeUID thingTypeUID, @Nullable Locale locale);
}
