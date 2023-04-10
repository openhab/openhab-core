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

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;

/**
 * Basic unit tests for {@link SystemRangeFilterProfile}.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class SystemRangeFilterProfileTest {

    private record TestParameter(@Nullable String range, boolean inverted, Map<? extends Type, Boolean> tests) {
    };

    private record RangeTestParameter(@Nullable String range, boolean isValid) {
    };

    public static List<TestParameter> singleRangeParameters() {
        return List.of( //
                // inclusive begin and end
                new TestParameter("[1..2]", false, Map.of( //
                        new DecimalType(0), false, //
                        new DecimalType(1), true, // included
                        new DecimalType(1.5), true, //
                        new DecimalType(2), true, // included
                        new DecimalType(2.1), false, //
                        new DecimalType(3), false, //
                        new DecimalType(-1), false //
                )), //
                new TestParameter("[30..70]", false, Map.of( //
                        new PercentType(0), false, //
                        new PercentType(29), false, //
                        new PercentType(30), true, //
                        new PercentType(50), true, //
                        new PercentType(70), true, //
                        new PercentType(71), false, //
                        new PercentType(100), false //
                )), //
                // exclusive begin, inclusive end
                new TestParameter("(1..2]", false, Map.of( //
                        new DecimalType(0), false, //
                        new DecimalType(1), false, // excluded
                        new DecimalType(1.5), true, //
                        new DecimalType(2), true, // included
                        new DecimalType(2.1), false //
                )), //
                // inclusive begin, exclusive end
                new TestParameter("[1..2)", false, Map.of( //
                        new DecimalType(0), false, //
                        new DecimalType(1), true, // included
                        new DecimalType(1.5), true, //
                        new DecimalType(2), false, // excluded
                        new DecimalType(2.1), false //
                )), //
                // exclusive begin and end
                new TestParameter("(1..2)", false, Map.of( //
                        new DecimalType(0), false, //
                        new DecimalType(1), false, // excluded
                        new DecimalType(1.5), true, //
                        new DecimalType(2), false, // excluded
                        new DecimalType(2.1), false //
                )), //
                // beginless
                new TestParameter("(..2)", false, Map.of( //
                        new DecimalType(-100), true, //
                        new DecimalType(0), true, //
                        new DecimalType(1), true, //
                        new DecimalType(1.5), true, //
                        new DecimalType(2), false, // excluded
                        new DecimalType(2.1), false //
                )), //
                // endless
                new TestParameter("(10..)", false, Map.of( //
                        new DecimalType(-100), false, //
                        new DecimalType(0), false, //
                        new DecimalType(10), false, //
                        new DecimalType(15), true, //
                        new DecimalType(100), true //
                )), //
                // unitless range will compare the input values regardless of their unit
                new TestParameter("[1..2]", false, Map.of( //
                        new QuantityType<>("0.9 °C"), false, //
                        new QuantityType<>("1 °C"), true, //
                        new QuantityType<>("1.5 °C"), true, //
                        new QuantityType<>("2 °C"), true, //
                        new QuantityType<>("3 °C"), false, //
                        new QuantityType<>("0.9 W"), false, //
                        new QuantityType<>("1 W"), true, //
                        new QuantityType<>("1.5 W"), true, //
                        new QuantityType<>("2 W"), true, //
                        new QuantityType<>("3 W"), false //
                )), // Test values in different unit
                new TestParameter("[1 °C..2 °C]", false, Map.of( //
                        new QuantityType<>("0.9 °C"), false, //
                        new QuantityType<>("1 °C"), true, //
                        new QuantityType<>("1.5 °C"), true, //
                        new QuantityType<>("2 °C"), true, //
                        new QuantityType<>("3 °C"), false, //
                        new QuantityType<>("1 °F"), false, //
                        new QuantityType<>("2 °F"), false, //
                        new QuantityType<>("34 °F"), true, //
                        new QuantityType<>("36 °F"), false //
                )), // Test mixing units in the range limits
                new TestParameter("[1 °C..35.6 °F]", false, Map.of( //
                        new QuantityType<>("0.9 °C"), false, //
                        new QuantityType<>("1 °C"), true, //
                        new QuantityType<>("1.5 °C"), true, //
                        new QuantityType<>("2 °C"), true, //
                        new QuantityType<>("3 °C"), false, //
                        new QuantityType<>("1 °F"), false, //
                        new QuantityType<>("2 °F"), false, //
                        new QuantityType<>("34 °F"), true, //
                        new QuantityType<>("35 °F"), true, //
                        new QuantityType<>("36 °F"), false //
                )), // Test values with incompatible unit
                new TestParameter("[1 °C..10 °C]", false, Map.of( //
                        new DecimalType(1), false, //
                        new QuantityType<>("5 W"), false //
                ))//
        );
    }

    public static List<TestParameter> multipleRangeParameters() {
        return List.of( //
                new TestParameter("[1..2],[10..20]", false, Map.of( //
                        new DecimalType(0), false, //
                        new DecimalType(1), true, //
                        new DecimalType(1.5), true, //
                        new DecimalType(2), true, //
                        new DecimalType(3), false, //
                        new DecimalType(10), true, //
                        new DecimalType(15), true, //
                        new DecimalType(20), true, //
                        new DecimalType(21), false //
                )), //
                new TestParameter("(1..2],[10..20]", false, Map.of( //
                        new DecimalType(0), false, //
                        new DecimalType(1), false, //
                        new DecimalType(1.5), true, //
                        new DecimalType(2), true, //
                        new DecimalType(3), false, //
                        new DecimalType(10), true, //
                        new DecimalType(15), true, //
                        new DecimalType(20), true, //
                        new DecimalType(21), false //
                )), //
                new TestParameter("[1..2),[10..20)", false, Map.of( //
                        new DecimalType(0), false, //
                        new DecimalType(1), true, //
                        new DecimalType(1.5), true, //
                        new DecimalType(2), false, //
                        new DecimalType(3), false, //
                        new DecimalType(10), true, //
                        new DecimalType(15), true, //
                        new DecimalType(20), false, //
                        new DecimalType(21), false //
                )), //
                new TestParameter("[1..2),[10..20],[50..60]", false, Map.ofEntries( //
                        entry(new DecimalType(0), false), //
                        entry(new DecimalType(1), true), //
                        entry(new DecimalType(1.5), true), //
                        entry(new DecimalType(2), false), //
                        entry(new DecimalType(3), false), //
                        entry(new DecimalType(10), true), //
                        entry(new DecimalType(15), true), //
                        entry(new DecimalType(20), true), //
                        entry(new DecimalType(30), false), //
                        entry(new DecimalType(50), true), //
                        entry(new DecimalType(51), true), //
                        entry(new DecimalType(60), true), //
                        entry(new DecimalType(61), false) //
                )) //
        );
    }

    public static List<TestParameter> invertedRangeParameters() {
        return List.of( //
                new TestParameter("[1..2]", true, Map.of( //
                        new DecimalType(0), true, //
                        new DecimalType(1), false, //
                        new DecimalType(1.5), false, //
                        new DecimalType(2), false, //
                        new DecimalType(2.1), true, //
                        new DecimalType(3), true, //
                        new DecimalType(-1), true //
                )), //
                new TestParameter("(1..2]", true, Map.of( //
                        new DecimalType(0), true, //
                        new DecimalType(1), true, //
                        new DecimalType(1.5), false, //
                        new DecimalType(2), false, //
                        new DecimalType(2.1), true //
                )), //
                new TestParameter("[1..2)", true, Map.of( //
                        new DecimalType(0), true, //
                        new DecimalType(1), false, //
                        new DecimalType(1.5), false, //
                        new DecimalType(2), true, //
                        new DecimalType(2.1), true //
                )), //
                // exclusive begin and end
                new TestParameter("(1..2)", true, Map.of( //
                        new DecimalType(0), true, //
                        new DecimalType(1), true, //
                        new DecimalType(1.5), false, //
                        new DecimalType(2), true, //
                        new DecimalType(2.1), true //
                )), //
                // beginless
                new TestParameter("(..2)", true, Map.of( //
                        new DecimalType(-100), false, //
                        new DecimalType(0), false, //
                        new DecimalType(1), false, //
                        new DecimalType(1.5), false, //
                        new DecimalType(2), true, //
                        new DecimalType(2.1), true //
                )), //
                // endless
                new TestParameter("(10..)", true, Map.of( //
                        new DecimalType(-100), true, //
                        new DecimalType(0), true, //
                        new DecimalType(10), true, //
                        new DecimalType(15), false, //
                        new DecimalType(100), false //
                )), //
                new TestParameter("[1..2),[10..20],[50..60]", true, Map.ofEntries( //
                        entry(new DecimalType(0), true), //
                        entry(new DecimalType(1), false), //
                        entry(new DecimalType(1.5), false), //
                        entry(new DecimalType(2), true), //
                        entry(new DecimalType(3), true), //
                        entry(new DecimalType(10), false), //
                        entry(new DecimalType(15), false), //
                        entry(new DecimalType(20), false), //
                        entry(new DecimalType(30), true), //
                        entry(new DecimalType(50), false), //
                        entry(new DecimalType(51), false), //
                        entry(new DecimalType(60), false), //
                        entry(new DecimalType(61), true) //
                )) //
        );
    }

    public static List<RangeTestParameter> syntaxTestParameters() {
        return List.of( //
                new RangeTestParameter(" [1..2]", true), //
                new RangeTestParameter("[1..2] ", true), //
                new RangeTestParameter(" [1..2] ", true), //
                new RangeTestParameter("[ 1..2]", true), //
                new RangeTestParameter("[1 ..2]", true), //
                new RangeTestParameter("[1.. 2]", true), //
                new RangeTestParameter("[1..2 ]", true), //
                new RangeTestParameter(" [ 1 .. 2 ] ", true), //
                new RangeTestParameter("[1..2][3..4]", false), //
                new RangeTestParameter("[1..2] [3..4]", false), //
                new RangeTestParameter("[1..2],[3..4]", true), //
                new RangeTestParameter("[1..2], [3..4]", true), //
                new RangeTestParameter("[1..2] ,[3..4]", true), //
                new RangeTestParameter("[1..2] , [3..4]", true), //
                new RangeTestParameter(" [1..2] , [3..4 ] ", true), //
                new RangeTestParameter("[1..2], [3..4], [5..6],[7..8]", true), //
                new RangeTestParameter("[1..2], [3..4], [5..6]|7...8]", false), //
                new RangeTestParameter("", false), //
                new RangeTestParameter("asdfafd", false), //
                new RangeTestParameter("[foo..3]", false), //
                new RangeTestParameter("[1..foo]", false), //
                new RangeTestParameter("[foo..bar]", false), //
                new RangeTestParameter("[1 kW..2 kW]", true) //
        );
    }

    public static List<RangeTestParameter> unitCompatibilityTestParameters() {
        return List.of( //
                new RangeTestParameter("[1 °C..200 °F]", true), //
                new RangeTestParameter("[10 psi..300 kPa]", true), //
                new RangeTestParameter("[1 kW..2 kW]", true), //
                new RangeTestParameter("[1 kW..2000 W]", true), //
                new RangeTestParameter("[1 kW..2]", false), //
                new RangeTestParameter("[1..2 kW]", false), //
                new RangeTestParameter("[1 kW..2 °C]", false) //
        );
    }

    public static List<RangeTestParameter> boundaryTestParameters() {
        return List.of( //
                new RangeTestParameter("[1 °C..2 °F]", false), // 2 °F is smaller than 1 °C
                new RangeTestParameter("[1..1]", true), //
                new RangeTestParameter("(1..1]", false), //
                new RangeTestParameter("[1..1)", false), //
                new RangeTestParameter("(1..1)", false) //
        );
    }

    private @Mock(answer = Answers.RETURNS_DEEP_STUBS) @NonNullByDefault({}) ProfileCallback callbackMock;
    private @Mock @NonNullByDefault({}) ProfileContext contextMock;
    private ItemChannelLink itemChannelLink = new ItemChannelLink("Item", new ChannelUID("thing:test:channel:uid"));

    @ParameterizedTest
    @MethodSource("singleRangeParameters")
    public void testOnStateUpdateFromHandlerWithSingleRange(TestParameter testParameter) {
        final StateProfile profile = initProfile(testParameter.range(), testParameter.inverted());
        testParameter.tests().forEach((input, passedThrough) -> {
            verifySendUpdate(profile, (State) input, passedThrough);
        });
    }

    @ParameterizedTest
    @MethodSource("singleRangeParameters")
    public void testOnCommandFromHandlerWithSingleRange(TestParameter testParameter) {
        final StateProfile profile = initProfile(testParameter.range(), testParameter.inverted());
        testParameter.tests().forEach((input, passedThrough) -> {
            verifySendCommand(profile, (Command) input, passedThrough);
        });
    }

    @ParameterizedTest
    @MethodSource("multipleRangeParameters")
    public void testOnCommandFromHandlerWithMultipleRanges(TestParameter testParameter) {
        final StateProfile profile = initProfile(testParameter.range(), testParameter.inverted());
        testParameter.tests().forEach((input, passedThrough) -> {
            verifySendCommand(profile, (Command) input, passedThrough);
        });
    }

    @ParameterizedTest
    @MethodSource("invertedRangeParameters")
    public void testOnCommandFromHandlerWithInvertedRanges(TestParameter testParameter) {
        final StateProfile profile = initProfile(testParameter.range(), testParameter.inverted());
        testParameter.tests().forEach((input, passedThrough) -> {
            verifySendCommand(profile, (Command) input, passedThrough);
        });
    }

    @ParameterizedTest
    @MethodSource("syntaxTestParameters")
    public void testRangeSyntax(RangeTestParameter testParameter) {
        performRangeTest(testParameter);
    }

    @ParameterizedTest
    @MethodSource("unitCompatibilityTestParameters")
    public void testRangeUnitCompatibilityCheck(RangeTestParameter testParameter) {
        performRangeTest(testParameter);
    }

    @ParameterizedTest
    @MethodSource("boundaryTestParameters")
    public void testRangeBoundaryCheck(RangeTestParameter testParameter) {
        performRangeTest(testParameter);
    }

    private void performRangeTest(RangeTestParameter testParameter) {
        if (testParameter.isValid()) {
            assertDoesNotThrow(() -> initProfile(testParameter.range(), false));
        } else {
            assertThrows(IllegalArgumentException.class, () -> initProfile(testParameter.range(), false));
        }
    }

    private StateProfile initProfile(@Nullable String range, boolean inverted) {
        final Map<String, @Nullable Object> properties = new HashMap<>(2);
        properties.put(SystemRangeFilterProfile.RANGE_PARAM, range);
        properties.put(SystemRangeFilterProfile.RANGE_ACTION_PARAM,
                inverted ? SystemRangeFilterProfile.RANGE_ACTION_DISCARD : SystemRangeFilterProfile.RANGE_ACTION_ALLOW);
        when(contextMock.getConfiguration()).thenReturn(new Configuration(properties));
        lenient().when(callbackMock.getItemChannelLink()).thenReturn(itemChannelLink);
        return new SystemRangeFilterProfile(callbackMock, contextMock);
    }

    private void verifySendCommand(StateProfile profile, Command command, boolean passedThrough) {
        reset(callbackMock);
        profile.onCommandFromHandler(command);
        if (passedThrough) {
            verify(callbackMock, times(1)).sendCommand(eq(command));
        } else {
            verify(callbackMock, never()).sendCommand(eq(command));
        }
    }

    private void verifySendUpdate(StateProfile profile, State state, boolean passedThrough) {
        reset(callbackMock);
        profile.onStateUpdateFromHandler(state);
        if (passedThrough) {
            verify(callbackMock, times(1)).sendUpdate(eq(state));
        } else {
            verify(callbackMock, never()).sendUpdate(eq(state));
        }
    }
}
