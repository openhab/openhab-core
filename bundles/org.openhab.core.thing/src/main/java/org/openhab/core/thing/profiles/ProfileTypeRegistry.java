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

import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link ProfileTypeRegistry} allows access to the {@link ProfileType}s provided by all
 * {@link ProfileTypeProvider}s.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public interface ProfileTypeRegistry {

    /**
     * Get the available {@link ProfileType}s from all providers using the default locale.
     *
     * @return all profile types
     */
    public List<ProfileType> getProfileTypes();

    /**
     * Get the available {@link ProfileType}s from all providers.
     *
     * @param locale the language to use (may be null)
     * @return all profile types
     */
    public List<ProfileType> getProfileTypes(@Nullable Locale locale);
}
