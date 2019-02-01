/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.thing.internal.profiles;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import javax.measure.Unit;
import javax.measure.quantity.Temperature;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.ImperialUnits;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.thing.profiles.ProfileCallback;
import org.eclipse.smarthome.core.thing.profiles.ProfileContext;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests for the system:offset profile
 *
 * @author Stefan Triller - initial contribution
 *
 */
public class SystemOffsetProfileTest {

    @Before
    public void setup() {
        // initialize parser with ImperialUnits, otherwise units like °F are unknown
        @SuppressWarnings("unused")
        Unit<Temperature> fahrenheit = ImperialUnits.FAHRENHEIT;
    }

    @Test
    public void testDecimalTypeOnCommandFromItem() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemOffsetProfile offsetProfile = createProfile(callback, "3");

        Command cmd = new DecimalType(23);
        offsetProfile.onCommandFromItem(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).handleCommand(capture.capture());

        Command result = capture.getValue();
        DecimalType decResult = (DecimalType) result;
        assertEquals(20, decResult.intValue());
    }

    @Test
    public void testDecimalTypeOnStateUpdateFromItem() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemOffsetProfile offsetProfile = createProfile(callback, "3");

        State state = new DecimalType(23);
        offsetProfile.onStateUpdateFromItem(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).handleUpdate(capture.capture());

        State result = capture.getValue();
        DecimalType decResult = (DecimalType) result;
        assertEquals(20, decResult.intValue());
    }

    @Test
    public void testQuantityTypeOnCommandFromItem() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemOffsetProfile offsetProfile = createProfile(callback, "3°C");

        Command cmd = new QuantityType<Temperature>("23°C");
        offsetProfile.onCommandFromItem(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).handleCommand(capture.capture());

        Command result = capture.getValue();
        @SuppressWarnings("unchecked")
        QuantityType<Temperature> decResult = (QuantityType<Temperature>) result;
        assertEquals(20, decResult.intValue());
        assertEquals(SIUnits.CELSIUS, decResult.getUnit());
    }

    @Test
    public void testQuantityTypeOnStateUpdateFromItem() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemOffsetProfile offsetProfile = createProfile(callback, "3°C");

        State state = new QuantityType<Temperature>("23°C");
        offsetProfile.onStateUpdateFromItem(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).handleUpdate(capture.capture());

        State result = capture.getValue();
        @SuppressWarnings("unchecked")
        QuantityType<Temperature> decResult = (QuantityType<Temperature>) result;
        assertEquals(20, decResult.intValue());
        assertEquals(SIUnits.CELSIUS, decResult.getUnit());
    }

    @Test
    public void testDecimalTypeOnCommandFromHandler() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemOffsetProfile offsetProfile = createProfile(callback, "3");

        Command cmd = new DecimalType(23);
        offsetProfile.onCommandFromHandler(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).sendCommand(capture.capture());

        Command result = capture.getValue();
        DecimalType decResult = (DecimalType) result;
        assertEquals(26, decResult.intValue());
    }

    @Test
    public void testDecimalTypeOnStateUpdateFromHandler() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemOffsetProfile offsetProfile = createProfile(callback, "3");

        State state = new DecimalType(23);
        offsetProfile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        State result = capture.getValue();
        DecimalType decResult = (DecimalType) result;
        assertEquals(26, decResult.intValue());
    }

    @Test
    public void testQuantityTypeOnCommandFromHandler() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemOffsetProfile offsetProfile = createProfile(callback, "3°C");

        Command cmd = new QuantityType<Temperature>("23°C");
        offsetProfile.onCommandFromHandler(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).sendCommand(capture.capture());

        Command result = capture.getValue();
        @SuppressWarnings("unchecked")
        QuantityType<Temperature> decResult = (QuantityType<Temperature>) result;
        assertEquals(26, decResult.intValue());
        assertEquals(SIUnits.CELSIUS, decResult.getUnit());
    }

    @Test
    public void testQuantityTypeOnStateUpdateFromHandler() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemOffsetProfile offsetProfile = createProfile(callback, "3°C");

        State state = new QuantityType<Temperature>("23°C");
        offsetProfile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        State result = capture.getValue();
        @SuppressWarnings("unchecked")
        QuantityType<Temperature> decResult = (QuantityType<Temperature>) result;
        assertEquals(26, decResult.intValue());
        assertEquals(SIUnits.CELSIUS, decResult.getUnit());
    }

    @Test
    public void testQuantityTypeOnStateUpdateFromHandlerFahrenheitOffset() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemOffsetProfile offsetProfile = createProfile(callback, "3 °F");

        State state = new QuantityType<Temperature>("23 °C");
        offsetProfile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        State result = capture.getValue();
        @SuppressWarnings("unchecked")
        QuantityType<Temperature> decResult = (QuantityType<Temperature>) result;
        assertThat(decResult.doubleValue(), is(closeTo(24.6666666666666666666666666666667d, 0.0000000000000001d)));
        assertEquals(SIUnits.CELSIUS, decResult.getUnit());
    }

    @Test
    public void testQuantityTypeOnStateUpdateFromHandlerDecimalOffset() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemOffsetProfile offsetProfile = createProfile(callback, "3");

        State state = new QuantityType<Temperature>("23 °C");
        offsetProfile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        State result = capture.getValue();
        @SuppressWarnings("unchecked")
        QuantityType<Temperature> decResult = (QuantityType<Temperature>) result;
        assertEquals(26, decResult.intValue());
        assertEquals(SIUnits.CELSIUS, decResult.getUnit());
    }

    private SystemOffsetProfile createProfile(ProfileCallback callback, String offset) {
        ProfileContext context = mock(ProfileContext.class);
        Configuration config = new Configuration();
        config.put("offset", offset);
        when(context.getConfiguration()).thenReturn(config);

        return new SystemOffsetProfile(callback, context);
    }
}
