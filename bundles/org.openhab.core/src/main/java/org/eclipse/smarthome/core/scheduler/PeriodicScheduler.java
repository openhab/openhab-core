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
package org.eclipse.smarthome.core.scheduler;

import java.time.Duration;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Scheduler that runs the same job at the given periods.
 *
 * @author Peter Kriens - initial contribution and API
 * @author Simon Kaufmann - adapted to CompletableFutures
 * @author Hilbrand Bouwkamp - moved periodic scheduling to it's own interface
 */
@NonNullByDefault
public interface PeriodicScheduler {

    /**
     * Schedule a runnable to be executed in definitely at the given delays.
     *
     * Schedules the job based on the given delay. If no more delays are present,
     * the last value is re-used. The method returns a {@link ScheduledCompletableFuture}
     * that can be used to stop scheduling. This is a fixed rate scheduler. That
     * is, a base time is established when this method is called and subsequent
     * firings are always calculated relative to this start time.
     *
     * @param runnable the runnable to run after each duration
     * @param delays subsequent delays
     * @return returns a {@link ScheduledCompletableFuture} to cancel the schedule
     */
    <T> ScheduledCompletableFuture<T> schedule(SchedulerRunnable runnable, Duration... delays);
}
