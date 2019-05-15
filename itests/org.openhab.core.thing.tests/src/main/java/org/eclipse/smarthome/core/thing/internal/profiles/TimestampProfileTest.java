/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.thing.internal.profiles;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.profiles.ProfileCallback;
import org.eclipse.smarthome.core.types.State;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests for the system:timestamp-update profile
 *
 * @author Gaël L'hopital - initial contribution
 *
 */
public class TimestampProfileTest {

    @Test
    public void testTimestampOnUpdate() {
        ProfileCallback callback = mock(ProfileCallback.class);
        TimestampUpdateProfile timestampProfile = new TimestampUpdateProfile(callback);

        State state = new DecimalType(23);
        ZonedDateTime now = ZonedDateTime.now();
        timestampProfile.onStateUpdateFromItem(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        State result = capture.getValue();
        DateTimeType updateResult = (DateTimeType) result;
        ZonedDateTime timestamp = updateResult.getZonedDateTime();
        long difference = ChronoUnit.MINUTES.between(now, timestamp);
        assertTrue(difference < 1);
    }

    @Test
    public void testTimestampOnChange() {
        ProfileCallback callback = mock(ProfileCallback.class);
        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        TimestampChangeProfile timestampProfile = new TimestampChangeProfile(callback);

        timestampProfile.onStateUpdateFromItem(new DecimalType(23));
        verify(callback, atLeastOnce()).sendUpdate(capture.capture());
        State result = capture.getValue();

        DateTimeType changeResult = (DateTimeType) result;
        timestampProfile.onStateUpdateFromItem(new DecimalType(23));
        verify(callback, atLeastOnce()).sendUpdate(capture.capture());
        result = capture.getValue();
        DateTimeType newChangeResult = (DateTimeType) result;
        assertEquals(changeResult, newChangeResult);

        timestampProfile.onStateUpdateFromItem(new DecimalType(24));
        verify(callback, atLeastOnce()).sendUpdate(capture.capture());
        result = capture.getValue();
        DateTimeType updatedResult = (DateTimeType) result;
        assertTrue(updatedResult.getZonedDateTime().isAfter(newChangeResult.getZonedDateTime()));
    }

}
