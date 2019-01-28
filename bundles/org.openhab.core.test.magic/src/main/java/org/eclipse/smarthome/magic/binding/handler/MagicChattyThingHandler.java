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
package org.eclipse.smarthome.magic.binding.handler;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ThingHandler that randomly sends numbers and strings to channels based on a configured interval
 *
 * @author Stefan Triller - initial contribution
 *
 */
@NonNullByDefault
public class MagicChattyThingHandler extends BaseThingHandler {

    private static Logger logger = LoggerFactory.getLogger(MagicChattyThingHandler.class);
    private static String PARAM_INTERVAL = "interval";
    private static int START_DELAY = 3;

    private static final List<String> randomTexts = Stream
            .of("OPEN", "CLOSED", "ON", "OFF", "Hello", "This is a sentence").collect(Collectors.toList());

    private final Set<ChannelUID> numberChannelUIDs = new HashSet<>();
    private final Set<ChannelUID> textChannelUIDs = new HashSet<>();

    private BigDecimal interval = new BigDecimal(0);
    private final Runnable chatRunnable;
    private @Nullable ScheduledFuture<?> backgroundJob = null;

    @Override
    public void initialize() {
        Configuration config = getConfig();
        interval = (BigDecimal) config.get(PARAM_INTERVAL);

        if (interval == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Interval not set");
            return;
        }

        // do not start the chatting job if interval is 0, just set the thing to ONLINE
        if (interval.intValue() > 0) {
            backgroundJob = scheduler.scheduleWithFixedDelay(chatRunnable, START_DELAY, interval.intValue(),
                    TimeUnit.SECONDS);
        }

        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        if (backgroundJob != null && !backgroundJob.isCancelled()) {
            backgroundJob.cancel(true);
        }
    }

    public MagicChattyThingHandler(Thing thing) {
        super(thing);

        chatRunnable = new Runnable() {
            @Override
            public void run() {
                for (ChannelUID channelUID : numberChannelUIDs) {
                    double randomValue = Math.random() * 100;
                    int intValue = (int) randomValue;
                    State cmd;
                    if (intValue % 2 == 0) {
                        cmd = new QuantityType<Temperature>(randomValue + "°C");
                    } else {
                        cmd = new DecimalType(randomValue);
                    }
                    updateState(channelUID, cmd);
                }

                for (ChannelUID channelUID : textChannelUIDs) {
                    int pos = (int) (Math.random() * (randomTexts.size() - 1));
                    String randomValue = randomTexts.get(pos);

                    StringType cmd = new StringType(randomValue);
                    updateState(channelUID, cmd);
                }
            }
        };

    }

    @Override
    public void channelLinked(ChannelUID channelUID) {
        super.channelLinked(channelUID);

        if ("number".equals(channelUID.getId())) {
            numberChannelUIDs.add(channelUID);
        } else if ("text".equals(channelUID.getId())) {
            textChannelUIDs.add(channelUID);
        }
    }

    @Override
    public void channelUnlinked(ChannelUID channelUID) {
        super.channelUnlinked(channelUID);

        if ("number".equals(channelUID.getId())) {
            numberChannelUIDs.remove(channelUID);
        } else if ("text".equals(channelUID.getId())) {
            textChannelUIDs.remove(channelUID);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received command {} on channel {}", command, channelUID);
    }

    @Override
    protected void updateState(ChannelUID channelUID, State state) {
        logger.debug("Got state {} from device on channel {}", state, channelUID);
        super.updateState(channelUID, state);
    }

}
