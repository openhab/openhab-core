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
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.builder.ThingBuilder;

/**
 * The {@link RemoveChannelInstructionImpl} implements a {@link ThingUpdateInstruction} that removes a channel from a
 * thing.
 * <p />
 * Parameters are:
 * <ul>
 * <li>channelId - the id of the channel</li>
 * </ul>
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class RemoveChannelInstructionImpl implements ThingUpdateInstruction {
    private final int thingTypeVersion;
    private final List<String> parameters;

    RemoveChannelInstructionImpl(int thingTypeVersion, List<String> parameters) {
        this.thingTypeVersion = thingTypeVersion;
        this.parameters = parameters;
    }

    @Override
    public int getThingTypeVersion() {
        return thingTypeVersion;
    }

    @Override
    public void perform(Thing thing, ThingBuilder thingBuilder) {
        ChannelUID affectedChannelUid = new ChannelUID(thing.getUID(), parameters.get(0));
        thingBuilder.withoutChannel(affectedChannelUid);
    }
}
