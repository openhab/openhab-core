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

import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Interface that extends {@link TemporalAdjuster} and adds more functionality.
 * This interface is passed to the scheduler for repeating schedules.
 *
 * @author Hilbrand Bouwkamp - initial contribution
 */
@NonNullByDefault
public interface SchedulerTemporalAdjuster extends TemporalAdjuster {
    /**
     * Used by the scheduler to determine if it should continue scheduling jobs.
     * If returns true the implementation of this interface determines the job
     * should not run again given. No new job will be scheduled.
     *
     * @param temporal The temporal to determine if the next run should be scheduled
     * @return true if running is done and the job should not run anymore.
     */
    boolean isDone(Temporal temporal);
}