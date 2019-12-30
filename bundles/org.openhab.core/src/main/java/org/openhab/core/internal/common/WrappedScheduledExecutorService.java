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
package org.openhab.core.internal.common;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class wraps the ScheduledThreadPoolExecutor to implement the {@link #afterExecute(Runnable, Throwable)} method
 * and log the exception in case the scheduled runnable threw an exception. The error will otherwise go unnoticed
 * because an exception thrown in the runnable will simply end with no logging unless the user handles it. This
 * wrapper removes the burden for the user to always catch errors in scheduled runnables for logging, and it also
 * catches unchecked exceptions that can be the cause of very hard to catch bugs because no error is ever shown if the
 * user doesn't catch the error in the runnable itself.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
public class WrappedScheduledExecutorService extends ScheduledThreadPoolExecutor {

    final Logger logger = LoggerFactory.getLogger(WrappedScheduledExecutorService.class);

    public WrappedScheduledExecutorService(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        Throwable actualThrowable = t;
        if (actualThrowable == null && r instanceof Future<?>) {
            Future<?> f = (Future<?>) r;

            // The Future is the wrapper task around our scheduled Runnable. This is only "done" if an Exception
            // occurred, the Task was completed, or aborted. A periodic Task (scheduleWithFixedDelay etc.) is NEVER
            // "done" unless there was an Exception because the outer Task is always rescheduled.
            if (f.isDone()) {
                try {
                    // we are NOT interested in the result of the Future but we have to call get() to obtain a possible
                    // Exception from it
                    f.get();
                } catch (CancellationException ce) {
                    // ignore canceled tasks
                } catch (ExecutionException ee) {
                    actualThrowable = ee.getCause();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (actualThrowable != null) {
            logger.warn("Scheduled runnable ended with an exception: ", actualThrowable);
        }
    }
}
