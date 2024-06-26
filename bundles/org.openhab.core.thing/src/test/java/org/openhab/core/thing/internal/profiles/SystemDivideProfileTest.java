/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

import javax.measure.Unit;
import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

/**
 * Tests for the system:divide profile
 *
 * @author John Cocula - Initial contribution closely based on SystemOffsetProfileTest
 */
@NonNullByDefault
public class SystemDivideProfileTest {

    @BeforeEach
    public void setup() {
        // initialize parser with ImperialUnits, otherwise units like °F are unknown
        @SuppressWarnings("unused")
        Unit<Temperature> fahrenheit = ImperialUnits.FAHRENHEIT;
    }

    @Test
    public void testDecimalTypeOnCommandFromItem() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemDivideProfile divideProfile = createProfile(callback, "3");

        Command cmd = new DecimalType(21);
        divideProfile.onCommandFromItem(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).handleCommand(capture.capture());

        Command result = capture.getValue();
        DecimalType decResult = (DecimalType) result;
        assertEquals(63, decResult.intValue());
    }

    @Test
    public void testDecimalTypeOnCommandFromHandler() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemDivideProfile divideProfile = createProfile(callback, "3");

        Command cmd = new DecimalType(21);
        divideProfile.onCommandFromHandler(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).sendCommand(capture.capture());

        Command result = capture.getValue();
        DecimalType decResult = (DecimalType) result;
        assertEquals(7, decResult.intValue());
    }

    @Test
    public void testDecimalTypeOnStateUpdateFromHandler() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemDivideProfile divideProfile = createProfile(callback, "3");

        State state = new DecimalType(21);
        divideProfile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        State result = capture.getValue();
        DecimalType decResult = (DecimalType) result;
        assertEquals(7, decResult.intValue());
    }

    @Test
    public void testQuantityTypeOnCommandFromHandler() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemDivideProfile divideProfile = createProfile(callback, "3");

        Command cmd = new QuantityType<>("21 m");
        divideProfile.onCommandFromHandler(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).sendCommand(capture.capture());

        Command result = capture.getValue();
        QuantityType<?> decResult = (QuantityType<?>) result;
        assertEquals(7, decResult.intValue());
        assertEquals(SIUnits.METRE, decResult.getUnit());
    }

    @Test
    public void testQuantityTypeOnStateUpdateFromHandler() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemDivideProfile divideProfile = createProfile(callback, "3");

        State state = new QuantityType<>("21 V");
        divideProfile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        State result = capture.getValue();
        QuantityType<?> decResult = (QuantityType<?>) result;
        assertEquals(7, decResult.intValue());
        assertEquals(Units.VOLT, decResult.getUnit());
    }

    @Test
    public void testQuantityTypeWithUnitCelsiusOnStateUpdateFromHandlerDecimalDivide() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemDivideProfile divideProfile = createProfile(callback, "3");

        State state = new QuantityType<>("21 kWh");
        divideProfile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        State result = capture.getValue();
        QuantityType<?> decResult = (QuantityType<?>) result;
        assertEquals(7, decResult.intValue());
        assertEquals(Units.KILOWATT_HOUR, decResult.getUnit());
    }

    @Test
    public void testQuantityTypeWithUnitOneOnStateUpdateFromHandlerDecimalDivide() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemDivideProfile divideProfile = createProfile(callback, "3");

        State state = new QuantityType<>();
        divideProfile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        State result = capture.getValue();
        QuantityType<?> decResult = (QuantityType<?>) result;
        assertEquals(0, decResult.intValue());
        assertEquals(Units.ONE, decResult.getUnit());
    }

    private SystemDivideProfile createProfile(ProfileCallback callback, String divisor) {
        ProfileContext context = mock(ProfileContext.class);
        Configuration config = new Configuration();
        config.put(SystemDivideProfile.DIVISOR_PARAM, divisor);
        when(context.getConfiguration()).thenReturn(config);

        return new SystemDivideProfile(callback, context);
    }
}
