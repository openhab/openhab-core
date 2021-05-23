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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.thing.CommonTriggerEvents;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.TriggerProfile;
import org.openhab.core.types.State;

/**
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class TimestampTriggerProfileTest {

    @Test
    public void testTimestampOnTrigger() {
        ProfileCallback callback = mock(ProfileCallback.class);
        TriggerProfile profile = new TimestampTriggerProfile(callback);

        ZonedDateTime now = ZonedDateTime.now();
        profile.onTriggerFromHandler(CommonTriggerEvents.PRESSED);
        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        State result = capture.getValue();
        DateTimeType updateResult = (DateTimeType) result;
        ZonedDateTime timestamp = updateResult.getZonedDateTime();
        long difference = ChronoUnit.MINUTES.between(now, timestamp);
        assertTrue(difference < 1);
    }
}
