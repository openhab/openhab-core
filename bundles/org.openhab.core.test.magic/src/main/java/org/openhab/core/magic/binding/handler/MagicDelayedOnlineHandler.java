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
package org.openhab.core.magic.binding.handler;

import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;

/**
 * ThingHandler for a thing that goes online after 15 seconds
 *
 * @author Stefan Triller - Initial contribution
 */
@NonNullByDefault
public class MagicDelayedOnlineHandler extends BaseThingHandler {

    private static final int DELAY = 15;

    public MagicDelayedOnlineHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        // schedule delayed job to set the thing to ONLINE
        scheduler.schedule(() -> updateStatus(ThingStatus.ONLINE), DELAY, TimeUnit.SECONDS);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals("number")) {
            if (command instanceof DecimalType) {
                DecimalType cmd = (DecimalType) command;
                int cmdInt = cmd.intValue();
                ThingStatus status = cmdInt > 0 ? ThingStatus.ONLINE : ThingStatus.OFFLINE;
                int waitTime = Math.abs(cmd.intValue());
                scheduler.schedule(() -> updateStatus(status), waitTime, TimeUnit.SECONDS);
            }
        }
    }
}
