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
package org.openhab.core.model.script.internal.actions;

import java.time.ZonedDateTime;

import org.openhab.core.model.script.actions.Timer;
import org.openhab.core.scheduler.ScheduledCompletableFuture;
import org.openhab.core.scheduler.Scheduler;
import org.openhab.core.scheduler.SchedulerRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an implementation of the {@link Timer} interface.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class TimerImpl implements Timer {

    private final Logger logger = LoggerFactory.getLogger(TimerImpl.class);

    private final Scheduler scheduler;
    private final ZonedDateTime startTime;
    private final SchedulerRunnable runnable;
    private ScheduledCompletableFuture<Object> future;

    private boolean cancelled;

    public TimerImpl(Scheduler scheduler, ZonedDateTime startTime, SchedulerRunnable runnable) {
        this.scheduler = scheduler;
        this.startTime = startTime;
        this.runnable = runnable;

        future = scheduler.schedule(runnable, startTime.toInstant());
    }

    @Override
    public boolean cancel() {
        cancelled = true;
        return future.cancel(true);
    }

    @Override
    public boolean reschedule(ZonedDateTime newTime) {
        if (future.cancel(false)) {
            cancelled = false;
            future = scheduler.schedule(runnable, newTime.toInstant());
            return true;
        } else {
            logger.warn("Rescheduling failed as execution has already started!");
            return false;
        }
    }

    @Override
    public boolean isActive() {
        return !future.isDone() && !cancelled;
    }

    @Override
    public boolean isRunning() {
        return isActive() && ZonedDateTime.now().isAfter(startTime);
    }

    @Override
    public boolean hasTerminated() {
        return future.isDone();
    }
}
