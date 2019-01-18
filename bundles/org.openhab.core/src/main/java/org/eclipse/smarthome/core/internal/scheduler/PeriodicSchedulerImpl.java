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
package org.eclipse.smarthome.core.internal.scheduler;

import java.time.Duration;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.scheduler.PeriodicScheduler;
import org.eclipse.smarthome.core.scheduler.ScheduledCompletableFuture;
import org.eclipse.smarthome.core.scheduler.Scheduler;
import org.eclipse.smarthome.core.scheduler.SchedulerRunnable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Implementation of a {@link PeriodicScheduler}.
 *
 * @author Peter Kriens - initial contribution and API
 * @author Simon Kaufmann - adapted to CompletableFutures
 * @author Hilbrand Bouwkamp - moved periodic scheduling to it's own interface
 */
@Component(service = PeriodicScheduler.class)
@NonNullByDefault
public class PeriodicSchedulerImpl implements PeriodicScheduler {

    private @NonNullByDefault({}) Scheduler scheduler;

    @Override
    public <T> ScheduledCompletableFuture<T> schedule(SchedulerRunnable runnable, Duration... delays) {
        return scheduler.schedule(runnable, new PeriodicAdjuster(delays));
    }

    @Reference
    void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    void unsetScheduler(Scheduler scheduler) {
        this.scheduler = null;
    }
}
