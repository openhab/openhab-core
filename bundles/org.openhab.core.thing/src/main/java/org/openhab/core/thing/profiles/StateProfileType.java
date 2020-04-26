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

/**
 * Describes a {@link StateProfile} type.
 *
 * @author Simon Kaufmann - Initial contribution
 * @author Stefan Triller - added getSupportedItemTypesOfChannel method
 */
@NonNullByDefault
public interface StateProfileType extends ProfileType {

    /**
     * Get a collection of ItemType names that a Channel needs to support in order to able to use this ProfileType
     *
     * @return a collection of supported ItemType names (an empty list means ALL types are supported)
     */
    Collection<String> getSupportedItemTypesOfChannel();
}
