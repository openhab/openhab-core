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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.thing.profiles.ProfileCallback;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests for the system:invert-rollershutter profile
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class SystemInvertRollershutterProfileTest {

    @Test
    public void testInversionOnStateUpdateFromItem() {
        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemInvertRollershutterProfile invertProfile = new SystemInvertRollershutterProfile(callback);

        invertProfile.onStateUpdateFromItem(UpDownType.UP);
        verify(callback, times(1)).handleUpdate(capture.capture());
        assertEquals(UpDownType.DOWN, capture.getValue());

        invertProfile.onStateUpdateFromItem(UpDownType.DOWN);
        verify(callback, times(2)).handleUpdate(capture.capture());
        assertEquals(UpDownType.UP, capture.getValue());

        State state = new PercentType(23);
        invertProfile.onStateUpdateFromItem(state);
        verify(callback, times(3)).handleUpdate(capture.capture());
        State result = capture.getValue();
        assertEquals(77, ((PercentType) result).intValue());
    }

    @Test
    public void testInversionOnCommandFromItem() {
        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemInvertRollershutterProfile invertProfile = new SystemInvertRollershutterProfile(callback);

        invertProfile.onCommandFromItem(UpDownType.UP);
        verify(callback, times(1)).handleCommand(capture.capture());
        assertEquals(UpDownType.DOWN, capture.getValue());

        invertProfile.onCommandFromItem(UpDownType.DOWN);
        verify(callback, times(2)).handleCommand(capture.capture());
        assertEquals(UpDownType.UP, capture.getValue());

        Command cmd = new PercentType(23);
        invertProfile.onCommandFromItem(cmd);
        verify(callback, times(3)).handleCommand(capture.capture());
        Command result = capture.getValue();
        assertEquals(77, ((PercentType) result).intValue());
    }

    @Test
    public void testInversionOnCommandFromHandler() {
        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemInvertRollershutterProfile invertProfile = new SystemInvertRollershutterProfile(callback);

        invertProfile.onCommandFromHandler(UpDownType.UP);
        verify(callback, times(1)).sendCommand(capture.capture());
        assertEquals(UpDownType.DOWN, capture.getValue());

        invertProfile.onCommandFromHandler(UpDownType.DOWN);
        verify(callback, times(2)).sendCommand(capture.capture());
        assertEquals(UpDownType.UP, capture.getValue());

        Command cmd = new PercentType(23);
        invertProfile.onCommandFromHandler(cmd);
        verify(callback, times(3)).sendCommand(capture.capture());
        Command result = capture.getValue();
        assertEquals(77, ((PercentType) result).intValue());
    }

    @Test
    public void testInversionOnStateUpdateFromHandler() {
        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemInvertRollershutterProfile invertProfile = new SystemInvertRollershutterProfile(callback);

        invertProfile.onStateUpdateFromHandler(UpDownType.UP);
        verify(callback, times(1)).sendUpdate(capture.capture());
        assertEquals(UpDownType.DOWN, capture.getValue());

        invertProfile.onStateUpdateFromHandler(UpDownType.DOWN);
        verify(callback, times(2)).sendUpdate(capture.capture());
        assertEquals(UpDownType.UP, capture.getValue());

        State state = new PercentType(23);
        invertProfile.onStateUpdateFromHandler(state);
        verify(callback, times(3)).sendUpdate(capture.capture());
        State result = capture.getValue();
        assertEquals(77, ((PercentType) result).intValue());
    }
}
