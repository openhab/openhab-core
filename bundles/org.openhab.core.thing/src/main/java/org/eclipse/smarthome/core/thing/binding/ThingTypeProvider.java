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
package org.eclipse.smarthome.core.thing.binding;

import java.util.Collection;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.type.ThingType;

/**
 * The {@link ThingTypeProvider} is responsible for providing thing types.
 *
 * @author Dennis Nobel
 *
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
