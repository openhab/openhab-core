/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
import org.openhab.core.common.registry.Identifiable;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * Describes a profile type.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public interface ProfileType extends Identifiable<ProfileTypeUID> {

    /**
     * Get a collection of ItemType names that this profile type supports
     *
     * @return a collection of item types (may be empty if all are supported)
     */
    Collection<String> getSupportedItemTypes();

    /**
     * Get a collection of ItemType names that a Channel needs to support in order to able to use this ProfileType
     *
     * @return a collection of supported ItemType names (an empty list means ALL types are supported)
     */
    Collection<String> getSupportedItemTypesOfChannel();

    /**
     * Get a collection of ChannelType UIDs that this profile type supports
     *
     * @return a collection of ChannelTypeUIDs (may be empty if all are supported).
     */
    Collection<ChannelTypeUID> getSupportedChannelTypeUIDs();

    /**
     * Get the ChannelKind this profile type supports.
     *
     * @return The supported ChannelKind for this profile type. If null then all channel kinds are supported
     */
    @Nullable
    ChannelKind getSupportedChannelKind();

    /**
     * Get a human readable description.
     *
     * @return the label
     */
    String getLabel();
}
