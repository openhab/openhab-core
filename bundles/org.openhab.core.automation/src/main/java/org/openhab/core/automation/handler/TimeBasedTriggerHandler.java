/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.automation.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.scheduler.SchedulerTemporalAdjuster;

/**
 * Marker Interface for an {@link TriggerHandler} that contains an time based execution.
 *
 * @author Sönke Küper - Initial contribution
 */
@NonNullByDefault
public interface TimeBasedTriggerHandler extends TriggerHandler {

    /**
     * Returns the {@link SchedulerTemporalAdjuster} which can be used to determine the next execution times.
     */
    abstract SchedulerTemporalAdjuster getTemporalAdjuster();
}
