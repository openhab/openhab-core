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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Implementors are capable of creating {@link Profile} instances.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public interface ProfileFactory {

    /**
     * Creates a {@link Profile} instance for the given profile type identifier.
     *
     * @param profileTypeUID the profile type identifier
     * @param callback the {@link ProfileCallback} instance to be used by the {@link Profile} instance
     * @param profileContext giving access to the profile's context like configuration, scheduler, etc.
     * @return the profile instance or {@code null} if this factory cannot handle the given link
     */
    @Nullable
    Profile createProfile(ProfileTypeUID profileTypeUID, ProfileCallback callback, ProfileContext profileContext);

    /**
     * Return the identifiers of all supported profile types.
     *
     * @return a collection of all profile type identifier which this class is capable of creating
     */
    Collection<ProfileTypeUID> getSupportedProfileTypeUIDs();

}
