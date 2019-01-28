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
package org.eclipse.smarthome.core.thing.internal.profiles;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeUID;
import org.eclipse.smarthome.core.thing.profiles.TriggerProfileType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;

/**
 * Default implementation of a {@link TriggerProfileTypeImpl}.
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
@NonNullByDefault
public class TriggerProfileTypeImpl implements TriggerProfileType {

    private final ProfileTypeUID profileTypeUID;
    private final Collection<ChannelTypeUID> supportedChannelTypeUIDs;
    private final String label;
    private final Collection<String> supportedItemTypes;

    public TriggerProfileTypeImpl(ProfileTypeUID profileTypeUID, String label, Collection<String> supportedItemTypes,
            Collection<ChannelTypeUID> supportedChannelTypeUIDs) {
        this.profileTypeUID = profileTypeUID;
        this.label = label;
        this.supportedItemTypes = Collections.unmodifiableCollection(supportedItemTypes);
        this.supportedChannelTypeUIDs = Collections.unmodifiableCollection(supportedChannelTypeUIDs);
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
    public String getLabel() {
        return label;
    }

    @Override
    public ProfileTypeUID getUID() {
        return profileTypeUID;
    }

}
