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
package org.openhab.core.thing.internal.profiles;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.test.java.JavaTest;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.types.State;

/**
 * Tests for {@link TimestampChangeProfile} and {@link TimestampUpdateProfile}.
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class TimestampProfileTest extends JavaTest {

    @Test
    public void testTimestampOnUpdateStateUpdateFromHandler() {
        ProfileCallback callback = mock(ProfileCallback.class);
        TimestampUpdateProfile timestampProfile = new TimestampUpdateProfile(callback);

        ZonedDateTime now = ZonedDateTime.now();
        timestampProfile.onStateUpdateFromHandler(new DecimalType(23));

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        State result = capture.getValue();
        DateTimeType updateResult = (DateTimeType) result;
        ZonedDateTime timestamp = updateResult.getZonedDateTime();
        long difference = ChronoUnit.MINUTES.between(now, timestamp);
        assertTrue(difference < 1);
    }

    @Test
    public void testTimestampOnChangeStateUpdateFromHandler() {
        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);

        ProfileCallback callback = mock(ProfileCallback.class);
        TimestampChangeProfile timestampProfile = new TimestampChangeProfile(callback);

        // No existing previous state saved, the callback is first called
        timestampProfile.onStateUpdateFromHandler(new DecimalType(23));
        verify(callback, times(1)).sendUpdate(capture.capture());
        State result = capture.getValue();
        DateTimeType changeResult = (DateTimeType) result;

        waitForAssert(() -> assertTrue(ZonedDateTime.now().isAfter(changeResult.getZonedDateTime())));

        // The state is unchanged, no additional call to the callback
        timestampProfile.onStateUpdateFromHandler(new DecimalType(23));
        verify(callback, times(1)).sendUpdate(capture.capture());

        // The state is changed, one additional call to the callback
        timestampProfile.onStateUpdateFromHandler(new DecimalType(24));
        verify(callback, times(2)).sendUpdate(capture.capture());
        result = capture.getValue();
        DateTimeType updatedResult = (DateTimeType) result;
        assertTrue(updatedResult.getZonedDateTime().isAfter(changeResult.getZonedDateTime()));
    }
}
