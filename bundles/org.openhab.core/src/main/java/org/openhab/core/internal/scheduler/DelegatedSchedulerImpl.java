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
package org.openhab.core.internal.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.scheduler.ScheduledCompletableFuture;
import org.openhab.core.scheduler.Scheduler;
import org.openhab.core.scheduler.SchedulerRunnable;
import org.openhab.core.scheduler.SchedulerTemporalAdjuster;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * Wraps the actual Scheduler and keeps track of scheduled jobs.
 * It shuts down jobs in case the service is deactivated.
 *
 * @author Peter Kriens - Initial contribution
 */
@Component(service = Scheduler.class, immediate = true)
@NonNullByDefault
public class DelegatedSchedulerImpl implements Scheduler {

    private final Set<ScheduledCompletableFuture<?>> scheduledJobs = new HashSet<>();

    private @NonNullByDefault({}) SchedulerImpl delegate;

    @Activate
    public DelegatedSchedulerImpl(final @Reference SchedulerImpl scheduler) {
        this.delegate = scheduler;
    }

    @Deactivate
    void deactivate() {
        while (!scheduledJobs.isEmpty()) {
            final ScheduledCompletableFuture<?> scheduledJob;

            synchronized (scheduledJobs) {
                if (scheduledJobs.isEmpty()) {
                    return;
                }
                Iterator<ScheduledCompletableFuture<?>> iterator = scheduledJobs.iterator();
                scheduledJob = iterator.next();
                iterator.remove();
            }
            scheduledJob.cancel(true);
        }
    }

    @Override
    public ScheduledCompletableFuture<Instant> after(Duration delay) {
        return add(delegate.after(delay));
    }

    @Override
    public <T> ScheduledCompletableFuture<T> after(Callable<T> callable, Duration delay) {
        return add(delegate.after(callable, delay));
    }

    @Override
    public <T> ScheduledCompletableFuture<T> before(CompletableFuture<T> promise, Duration timeout) {
        return add(delegate.before(promise, timeout));
    }

    @Override
    public ScheduledCompletableFuture<Instant> at(Instant instant) {
        return add(delegate.at(instant));
    }

    @Override
    public <T> ScheduledCompletableFuture<T> at(Callable<T> callable, Instant instant) {
        return add(delegate.at(callable, instant));
    }

    @Override
    public <T> ScheduledCompletableFuture<T> schedule(SchedulerRunnable runnable,
            SchedulerTemporalAdjuster temporalAdjuster) {
        return add(delegate.schedule(runnable, temporalAdjuster));
    }

    private <T> ScheduledCompletableFuture<T> add(ScheduledCompletableFuture<T> t) {
        synchronized (scheduledJobs) {
            scheduledJobs.add(t);
        }
        t.getPromise().handle((v, e) -> {
            synchronized (scheduledJobs) {
                scheduledJobs.remove(t);
                return v;
            }
        });
        return t;
    }

}
