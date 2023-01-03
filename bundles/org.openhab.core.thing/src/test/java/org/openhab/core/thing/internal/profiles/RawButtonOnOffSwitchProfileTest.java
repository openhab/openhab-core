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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.CommonTriggerEvents;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.TriggerProfile;
import org.openhab.core.types.Command;

/**
 * Tests for the system:rawbutton-on-off-switch profile
 *
 * @author Mark Hilbush - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class RawButtonOnOffSwitchProfileTest {

    private @Mock @NonNullByDefault({}) ProfileCallback callbackMock;

    @Test
    public void testOnOffSwitchItem() {
        TriggerProfile profile = new RawButtonOnOffSwitchProfile(callbackMock);
        verifyAction(profile, CommonTriggerEvents.PRESSED, OnOffType.ON);
        verifyAction(profile, CommonTriggerEvents.RELEASED, OnOffType.OFF);
    }

    private void verifyAction(TriggerProfile profile, String trigger, Command expectation) {
        reset(callbackMock);
        profile.onTriggerFromHandler(trigger);
        verify(callbackMock, times(1)).sendCommand(eq(expectation));
    }
}
