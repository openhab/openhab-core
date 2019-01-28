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
package org.eclipse.smarthome.core.thing.profiles;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Implementors are capable of creating a {@link Profile} instances.
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
@NonNullByDefault
public interface ProfileFactory {

    /**
     * Create a {@link Profile} instance for the given profile tye ID.
     *
     * @param profileTypeUID the profile identifier
     * @param callback the ProfileCallback instance to be used by the {@link Profile} instance
     * @param profileContext giving access to the profile's context like configuration, scheduler, etc.
     * @return the profile instance or {@code null} if this factory cannot handle the given link
     */
    @Nullable
    Profile createProfile(ProfileTypeUID profileTypeUID, ProfileCallback callback, ProfileContext profileContext);

    /**
     * Return the identifiers of all supported profile types
     *
     * @return a collection of all profile type identifier which this class is capable of creating
     */
    Collection<ProfileTypeUID> getSupportedProfileTypeUIDs();

}
