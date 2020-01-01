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
package org.openhab.core.thing.internal.profiles;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.test.java.JavaTest;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.types.State;

/**
 * Tests for the system:timestamp-update profile
 *
 * @author Gaël L'hopital - Initial contribution
 */
public class TimestampProfileTest extends JavaTest {

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

        // No existing previous state saved, the callback is first called
        timestampProfile.onStateUpdateFromItem(new DecimalType(23));
        verify(callback, times(1)).sendUpdate(capture.capture());
        State result = capture.getValue();
        DateTimeType changeResult = (DateTimeType) result;

        waitForAssert(() -> assertTrue(ZonedDateTime.now().isAfter(changeResult.getZonedDateTime())));

        // The state is unchanged, no additional call to the callback
        timestampProfile.onStateUpdateFromItem(new DecimalType(23));
        verify(callback, times(1)).sendUpdate(capture.capture());

        // The state is changed, one additional call to the callback
        timestampProfile.onStateUpdateFromItem(new DecimalType(24));
        verify(callback, times(2)).sendUpdate(capture.capture());
        result = capture.getValue();
        DateTimeType updatedResult = (DateTimeType) result;
        assertTrue(updatedResult.getZonedDateTime().isAfter(changeResult.getZonedDateTime()));
    }

}
