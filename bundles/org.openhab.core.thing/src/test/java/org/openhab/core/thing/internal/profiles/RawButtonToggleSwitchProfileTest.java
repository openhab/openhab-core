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
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.thing.CommonTriggerEvents;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.TriggerProfile;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 *
 * @author Simon Kaufmann - Initial contribution
 */
public class RawButtonToggleSwitchProfileTest {

    @Mock
    private ProfileCallback mockCallback;

    @Before
    public void setup() {
        mockCallback = mock(ProfileCallback.class);
    }

    @Test
    public void testSwitchItem() {
        TriggerProfile profile = new RawButtonToggleSwitchProfile(mockCallback);
        verifyAction(profile, UnDefType.NULL, OnOffType.ON);
        verifyAction(profile, OnOffType.ON, OnOffType.OFF);
        verifyAction(profile, OnOffType.OFF, OnOffType.ON);
    }

    @Test
    public void testDimmerItem() {
        TriggerProfile profile = new RawButtonToggleSwitchProfile(mockCallback);
        verifyAction(profile, UnDefType.NULL, OnOffType.ON);
        verifyAction(profile, PercentType.HUNDRED, OnOffType.OFF);
        verifyAction(profile, PercentType.ZERO, OnOffType.ON);
        verifyAction(profile, new PercentType(50), OnOffType.OFF);
    }

    @Test
    public void testColorItem() {
        TriggerProfile profile = new RawButtonToggleSwitchProfile(mockCallback);
        verifyAction(profile, UnDefType.NULL, OnOffType.ON);
        verifyAction(profile, HSBType.WHITE, OnOffType.OFF);
        verifyAction(profile, HSBType.BLACK, OnOffType.ON);
        verifyAction(profile, new HSBType("0,50,50"), OnOffType.OFF);
    }

    private void verifyAction(TriggerProfile profile, State preCondition, Command expectation) {
        reset(mockCallback);
        profile.onStateUpdateFromItem(preCondition);
        profile.onTriggerFromHandler(CommonTriggerEvents.PRESSED);
        verify(mockCallback, times(1)).sendCommand(eq(expectation));
    }

}
