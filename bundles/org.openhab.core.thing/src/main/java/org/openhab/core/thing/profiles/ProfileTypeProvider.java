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
package org.openhab.core.thing.profiles;

import java.util.Collection;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A {@link ProfileTypeProvider} is responsible for providing {@link ProfileType}s.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public interface ProfileTypeProvider {

    /**
     * Returns all profile types for the given {@link Locale}.
     *
     * @param locale (can be null)
     * @return all profile types or empty list if no profile type exists
     */
    Collection<ProfileType> getProfileTypes(@Nullable Locale locale);
}
