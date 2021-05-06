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
package org.openhab.core.thing.internal.profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * Tests for the system:timestamp-offset profile
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class TimestampOffsetProfileTest {

    public static class ParameterSet {
        public final long seconds;
        public final @Nullable String timeZone;
        public final ZoneOffset expectedZoneOffset;

        public ParameterSet(long seconds, @Nullable String timeZone) {
            this.seconds = seconds;
            this.timeZone = timeZone;
            this.expectedZoneOffset = timeZone == null ? ZoneOffset.UTC
                    : ZoneId.of(timeZone).getRules().getOffset(LocalDateTime.now());
        }
    }

    public static Collection<Object[]> parameters() {
        return List.of(new Object[][] { //
                { new ParameterSet(0, null) }, //
                { new ParameterSet(30, null) }, //
                { new ParameterSet(-30, null) }, //
                { new ParameterSet(0, "Europe/Berlin") }, //
                { new ParameterSet(30, "Europe/Berlin") }, //
                { new ParameterSet(-30, "Europe/Berlin") } });
    }

    @Test
    public void testUNDEFOnStateUpdateFromHandler() {
        ProfileCallback callback = mock(ProfileCallback.class);
        TimestampOffsetProfile offsetProfile = createProfile(callback, Long.toString(60), null);

        State state = UnDefType.UNDEF;
        offsetProfile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        State result = capture.getValue();
        assertEquals(UnDefType.UNDEF, result);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testOnCommandFromItem(ParameterSet parameterSet) {
        ProfileCallback callback = mock(ProfileCallback.class);
        TimestampOffsetProfile offsetProfile = createProfile(callback, Long.toString(parameterSet.seconds),
                parameterSet.timeZone);

        Command cmd = DateTimeType.valueOf("2021-03-30T10:58:47.033+0000");
        offsetProfile.onCommandFromItem(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).handleCommand(capture.capture());

        Command result = capture.getValue();
        DateTimeType updateResult = (DateTimeType) result;
        DateTimeType expectedResult = new DateTimeType(
                ((DateTimeType) cmd).getZonedDateTime().minusSeconds(parameterSet.seconds));
        assertEquals(ZoneOffset.UTC, updateResult.getZonedDateTime().getOffset());
        assertEquals(expectedResult.getZonedDateTime(), updateResult.getZonedDateTime());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testOnCommandFromHandler(ParameterSet parameterSet) {
        ProfileCallback callback = mock(ProfileCallback.class);
        TimestampOffsetProfile offsetProfile = createProfile(callback, Long.toString(parameterSet.seconds),
                parameterSet.timeZone);

        Command cmd = new DateTimeType("2021-03-30T10:58:47.033+0000");
        offsetProfile.onCommandFromHandler(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).sendCommand(capture.capture());

        Command result = capture.getValue();
        DateTimeType updateResult = (DateTimeType) result;
        DateTimeType expectedResult = new DateTimeType(
                ((DateTimeType) cmd).getZonedDateTime().plusSeconds(parameterSet.seconds));
        assertEquals(parameterSet.expectedZoneOffset, updateResult.getZonedDateTime().getOffset());
        String timeZone = parameterSet.timeZone;
        if (timeZone != null) {
            expectedResult = expectedResult.toZone(timeZone);
        }
        assertEquals(expectedResult.getZonedDateTime(), updateResult.getZonedDateTime());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testOnStateUpdateFromHandler(ParameterSet parameterSet) {
        ProfileCallback callback = mock(ProfileCallback.class);
        TimestampOffsetProfile offsetProfile = createProfile(callback, Long.toString(parameterSet.seconds),
                parameterSet.timeZone);

        State state = new DateTimeType("2021-03-30T10:58:47.033+0000");
        offsetProfile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        State result = capture.getValue();
        DateTimeType updateResult = (DateTimeType) result;
        DateTimeType expectedResult = new DateTimeType(
                ((DateTimeType) state).getZonedDateTime().plusSeconds(parameterSet.seconds));
        assertEquals(parameterSet.expectedZoneOffset, updateResult.getZonedDateTime().getOffset());
        String timeZone = parameterSet.timeZone;
        if (timeZone != null) {
            expectedResult = expectedResult.toZone(timeZone);
        }
        assertEquals(expectedResult.getZonedDateTime(), updateResult.getZonedDateTime());
    }

    private TimestampOffsetProfile createProfile(ProfileCallback callback, String offset, @Nullable String timeZone) {
        ProfileContext context = mock(ProfileContext.class);
        Map<String, Object> properties = new HashMap<>();
        properties.put(TimestampOffsetProfile.OFFSET_PARAM, offset);
        if (timeZone != null) {
            properties.put(TimestampOffsetProfile.TIMEZONE_PARAM, timeZone);
        }
        when(context.getConfiguration()).thenReturn(new Configuration(properties));
        return new TimestampOffsetProfile(callback, context);
    }
}
