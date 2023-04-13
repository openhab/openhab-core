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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;

/**
 * Tests for the system:offset profile
 *
 * @author Stefan Triller - Initial contribution
 * @author Jimmy Tanagra - Refactor and add tests for multiplier
 */
@NonNullByDefault
public class SystemOffsetProfileTest {

    record TestParameters(@Nullable Object multiplier, @Nullable Object offset, boolean towardsItem, Type input,
            @Nullable Type expectedResult) {
    }

    public static DecimalType ONE = new DecimalType(1);

    public static Collection<TestParameters> identityParameters() {
        return List.of( //
                // basic identity checks that convert ONE to ONE
                new TestParameters(null, null, true, ONE, ONE), //
                new TestParameters(null, null, true, new DecimalType(10), new DecimalType(10)), //
                new TestParameters(null, null, true, new QuantityType<>("0 °C"), new QuantityType<>("0 °C")), //
                new TestParameters(null, null, false, ONE, ONE), //
                new TestParameters(1, null, false, ONE, ONE), //
                new TestParameters(1, null, true, ONE, ONE), //
                new TestParameters(1, DecimalType.ZERO, false, ONE, ONE), //
                new TestParameters(1, DecimalType.ZERO, true, ONE, ONE), //
                new TestParameters(null, DecimalType.ZERO, false, ONE, ONE), //
                new TestParameters(null, DecimalType.ZERO, true, ONE, ONE), //
                new TestParameters("1", "0", false, ONE, ONE), //
                new TestParameters("1", "0", true, ONE, ONE), //
                new TestParameters(1, 0, true, ONE, ONE), //

                new TestParameters(null, "", true, ONE, ONE), //

                new TestParameters(null, null, false, ONE, ONE) // dummy entry so I don't have to keep editing the comma
        );
    }

    public static Collection<TestParameters> decimalTypeParameters() {
        return List.of( //
                new TestParameters(5, 0, true, new DecimalType(7), new DecimalType(35)), //
                new TestParameters(5, 0, false, new DecimalType(35), new DecimalType(7)), //

                new TestParameters(0.1, 0, true, new DecimalType(100), new DecimalType(10)), //
                new TestParameters(0.1, 0, false, new DecimalType(10), new DecimalType(100)), //

                new TestParameters(-1, 0, true, new DecimalType(100), new DecimalType(-100)), //
                new TestParameters(-1, 0, false, new DecimalType(-100), new DecimalType(100)), //

                new TestParameters(1, 3, true, new DecimalType(23), new DecimalType(26)), //
                new TestParameters(1, 3, false, new DecimalType(26), new DecimalType(23)), //

                new TestParameters(1, -3, true, new DecimalType(23), new DecimalType(20)), //
                new TestParameters(1, -3, false, new DecimalType(20), new DecimalType(23)), //

                new TestParameters(5, 3, true, new DecimalType(23), new DecimalType(118)), //
                new TestParameters(5, 3, false, new DecimalType(118), new DecimalType(23)), //

                new TestParameters(null, null, false, ONE, ONE) // dummy entry so I don't have to keep editing the comma
        );
    }

    public static Collection<TestParameters> quantityTypeParameters() {
        return List.of( //
                new TestParameters(2, null, true, new QuantityType<>("23 °C"), new QuantityType<>("46 °C")), //
                new TestParameters(2, null, false, new QuantityType<>("46 °C"), new QuantityType<>("23 °C")), //

                new TestParameters(-2, null, true, new QuantityType<>("23 °C"), new QuantityType<>("-46 °C")), //
                new TestParameters(-2, null, false, new QuantityType<>("-46 °C"), new QuantityType<>("23 °C")), //

                new TestParameters(null, "3°C", true, new QuantityType<>("23°C"), new QuantityType<>("26°C")), //
                new TestParameters(null, "3°C", false, new QuantityType<>("26°C"), new QuantityType<>("23°C")), //

                new TestParameters(null, "-3°C", true, new QuantityType<>("23°C"), new QuantityType<>("20°C")), //
                new TestParameters(null, "-3°C", false, new QuantityType<>("20°C"), new QuantityType<>("23°C")), //

                new TestParameters("5", "3°C", true, new QuantityType<>("23 °C"), new QuantityType<>("118 °C")), //
                new TestParameters("5", "3°C", false, new QuantityType<>("118 °C"), new QuantityType<>("23 °C")), //

                // Offset in a different unit to the value
                new TestParameters(null, "9 °F", true, new QuantityType<>("23°C"), new QuantityType<>("28°C")), //
                new TestParameters(null, "9 °F", false, new QuantityType<>("28°C"), new QuantityType<>("23°C")), //
                new TestParameters(null, "1 kW", true, new QuantityType<>("120 W"), new QuantityType<>("1120 W")), //
                new TestParameters(null, "1 kW", false, new QuantityType<>("1120 W"), new QuantityType<>("120 W")), //

                // Non unit offset + value with unit
                new TestParameters(null, 3, true, new QuantityType<>("23°C"), new QuantityType<>("26°C")), //

                // Decimal offset, input is QuantityType with unit ONE
                new TestParameters(null, 3, true, new QuantityType<>(), new QuantityType<>("3")), //

                new TestParameters(null, null, false, ONE, ONE) // dummy entry so I don't have to keep editing the comma
        );
    }

    public static Collection<TestParameters> invalidParameters() {
        return List.of( //
                // Incompatible units
                new TestParameters(1, "1 °C", true, new QuantityType<>("5 W"), null), //
                new TestParameters(1, "1 °C", false, new QuantityType<>("5 W"), null), //

                // Incoming values are passed through when the parameters were invalid
                new TestParameters(1, "bar", true, ONE, ONE), //
                new TestParameters(1, "bar", true, new QuantityType<>("5 °F"), new QuantityType<>("5 °F")), //

                new TestParameters(null, null, false, ONE, ONE) // dummy entry so I don't have to keep editing the comma
        );
    }

    @ParameterizedTest
    @MethodSource("identityParameters")
    public void testIdentity(TestParameters parameters) {
        performTest(parameters);
    }

    @ParameterizedTest
    @MethodSource("decimalTypeParameters")
    public void testDecimalType(TestParameters parameters) {
        performTest(parameters);
    }

    @ParameterizedTest
    @MethodSource("quantityTypeParameters")
    public void testQuantityType(TestParameters parameters) {
        performTest(parameters);
    }

    @ParameterizedTest
    @MethodSource("invalidParameters")
    public void testInvalidParameters(TestParameters parameters) {
        performTest(parameters);
    }

    private void performTest(TestParameters parameters) {
        ProfileCallback callback = mock(ProfileCallback.class);
        SystemOffsetProfile offsetProfile = createProfile(callback, parameters.multiplier(), parameters.offset());

        if (parameters.towardsItem()) {
            performTestTowardsItem(callback, offsetProfile, parameters);
        } else {
            performTestFromItem(callback, offsetProfile, parameters);
        }
    }

    private void performTestTowardsItem(ProfileCallback callback, SystemOffsetProfile offsetProfile,
            TestParameters parameters) {
        offsetProfile.onCommandFromHandler((Command) parameters.input());
        if (parameters.expectedResult() == null) {
            verify(callback, never()).sendCommand(any());
        } else {
            ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
            verify(callback, times(1)).sendCommand(capture.capture());
            assertEqualValuesAndUnits(parameters.expectedResult(), capture.getValue());
        }

        offsetProfile.onStateUpdateFromHandler((State) parameters.input());
        if (parameters.expectedResult() == null) {
            verify(callback, never()).sendUpdate(any());
        } else {
            ArgumentCaptor<State> capturedState = ArgumentCaptor.forClass(State.class);
            verify(callback, times(1)).sendUpdate(capturedState.capture());
            assertEqualValuesAndUnits(parameters.expectedResult(), capturedState.getValue());
        }
    }

    private void performTestFromItem(ProfileCallback callback, SystemOffsetProfile offsetProfile,
            TestParameters parameters) {
        offsetProfile.onCommandFromItem((Command) parameters.input());
        if (parameters.expectedResult() == null) {
            verify(callback, never()).handleCommand(any());
        } else {
            ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
            verify(callback, times(1)).handleCommand(capture.capture());
            assertEqualValuesAndUnits(parameters.expectedResult(), capture.getValue());
        }
    }

    private SystemOffsetProfile createProfile(ProfileCallback callback, @Nullable Object multiplier,
            @Nullable Object offset) {
        ProfileContext context = mock(ProfileContext.class);
        Configuration config = new Configuration();
        if (offset != null) {
            config.put(SystemOffsetProfile.OFFSET_PARAM, offset);
        }
        if (multiplier != null) {
            config.put(SystemOffsetProfile.MULTIPLIER_PARAM, multiplier);
        }
        when(context.getConfiguration()).thenReturn(config);

        return new SystemOffsetProfile(callback, context);
    }

    private void assertEqualValuesAndUnits(Object expected, Object captured) {
        if (expected instanceof QuantityType qtyExpected) {
            assertThat(captured, instanceOf(QuantityType.class));
            QuantityType<?> qtyCaptured = (QuantityType<?>) captured;
            assertEquals(qtyExpected.toBigDecimal(), qtyCaptured.toBigDecimal());
            assertEquals(qtyExpected.getUnit(), qtyCaptured.getUnit());
        } else {
            assertEquals(expected, captured);
        }
    }
}
