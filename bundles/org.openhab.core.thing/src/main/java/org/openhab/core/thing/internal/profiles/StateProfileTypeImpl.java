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
package org.openhab.core.thing.internal.profiles;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfileType;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * Default implementation of a {@link StateProfileType}.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public class StateProfileTypeImpl implements StateProfileType {

    private final ProfileTypeUID profileTypeUID;
    private final String label;
    private final Collection<String> supportedItemTypes;
    private final Collection<String> supportedItemTypesOfChannel;
    private final Collection<ChannelTypeUID> supportedChannelTypeUIDs;

    public StateProfileTypeImpl(ProfileTypeUID profileTypeUID, String label, Collection<String> supportedItemTypes,
            Collection<String> supportedItemTypesOfChannel, Collection<ChannelTypeUID> supportedChannelTypeUIDs) {
        this.profileTypeUID = profileTypeUID;
        this.label = label;
        this.supportedItemTypes = Collections.unmodifiableCollection(supportedItemTypes);
        this.supportedItemTypesOfChannel = Collections.unmodifiableCollection(supportedItemTypesOfChannel);
        this.supportedChannelTypeUIDs = Collections.unmodifiableCollection(supportedChannelTypeUIDs);
    }

    @Override
    public ProfileTypeUID getUID() {
        return profileTypeUID;
    }

    @Override
    public Collection<ChannelTypeUID> getSupportedChannelTypeUIDs() {
        return supportedChannelTypeUIDs;
    }

    @Override
    public Collection<String> getSupportedItemTypes() {
        return supportedItemTypes;
    }

    @Override
    @Nullable
    public ChannelKind getSupportedChannelKind() {
        return ChannelKind.STATE;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public Collection<String> getSupportedItemTypesOfChannel() {
        return supportedItemTypesOfChannel;
    }
}
