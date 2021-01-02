/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
import org.openhab.core.common.registry.Identifiable;

/**
 * Describes a profile type.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public interface ProfileType extends Identifiable<ProfileTypeUID> {

    /**
     *
     * @return a collection of item types (may be empty if all are supported)
     */
    Collection<String> getSupportedItemTypes();

    /**
     * Get a human readable description.
     *
     * @return the label
     */
    String getLabel();
}
