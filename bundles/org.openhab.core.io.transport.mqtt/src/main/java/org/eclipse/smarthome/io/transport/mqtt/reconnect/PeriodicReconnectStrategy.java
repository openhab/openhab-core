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
package org.eclipse.smarthome.io.transport.mqtt.reconnect;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.io.transport.mqtt.MqttBrokerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an implementation of the {@link AbstractReconnectStrategy}. This
 * strategy tries to reconnect after 10 seconds and then every 60 seconds
 * after a broker connection has been lost.
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public class PeriodicReconnectStrategy extends AbstractReconnectStrategy {
    private final Logger logger = LoggerFactory.getLogger(PeriodicReconnectStrategy.class);
    private final int reconnectFrequency;
    private final int firstReconnectAfter;

    private @Nullable ScheduledExecutorService scheduler = null;
    private @Nullable ScheduledFuture<?> scheduledTask;

    /**
     * Use a default 60s reconnect frequency and try the first reconnect after 10s.
     */
    public PeriodicReconnectStrategy() {
        this(10000, 60000);
    }

    /**
     * Create a {@link PeriodicReconnectStrategy} with the given reconnect frequency and
     * first reconnect time parameters.
     *
     * @param reconnectFrequency This strategy tries to reconnect in this frequency in ms.
     * @param firstReconnectAfter After a connection is lost, the very first reconnect attempt will be performed after
     *            this time in ms.
     */
    public PeriodicReconnectStrategy(int reconnectFrequency, int firstReconnectAfter) {
        this.reconnectFrequency = reconnectFrequency;
        this.firstReconnectAfter = firstReconnectAfter;
    }

    @Override
    public synchronized void start() {
        if (scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(1);
        }
    }

    @Override
    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }

        // If there is a scheduled task ensure it is canceled.
        if (scheduledTask != null) {
            scheduledTask.cancel(true);
            scheduledTask = null;
        }
    }

    /**
     * Returns if the reconnect strategy has been started.
     *
     * @return true if started
     */
    public synchronized boolean isStarted() {
        return scheduler != null;
    }

    @Override
    public synchronized void lostConnection() {
        // Check if we are running (has been started and not stopped) state.
        if (scheduler == null) {
            return;
        }
        if (brokerConnection == null) {
            stop();
            return;
        }

        // If there is already a scheduled task, we continue only if it has been done (shouldn't be the case at all).
        if (scheduledTask != null && !scheduledTask.isDone()) {
            return;
        }

        assert scheduler != null;
        scheduledTask = scheduler.scheduleWithFixedDelay(() -> {
            MqttBrokerConnection brokerConnection = this.brokerConnection;
            // If the broker connections is not available anymore, stop the timed reconnect.
            if (brokerConnection == null) {
                stop();
                return;
            }
            logger.info("Try to restore connection to '{}'. Next attempt in {}ms", brokerConnection.getHost(),
                    getReconnectFrequency());

            brokerConnection.start().exceptionally(e -> {
                logger.warn("Broker connection couldn't be started", e);
                return false;
            });
        }, getFirstReconnectAfter(), getReconnectFrequency(), TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized void connectionEstablished() {
        // Stop the reconnect task if existing.
        if (scheduledTask != null) {
            scheduledTask.cancel(true);
            scheduledTask = null;
        }
    }

    @Override
    public synchronized boolean isReconnecting() {
        return scheduledTask != null;
    }

    public int getReconnectFrequency() {
        return reconnectFrequency;
    }

    public int getFirstReconnectAfter() {
        return firstReconnectAfter;
    }
}
