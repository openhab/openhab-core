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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
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
    private final List<String> parameters;
    private final boolean addOnly;

    UpdateChannelInstructionImpl(int thingTypeVersion, List<String> parameters, boolean addOnly) {
        this.thingTypeVersion = thingTypeVersion;
        this.parameters = List.copyOf(parameters);
        this.addOnly = addOnly;
    }

    @Override
    public int getThingTypeVersion() {
        return thingTypeVersion;
    }

    @Override
    public void perform(Thing thing, ThingBuilder thingBuilder) {
        ChannelUID affectedChannelUid = new ChannelUID(thing.getUID(), parameters.get(0));
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
                .withType(new ChannelTypeUID(parameters.get(1))).withConfiguration(channelConfiguration);

        if (parameters.size() >= 3) {
            // label is optional (could be inherited from thing-type)
            channelBuilder.withLabel(parameters.get(2));
        }
        if (parameters.size() == 4) {
            // description is optional (could be inherited from thing-type)
            channelBuilder.withDescription(parameters.get(3));
        }
        thingBuilder.withChannel(channelBuilder.build());
    }
}
