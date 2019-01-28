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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.scheduler.CronJob;
import org.eclipse.smarthome.core.scheduler.CronScheduler;
import org.eclipse.smarthome.core.scheduler.Scheduler;
import org.eclipse.smarthome.core.scheduler.SchedulerRunnable;
import org.eclipse.smarthome.core.scheduler.ScheduledCompletableFuture;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a {@link CronScheduler}.
 *
 * @author Peter Kriens - initial contribution and API
 * @author Simon Kaufmann - adapted to CompletableFutures
 * @author Hilbrand Bouwkamp - moved cron scheduling to it's own interface
 */
@Component(service = CronScheduler.class)
@NonNullByDefault
public class CronSchedulerImpl implements CronScheduler {

    private final Logger logger = LoggerFactory.getLogger(CronSchedulerImpl.class);

    private final List<Cron> crons = new ArrayList<>();

    private @NonNullByDefault({}) Scheduler scheduler;

    @Override
    public ScheduledCompletableFuture<@Nullable Void> schedule(SchedulerRunnable runnable, String cronExpression) {
        return schedule(d -> runnable.run(), Collections.emptyMap(), cronExpression);
    }

    @Override
    public ScheduledCompletableFuture<@Nullable Void> schedule(CronJob job, Map<String, Object> config,
            String cronExpression) {
        final CronAdjuster cronAdjuster = new CronAdjuster(cronExpression);
        final SchedulerRunnable runnable = () -> {
            job.run(config);
        };

        if (cronAdjuster.isReboot()) {
            return scheduler.at(runnable, Instant.ofEpochMilli(1));
        } else {
            return scheduler.schedule(runnable, cronAdjuster);
        }
    }

    @Reference
    void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    void unsetScheduler(Scheduler scheduler) {
        this.scheduler = null;
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    <T> void addSchedule(CronJob cronJob, Map<String, Object> map) {
        final Object scheduleConfig = map.get(CronJob.CRON);
        String[] schedules = null;

        if (scheduleConfig instanceof String[]) {
            schedules = (String[]) scheduleConfig;
        } else if (scheduleConfig instanceof String) {
            schedules = new String[] { (String) scheduleConfig };
        }
        if (schedules == null || schedules.length == 0) {
            logger.info("No schedules in map with key '" + CronJob.CRON + "'. Nothing scheduled");
            return;
        }

        synchronized (crons) {
            for (String schedule : schedules) {
                try {
                    final Cron cron = new Cron(cronJob, schedule(cronJob, map, schedule));

                    crons.add(cron);
                } catch (RuntimeException e) {
                    logger.warn("Invalid cron expression {} from {}", schedule, map, e);
                }
            }
        }
    }

    void removeSchedule(CronJob s) {
        synchronized (crons) {
            for (Iterator<Cron> cron = crons.iterator(); cron.hasNext();) {
                final Cron c = cron.next();

                if (c.target == s) {
                    cron.remove();
                    c.schedule.cancel(true);
                }
            }
        }
    }

    private static class Cron {
        private final CronJob target;
        private final ScheduledCompletableFuture<?> schedule;

        public Cron(CronJob target, ScheduledCompletableFuture<?> schedule) {
            this.target = target;
            this.schedule = schedule;
        }
    }
}
