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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.types.UnDefType;

/**
 *
 * @author Sami Salonen - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class SystemNonUndefProfileTest {

    private @Mock @NonNullByDefault({}) ProfileCallback callbackMock;
    private @Mock @NonNullByDefault({}) ProfileContext contextMock;

    private SystemNonUndefProfile initProfile(boolean inverted) {
        final Map<String, @Nullable Object> properties = new HashMap<>(2);
        properties.put("inverted", inverted);
        when(contextMock.getConfiguration()).thenReturn(new Configuration(properties));
        SystemNonUndefProfile profile = new SystemNonUndefProfile(callbackMock, contextMock);
        return profile;
    }

    //
    // Command from item
    //

    @Test
    public void testOnCommandFromItemSwitch() {
        SystemNonUndefProfile profile = initProfile(false);
        profile.onCommandFromItem(OnOffType.ON);

        verify(callbackMock).handleCommand(eq(OnOffType.ON));
        verifyNoMoreInteractions(callbackMock);
    }

    @Test
    public void testOnCommandFromItemNumber() {
        SystemNonUndefProfile profile = initProfile(false);
        profile.onCommandFromItem(DecimalType.ZERO);

        verify(callbackMock).handleCommand(eq(DecimalType.ZERO));
        verifyNoMoreInteractions(callbackMock);
    }

    //
    // Command from item (inverted)
    // (no change in handleCommand calls)
    //

    @Test
    public void testOnCommandFromItemSwitchInverted() {
        SystemNonUndefProfile profile = initProfile(true);
        profile.onCommandFromItem(OnOffType.ON);

        verify(callbackMock).handleCommand(eq(OnOffType.ON));
        verifyNoMoreInteractions(callbackMock);
    }

    @Test
    public void testOnCommandFromItemNumberInverted() {
        SystemNonUndefProfile profile = initProfile(false);
        profile.onCommandFromItem(DecimalType.ZERO);

        verify(callbackMock).handleCommand(eq(DecimalType.ZERO));
        verifyNoMoreInteractions(callbackMock);
    }

    //
    // State update from handler
    //

    @Test
    public void testStateUpdatedFromHandlerSwitchOn() {
        SystemNonUndefProfile profile = initProfile(false);
        profile.onStateUpdateFromHandler(OnOffType.ON);

        verify(callbackMock).sendUpdate(eq(OnOffType.ON));
        verifyNoMoreInteractions(callbackMock);
    }

    @Test
    public void testStateUpdatedFromHandlerSwitchOff() {
        SystemNonUndefProfile profile = initProfile(false);
        profile.onStateUpdateFromHandler(OnOffType.OFF);

        verify(callbackMock).sendUpdate(eq(OnOffType.ON));
        verifyNoMoreInteractions(callbackMock);
    }

    @Test
    public void testStateUpdatedFromHandlerUndef() {
        SystemNonUndefProfile profile = initProfile(false);
        profile.onStateUpdateFromHandler(UnDefType.UNDEF);

        verify(callbackMock).sendUpdate(eq(OnOffType.OFF));
        verifyNoMoreInteractions(callbackMock);
    }

    @Test
    public void testStateUpdatedFromHandlerNull() {
        SystemNonUndefProfile profile = initProfile(false);
        profile.onStateUpdateFromHandler(UnDefType.NULL);

        verify(callbackMock).sendUpdate(eq(OnOffType.OFF));
        verifyNoMoreInteractions(callbackMock);
    }

    //
    // State update from handler (inverted)
    // Same as above but inverted updates
    //

    @Test
    public void testStateUpdatedFromHandlerSwitchOnInverted() {
        SystemNonUndefProfile profile = initProfile(true);
        profile.onStateUpdateFromHandler(OnOffType.ON);

        verify(callbackMock).sendUpdate(eq(OnOffType.OFF));
        verifyNoMoreInteractions(callbackMock);
    }

    @Test
    public void testStateUpdatedFromHandlerSwitchOffInverted() {
        SystemNonUndefProfile profile = initProfile(true);
        profile.onStateUpdateFromHandler(OnOffType.OFF);

        verify(callbackMock).sendUpdate(eq(OnOffType.OFF));
        verifyNoMoreInteractions(callbackMock);
    }

    @Test
    public void testStateUpdatedFromHandlerUndefInverted() {
        SystemNonUndefProfile profile = initProfile(true);
        profile.onStateUpdateFromHandler(UnDefType.UNDEF);

        verify(callbackMock).sendUpdate(eq(OnOffType.ON));
        verifyNoMoreInteractions(callbackMock);
    }

    @Test
    public void testStateUpdatedFromHandlerNullInverted() {
        SystemNonUndefProfile profile = initProfile(true);
        profile.onStateUpdateFromHandler(UnDefType.NULL);

        verify(callbackMock).sendUpdate(eq(OnOffType.ON));
        verifyNoMoreInteractions(callbackMock);
    }

    //
    // Command from handler
    //

    @Test
    public void testCommandFromHandlerSwitchOn() {
        SystemNonUndefProfile profile = initProfile(false);
        profile.onCommandFromHandler(OnOffType.ON);

        verify(callbackMock).sendCommand(eq(OnOffType.ON));
        verifyNoMoreInteractions(callbackMock);
    }

    @Test
    public void testCommandFromHandlerSwitchOff() {
        SystemNonUndefProfile profile = initProfile(false);
        profile.onCommandFromHandler(OnOffType.OFF);

        verify(callbackMock).sendCommand(eq(OnOffType.ON));
        verifyNoMoreInteractions(callbackMock);
    }

    @Test
    public void testCommandFromHandlerNumber() {
        SystemNonUndefProfile profile = initProfile(false);
        profile.onCommandFromHandler(DecimalType.ZERO);

        verify(callbackMock).sendCommand(eq(OnOffType.ON));
        verifyNoMoreInteractions(callbackMock);
    }

    //
    // Command from handler (inverted)
    // Same as above but inverted updates
    //

    @Test
    public void testCommandFromHandlerSwitchOnInverted() {
        SystemNonUndefProfile profile = initProfile(true);
        profile.onCommandFromHandler(OnOffType.ON);

        verify(callbackMock).sendCommand(eq(OnOffType.OFF));
        verifyNoMoreInteractions(callbackMock);
    }

    @Test
    public void testCommandFromHandlerSwitchOffInverted() {
        SystemNonUndefProfile profile = initProfile(true);
        profile.onCommandFromHandler(OnOffType.OFF);

        verify(callbackMock).sendCommand(eq(OnOffType.OFF));
        verifyNoMoreInteractions(callbackMock);
    }

    @Test
    public void testCommandFromHandlerNumberInverted() {
        SystemNonUndefProfile profile = initProfile(true);
        profile.onCommandFromHandler(DecimalType.ZERO);

        verify(callbackMock).sendCommand(eq(OnOffType.OFF));
        verifyNoMoreInteractions(callbackMock);
    }

}
