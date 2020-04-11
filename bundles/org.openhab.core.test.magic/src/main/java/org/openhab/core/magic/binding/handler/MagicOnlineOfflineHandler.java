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

import java.math.BigDecimal;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;

/**
 * The {@link MagicOnlineOfflineHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public class MagicOnlineOfflineHandler extends BaseThingHandler {

    private static final String TOGGLE_TIME = "toggleTime";

    private @NonNullByDefault({}) ScheduledFuture<?> toggleJob;

    public MagicOnlineOfflineHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        int toggleTime = ((BigDecimal) getConfig().get(TOGGLE_TIME)).intValue();

        toggleJob = scheduler.scheduleWithFixedDelay(() -> {
            if (getThing().getStatus() == ThingStatus.ONLINE) {
                updateStatus(ThingStatus.OFFLINE);
            } else if (getThing().getStatus() != ThingStatus.ONLINE) {
                updateStatus(ThingStatus.ONLINE);
            }
        }, 0, toggleTime, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        if (toggleJob != null) {
            toggleJob.cancel(true);
            toggleJob = null;
        }
        super.dispose();
    }
}
