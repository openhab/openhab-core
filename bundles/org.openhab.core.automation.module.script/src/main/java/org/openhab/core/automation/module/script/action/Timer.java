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
package org.openhab.core.automation.module.script.action;

import java.time.ZonedDateTime;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A timer is a handle for a block of code that is scheduled for future execution. A timer
 * can be canceled or rescheduled.
 * The script action "createTimer" returns a {@link Timer} instance.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public interface Timer {

    /**
     * Cancels the timer
     *
     * @return true, if cancellation was successful
     */
    boolean cancel();

    /**
     * Gets the scheduled exection time
     *
     * @return the scheduled execution time, or null if the timer was cancelled
     */
    @Nullable
    ZonedDateTime getExecutionTime();

    /**
     * Determines whether the scheduled execution is yet to happen.
     *
     * @return true, if the code is still scheduled to execute, false otherwise
     */
    boolean isActive();

    /**
     * Determines whether the timer has been cancelled
     *
     * @return true, if the timer has been cancelled, false otherwise
     */
    boolean isCancelled();

    /**
     * Determines whether the scheduled code is currently executed.
     *
     * @return true, if the code is being executed, false otherwise
     */
    boolean isRunning();

    /**
     * Determines whether the scheduled execution has already terminated.
     *
     * @return true, if the scheduled execution has already terminated, false otherwise
     */
    boolean hasTerminated();

    /**
     * Reschedules a timer to a new starting time.
     * This can also be called after a timer has terminated, which will result in another
     * execution of the same code.
     *
     * @param newTime the new time to execute the code
     * @return true, if the rescheduling was done successful
     */
    boolean reschedule(ZonedDateTime newTime);
}
