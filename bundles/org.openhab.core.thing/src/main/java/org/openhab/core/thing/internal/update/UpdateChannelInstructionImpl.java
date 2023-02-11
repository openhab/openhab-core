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
package org.openhab.core.thing.internal.update;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.internal.update.dto.AddChannel;
import org.openhab.core.thing.internal.update.dto.UpdateChannel;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * The {@link UpdateChannelInstructionImpl} implements a {@link ThingUpdateInstruction} that updates a channel of a
 * thing.
 * <p />
 * Parameters are:
 * <ul>
 * <li>channelId - the id of the channel</li>
 * <li>channelTypeUID - the {@link ChannelTypeUID} of the channel</li>
 * <li>label - the (optional) label for the channel</li>
 * <li>description - the (optional) description for the channel</li>
 * </ul>
 * <p />
 * If optional parameters are not given, they are inherited from the channel-type
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class UpdateChannelInstructionImpl implements ThingUpdateInstruction {
    private final int thingTypeVersion;
    private final boolean addOnly;
    private final String channelId;
    private final String channelTypeUid;
    private final @Nullable String label;
    private final @Nullable String description;

    UpdateChannelInstructionImpl(int thingTypeVersion, UpdateChannel updateChannel) {
        this.thingTypeVersion = thingTypeVersion;
        this.channelId = updateChannel.getId();
        this.channelTypeUid = updateChannel.getChannelTypeUid();
        this.label = updateChannel.getLabel();
        this.description = updateChannel.getDescription();
        this.addOnly = false;
    }

    UpdateChannelInstructionImpl(int thingTypeVersion, AddChannel addChannel) {
        this.thingTypeVersion = thingTypeVersion;
        this.channelId = addChannel.getId();
        this.channelTypeUid = addChannel.getChannelTypeUid();
        this.label = addChannel.getLabel();
        this.description = addChannel.getDescription();
        this.addOnly = true;
    }

    @Override
    public int getThingTypeVersion() {
        return thingTypeVersion;
    }

    @Override
    public void perform(Thing thing, ThingBuilder thingBuilder) {
        ChannelUID affectedChannelUid = new ChannelUID(thing.getUID(), channelId);
        Configuration channelConfiguration = new Configuration();

        if (!addOnly) {
            // if we update the channel, preserve the configuration
            Channel oldChannel = thing.getChannel(affectedChannelUid);
            if (oldChannel != null) {
                channelConfiguration = oldChannel.getConfiguration();
            }

            thingBuilder.withoutChannel(affectedChannelUid);
        }

        ChannelBuilder channelBuilder = ChannelBuilder.create(affectedChannelUid)
                .withType(new ChannelTypeUID(channelTypeUid)).withConfiguration(channelConfiguration);

        if (label != null) {
            // label is optional (could be inherited from thing-type)
            channelBuilder.withLabel(Objects.requireNonNull(label));
        }
        if (description != null) {
            // description is optional (could be inherited from thing-type)
            channelBuilder.withDescription(Objects.requireNonNull(description));
        }
        thingBuilder.withChannel(channelBuilder.build());
    }
}
