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
package org.openhab.core.model.script.actions;

import java.time.ZonedDateTime;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link Timer} is a wrapper for the {@link org.openhab.core.automation.module.script.action.Timer}
 * interface. This is necessary because the implementation methods of an interface can't be called from
 * the script engine if the implementation is not in a public package or internal to the model bundle
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class Timer {

    private final org.openhab.core.automation.module.script.action.Timer timer;

    public Timer(org.openhab.core.automation.module.script.action.Timer timer) {
        this.timer = timer;
    }

    public boolean cancel() {
        return timer.cancel();
    }

    public @Nullable ZonedDateTime getExecutionTime() {
        return timer.getExecutionTime();
    }

    public boolean isActive() {
        return timer.isActive();
    }

    public boolean isCancelled() {
        return timer.isCancelled();
    }

    public boolean isRunning() {
        return timer.isRunning();
    }

    public boolean hasTerminated() {
        return timer.hasTerminated();
    }

    public boolean reschedule(ZonedDateTime newTime) {
        return timer.reschedule(newTime);
    }
}
