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

import static org.openhab.core.magic.binding.MagicBindingConstants.CHANNEL_FORECAST;
import static org.openhab.core.types.TimeSeries.Policy.ADD;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.TimeSeries;

/**
 * The {@link MagicTimeSeriesHandler} is capable of providing a series of different forecasts
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class MagicTimeSeriesHandler extends BaseThingHandler {

    private @Nullable ScheduledFuture<?> scheduledJob;
    private Configuration configuration = new Configuration();

    public MagicTimeSeriesHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // no-op
    }

    @Override
    public void initialize() {
        configuration = getConfigAs(Configuration.class);
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
                Instant now = Instant.now();
                TimeSeries timeSeries = new TimeSeries(ADD);
                Duration stepSize = Duration.ofSeconds(configuration.interval / configuration.count);
                double range = configuration.max - configuration.min;
                for (int i = 1; i <= configuration.count; i++) {
                    double value = switch (configuration.type) {
                        case RND -> Math.random() * range + configuration.min;
                        case ASC -> (range / configuration.count) * i + configuration.min;
                        case DESC -> configuration.max + (range / configuration.count) * i;
                    };
                    timeSeries.add(now.plus(stepSize.multipliedBy(i)), new DecimalType(value));
                }
                sendTimeSeries(CHANNEL_FORECAST, timeSeries);
            }, 0, configuration.interval, TimeUnit.SECONDS);
        }
    }

    private void stopScheduledJob() {
        ScheduledFuture<?> localScheduledJob = scheduledJob;
        if (localScheduledJob != null && !localScheduledJob.isCancelled()) {
            localScheduledJob.cancel(true);
            scheduledJob = null;
        }
    }

    public static class Configuration {
        public int interval = 600;
        public Type type = Type.RND;
        public double min = 0.0;
        public double max = 100.0;
        public int count = 10;

        public Configuration() {
        }
    }

    public enum Type {
        RND,
        ASC,
        DESC
    }
}
