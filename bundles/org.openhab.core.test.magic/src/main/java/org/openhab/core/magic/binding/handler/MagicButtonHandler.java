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
package org.openhab.core.magic.binding.handler;

import static org.openhab.core.magic.binding.MagicBindingConstants.CHANNEL_RAWBUTTON;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.CommonTriggerEvents;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;

/**
 * The {@link MagicButtonHandler} is capable of triggering different events. Triggers a PRESSED event every 5 seconds on
 * the rawbutton trigger channel.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class MagicButtonHandler extends BaseThingHandler {

    private @Nullable ScheduledFuture<?> scheduledJob;

    public MagicButtonHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // no-op
    }

    @Override
    public void initialize() {
        startScheduledJob();

        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        stopScheduledJob();
    }

    private void startScheduledJob() {
        ScheduledFuture<?> localScheduledJob = scheduledJob;
        if (localScheduledJob == null || localScheduledJob.isCancelled()) {
            scheduledJob = scheduler.scheduleWithFixedDelay(() -> {
                triggerChannel(CHANNEL_RAWBUTTON, CommonTriggerEvents.PRESSED);
            }, 5, 5, TimeUnit.SECONDS);
        }
    }

    private void stopScheduledJob() {
        ScheduledFuture<?> localScheduledJob = scheduledJob;
        if (localScheduledJob != null && !localScheduledJob.isCancelled()) {
            localScheduledJob.cancel(true);
            scheduledJob = null;
        }
    }
}
