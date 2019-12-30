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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
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
public class RawButtonOnOffSwitchProfileTest {

    @Mock
    private ProfileCallback mockCallback;

    @Before
    public void setup() {
        mockCallback = mock(ProfileCallback.class);
    }

    @Test
    public void testOnOffSwitchItem() {
        TriggerProfile profile = new RawButtonOnOffSwitchProfile(mockCallback);
        verifyAction(profile, CommonTriggerEvents.PRESSED, OnOffType.ON);
        verifyAction(profile, CommonTriggerEvents.RELEASED, OnOffType.OFF);
    }

    private void verifyAction(TriggerProfile profile, String trigger, Command expectation) {
        reset(mockCallback);
        profile.onTriggerFromHandler(trigger);
        verify(mockCallback, times(1)).sendCommand(eq(expectation));
    }
}
