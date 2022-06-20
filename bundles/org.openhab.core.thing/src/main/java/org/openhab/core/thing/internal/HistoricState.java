/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.thing.internal;

import java.time.ZonedDateTime;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;

/**
 * A wrapper class for {@link State} which represents an item state plus a date time when the state is valid.
 * 
 * @author Jan M. Hochstein
 *
 */
@NonNullByDefault
public class HistoricState implements State {

    private State state;
    private ZonedDateTime dateTime;

    HistoricState(State state, ZonedDateTime dateTime) {
        this.state = state;
        this.dateTime = dateTime;
    }

    public State getState() {
        return state;
    }

    public ZonedDateTime getDateTime() {
        return dateTime;
    }

    @Override
    public String format(String pattern) {
        return state.format(pattern);
    }

    @Override
    public String toFullString() {
        return state.toFullString();
    }

    @Override
    public <T extends @Nullable State> @Nullable T as(@Nullable Class<T> target) {
        return state.as(target);
    }
}
