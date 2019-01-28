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
package org.eclipse.smarthome.core.thing.profiles.dto;

import org.eclipse.smarthome.core.thing.profiles.ProfileType;
import org.eclipse.smarthome.core.thing.profiles.TriggerProfileType;

/**
 * The {@link ProfileTypeDTOMapper} is an utility class to map profile-types into profile-type data transfer
 * objects (DTOs).
 *
 * @author Stefan Triller - initial contribution
 *
 */
public class ProfileTypeDTOMapper {

    /**
     * Maps profile type into stripped profile type data transfer object.
     *
     * @param profileType the profile type to be mapped
     * @return the profile type DTO object
     */
    public static ProfileTypeDTO map(ProfileType profileType) {
        return new ProfileTypeDTO(profileType.getUID().toString(), profileType.getLabel(),
                profileType instanceof TriggerProfileType ? "TRIGGER" : "STATE", profileType.getSupportedItemTypes());
    }
}
