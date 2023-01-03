/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.thing.internal.type;

import java.net.URI;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.TriggerChannelTypeBuilder;
import org.openhab.core.types.EventDescription;

/**
 * Implementation of {@link TriggerChannelTypeBuilder} to build {@link ChannelType}s of kind TRIGGER
 *
 * @author Stefan Triller - Initial contribution
 */
@NonNullByDefault
public class TriggerChannelTypeBuilderImpl extends AbstractChannelTypeBuilder<TriggerChannelTypeBuilder>
        implements TriggerChannelTypeBuilder {

    private class TriggerChannelTypeImpl extends ChannelType {
        TriggerChannelTypeImpl(ChannelTypeUID uid, boolean advanced, String label, @Nullable String description,
                @Nullable String category, @Nullable Set<String> tags, @Nullable EventDescription event,
                @Nullable URI configDescriptionURI) throws IllegalArgumentException {
            super(uid, advanced, null, ChannelKind.TRIGGER, label, description, category, tags, null, null, event,
                    configDescriptionURI, null);
        }
    }

    private @Nullable EventDescription eventDescription;

    public TriggerChannelTypeBuilderImpl(ChannelTypeUID channelTypeUID, String label) {
        super(channelTypeUID, label);
    }

    @Override
    public TriggerChannelTypeBuilder withEventDescription(EventDescription eventDescription) {
        this.eventDescription = eventDescription;
        return this;
    }

    @Override
    public ChannelType build() {
        return new TriggerChannelTypeImpl(channelTypeUID, advanced, label, description, category, tags,
                eventDescription, configDescriptionURI);
    }
}
