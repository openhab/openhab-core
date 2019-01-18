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

/**
 * Describes a {@link StateProfile} type.
 *
 * @author Simon Kaufmann - initial contribution and API.
 * @author Stefan Triller - added getSupportedItemTypesOfChannel method
 *
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
