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
package org.openhab.core.thing.internal.profiles;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.openhab.core.thing.profiles.SystemProfiles.RAWBUTTON_TOGGLE_SWITCH;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.thing.CommonTriggerEvents;
import org.openhab.core.thing.DefaultSystemChannelTypeProvider;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.TriggerProfile;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 *
 * @author Simon Kaufmann - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class RawButtonToggleSwitchProfileTest {

    private @Mock @NonNullByDefault({}) ProfileCallback callbackMock;
    private @Mock @NonNullByDefault({}) ProfileContext contextMock;

    @Test
    public void testSwitchItem() {
        TriggerProfile profile = newToggleProfile();
        verifyAction(profile, UnDefType.NULL, OnOffType.ON);
        verifyAction(profile, OnOffType.ON, OnOffType.OFF);
        verifyAction(profile, OnOffType.OFF, OnOffType.ON);
    }

    @Test
    public void testDimmerItem() {
        TriggerProfile profile = newToggleProfile();
        verifyAction(profile, UnDefType.NULL, OnOffType.ON);
        verifyAction(profile, PercentType.HUNDRED, OnOffType.OFF);
        verifyAction(profile, PercentType.ZERO, OnOffType.ON);
        verifyAction(profile, new PercentType(50), OnOffType.OFF);
    }

    @Test
    public void testColorItem() {
        TriggerProfile profile = newToggleProfile();
        verifyAction(profile, UnDefType.NULL, OnOffType.ON);
        verifyAction(profile, HSBType.WHITE, OnOffType.OFF);
        verifyAction(profile, HSBType.BLACK, OnOffType.ON);
        verifyAction(profile, new HSBType("0,50,50"), OnOffType.OFF);
    }

    private ToggleProfile<OnOffType> newToggleProfile() {
        return new ToggleProfile<OnOffType>(callbackMock, contextMock, RAWBUTTON_TOGGLE_SWITCH,
                DefaultSystemChannelTypeProvider.SYSTEM_RAWBUTTON, OnOffType.ON, OnOffType.OFF,
                CommonTriggerEvents.PRESSED);
    }

    private void verifyAction(TriggerProfile profile, State preCondition, Command expectation) {
        reset(callbackMock);
        profile.onStateUpdateFromItem(preCondition);
        profile.onTriggerFromHandler(CommonTriggerEvents.PRESSED);
        verify(callbackMock, times(1)).sendCommand(eq(expectation));
    }
}
