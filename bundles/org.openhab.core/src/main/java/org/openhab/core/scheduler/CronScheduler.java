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
package org.openhab.core.scheduler;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The software utility Cron is a time-based job scheduler in Unix-like computer
 * operating systems. People who set up and maintain software environments use
 * cron to schedule jobs (commands or shell scripts) to run periodically at
 * fixed times, dates, or intervals. It typically automates system maintenance
 * or administration—though its general-purpose nature makes it useful for
 * things like connecting to the Internet and downloading email at regular
 * intervals.[1] The name cron comes from the Greek word for time, χρόνος
 * chronos.
 * <p>
 * The Unix Cron defines a syntax that is used by the Cron service. A user
 * should register a Cron service with the {@link CronJob#CRON} property. The
 * value is according to the {link http://en.wikipedia.org/wiki/Cron}.
 * <p>
 *
 * <pre>
 * * * * * * * *
 * | │ │ │ │ │ |
 * | │ │ │ │ │ └ year (optional)
 * | │ │ │ │ └── day of week from Monday (1) to Sunday (7).
 * | │ │ │ └──── month (1 - 12) from January (1) to December (12).
 * | │ │ └────── day of month (1 - 31)
 * | │ └──────── hour (0 - 23)
 * | └────────── min (0 - 59)
 * └──────────── sec (0-59)
 * </pre>
 *
 * <pre>
 * Field name   mandatory  Values           Special characters
 * Seconds      Yes        0-59             * / , -
 * Minutes      Yes        0-59             * / , -
 * Hours        Yes        0-23             * / , -
 * Day of month Yes        1-31             * / , - ? L W
 * Month        Yes        1-12 or JAN-DEC  * / , -
 * Day of week  Yes        1-7 or MON-SUN   * / , - ? L #
 * Year         No         1970–2099        * / , -
 * </pre>
 *
 * <h3>Asterisk ( * )</h3>
 * <p>
 * The asterisk indicates that the cron expression matches for all values of the
 * field. E.g., using an asterisk in the 4th field (month) indicates every
 * month.
 * <h3>Slash ( / )</h3>
 * <p>
 * Slashes describe increments of ranges. For example 3-59/15 in the 1st field
 * (minutes) indicate the third minute of the hour and every 15 minutes
 * thereafter. The form "*\/..." is equivalent to the form "first-last/...",
 * that is, an increment over the largest possible range of the field.
 * <h3>Comma ( , )</h3>
 * <p>
 * Commas are used to separate items of a list. For example, using "MON,WED,FRI"
 * in the 5th field (day of week) means Mondays, Wednesdays and Fridays. Hyphen
 * ( - ) Hyphens define ranges. For example, 2000-2010 indicates every year
 * between 2000 and 2010 AD, inclusive.
 * <p>
 * Additionally, you can use some fixed formats:
 *
 * <pre>
 * &#64;yearly (or &#64;annually)  Run once a year at midnight on the morning of January 1                      0 0 1 1 *
 * &#64;monthly                Run once a month at midnight on the morning of the first day of the month    0 0 1 * *
 * &#64;weekly                 Run once a week at midnight on Sunday morning                                0 0 * * 0
 * &#64;daily                  Run once a day at midnight                                                   0 0 * * *
 * &#64;hourly                 Run once an hour at the beginning of the hour                                0 * * * *
 * &#64;reboot                 Run at startup                                                               &#64;reboot
 * </pre>
 * <p>
 * Please note that for the constants we follow the Java 8 Date & Time constants.
 * Major difference is the day number. In Quartz this is 0-6 for SAT-SUN while
 * here it is 1-7 for MON-SUN.
 *
 * @author Peter Kriens - Initial contribution
 * @author Simon Kaufmann - adapted to CompletableFutures
 * @author Hilbrand Bouwkamp - Moved Cron scheduling to it's own interface
 */
@NonNullByDefault
public interface CronScheduler {
    /**
     * Schedule a runnable to be executed for the give cron expression (See
     * {@link CronJob}). Every time when the cronExpression matches the current
     * time, the runnable will be run. The method returns a {@link ScheduledCompletableFuture}
     * that can be used to stop scheduling. This variation does not take an
     * environment object.
     *
     * @param runnable The runnable to run
     * @param cronExpression A cron expression
     * @return A {@link ScheduledCompletableFuture} to cancel the schedule
     */
    ScheduledCompletableFuture<Void> schedule(SchedulerRunnable runnable, String cronExpression);

    /**
     * Schedule a runnable to be executed for the give cron expression (See
     * {@link CronJob}). Every time when the cronExpression matches the current
     * time, the runnable will be run. The method returns a {@link ScheduledCompletableFuture}
     * that can be used to stop scheduling. The run method of cronJob takes
     * an environment object. An environment object is a custom interface where
     * the names of the methods are the keys in the properties (see {@link DTOs}).
     *
     * @param <T> The data type of the parameter for the cron job
     * @param cronJob The runnable to run
     * @param cronExpression A cron expression
     * @return A {@link ScheduledCompletableFuture} to cancel the schedule
     */
    ScheduledCompletableFuture<Void> schedule(CronJob cronJob, Map<String, Object> config, String cronExpression);
}
