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

import java.time.ZonedDateTime;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * {@link ConditionHandler} that evaluates, if the current time satisfies an specified condition.
 *
 * @author Sönke Küper - Initial contribution
 */
@NonNullByDefault
public interface TimeBasedConditionHandler extends ConditionHandler {

    /**
     * Checks if this condition is satisfied for the given time.
     *
     * @param time The time to check.
     * @return <code>true</code> if and only if the given time satisfies this condition.
     */
    public abstract boolean isSatisfiedAt(ZonedDateTime time);
}
