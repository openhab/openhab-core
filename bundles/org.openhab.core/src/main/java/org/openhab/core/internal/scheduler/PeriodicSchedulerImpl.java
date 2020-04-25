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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.scheduler.PeriodicScheduler;
import org.openhab.core.scheduler.ScheduledCompletableFuture;
import org.openhab.core.scheduler.Scheduler;
import org.openhab.core.scheduler.SchedulerRunnable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Implementation of a {@link PeriodicScheduler}.
 *
 * @author Peter Kriens - Initial contribution
 * @author Simon Kaufmann - adapted to CompletableFutures
 * @author Hilbrand Bouwkamp - moved periodic scheduling to it's own interface
 */
@Component(service = PeriodicScheduler.class)
@NonNullByDefault
public class PeriodicSchedulerImpl implements PeriodicScheduler {

    private final Scheduler scheduler;

    @Activate
    public PeriodicSchedulerImpl(final @Reference Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public <T> ScheduledCompletableFuture<T> schedule(SchedulerRunnable runnable, Duration... delays) {
        return scheduler.schedule(runnable, new PeriodicAdjuster(delays));
    }
}
