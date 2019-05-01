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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
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
 * Tests for the system:round profile
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class SystemRoundProfileTest {

    @Before
    public void setup() {
        // initialize parser with ImperialUnits, otherwise units like °F are unknown
        @SuppressWarnings("unused")
        Unit<Temperature> fahrenheit = ImperialUnits.FAHRENHEIT;
    }

    @Test
    public void testDecimalTypeOnCommandFromItem() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemRoundProfile roundProfile = createProfile(callback, 2, "HALF_UP");

        Command cmd = new DecimalType(23.333);
        roundProfile.onCommandFromItem(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).handleCommand(capture.capture());

        Command result = capture.getValue();
        DecimalType dtResult = (DecimalType) result;
        assertThat(dtResult.doubleValue(), is(23.33));
    }

    @Test
    public void testDecimalTypeOnCommandFromItemWithCeiling() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemRoundProfile roundProfile = createProfile(callback, 0, "CEILING");

        Command cmd = new DecimalType(23.3);
        roundProfile.onCommandFromItem(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).handleCommand(capture.capture());

        Command result = capture.getValue();
        DecimalType dtResult = (DecimalType) result;
        assertThat(dtResult.doubleValue(), is(24.0));
    }

    @Test
    public void testDecimalTypeOnCommandFromItemWithFloor() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemRoundProfile roundProfile = createProfile(callback, 0, "FLOOR");

        Command cmd = new DecimalType(23.6);
        roundProfile.onCommandFromItem(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).handleCommand(capture.capture());

        Command result = capture.getValue();
        DecimalType dtResult = (DecimalType) result;
        assertThat(dtResult.doubleValue(), is(23.0));
    }

    @Test
    public void testDecimalTypeOnStateUpdateFromItem() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemRoundProfile roundProfile = createProfile(callback, 2, "HALF_UP");

        State state = new DecimalType(23.666);
        roundProfile.onStateUpdateFromItem(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).handleUpdate(capture.capture());

        State result = capture.getValue();
        DecimalType dtResult = (DecimalType) result;
        assertThat(dtResult.doubleValue(), is(23.67));
    }

    @Test
    public void testQuantityTypeOnCommandFromItem() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemRoundProfile roundProfile = createProfile(callback, 1, "HALF_UP");

        Command cmd = new QuantityType<Temperature>("23.333 °C");
        roundProfile.onCommandFromItem(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).handleCommand(capture.capture());

        Command result = capture.getValue();
        @SuppressWarnings("unchecked")
        QuantityType<Temperature> qtResult = (QuantityType<Temperature>) result;
        assertThat(qtResult.doubleValue(), is(23.3));
        assertThat(qtResult.getUnit(), is(SIUnits.CELSIUS));
    }

    @Test
    public void testQuantityTypeOnStateUpdateFromItem() {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemRoundProfile roundProfile = createProfile(callback, 1, "HALF_UP");

        State state = new QuantityType<Temperature>("23.666 °C");
        roundProfile.onStateUpdateFromItem(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).handleUpdate(capture.capture());

        State result = capture.getValue();
        @SuppressWarnings("unchecked")
        QuantityType<Temperature> qtResult = (QuantityType<Temperature>) result;
        assertThat(qtResult.doubleValue(), is(23.7));
        assertThat(qtResult.getUnit(), is(SIUnits.CELSIUS));
    }

    private SystemRoundProfile createProfile(ProfileCallback callback, Integer scale, String mode) {
        ProfileContext context = mock(ProfileContext.class);
        Configuration config = new Configuration();
        config.put("scale", scale);
        config.put("mode", mode);
        when(context.getConfiguration()).thenReturn(config);

        return new SystemRoundProfile(callback, context);
    }
}
