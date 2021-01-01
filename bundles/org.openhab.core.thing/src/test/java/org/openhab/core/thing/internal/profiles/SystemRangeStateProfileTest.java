/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.openhab.core.types.UnDefType;

/**
 * Basic unit tests for {@link SystemRangeStateProfileTest}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class SystemRangeStateProfileTest {

    private static final String STRING_FOURTY = "40";
    private static final String QUANTITY_STRING_TEN = "10 %";
    private static final String QUANTITY_STRING_FOURTY = "40 %";
    private static final BigDecimal BIGDECIMAL_FOURTY = new BigDecimal(STRING_FOURTY);
    private static final PercentType PERCENT_TYPE_TEN = new PercentType(BigDecimal.TEN);
    private static final PercentType PERCENT_TYPE_TWENTY_FIVE = new PercentType(BigDecimal.valueOf(25));

    @NonNullByDefault
    public static class ParameterSet {
        public final List<State> states;
        public final List<State> resultingStates;
        public final List<Command> commands;
        public final List<@Nullable Command> resultingCommands;
        public final Object lower;
        public final @Nullable Object upper;
        public final boolean inverted;

        public ParameterSet(List<? extends Type> sources, List<? extends Type> results, Object lower,
                @Nullable Object upper, boolean inverted) {
            this.states = (List<State>) sources;
            this.resultingStates = (List<State>) results;
            this.commands = (List<Command>) sources;
            this.resultingCommands = new ArrayList<>(results.size());
            results.forEach(result -> {
                resultingCommands.add(result instanceof Command ? (Command) result : null);
            });
            this.lower = lower;
            this.upper = upper;
            this.inverted = inverted;
        }
    }

    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][] { //
                // lower bound = 10, upper bound = 40 (as BigDecimal), one state update / command (PercentType), not
                // inverted
                { new ParameterSet(List.of(PercentType.HUNDRED), List.of(OnOffType.OFF), BigDecimal.TEN,
                        BIGDECIMAL_FOURTY, false) }, //
                { new ParameterSet(List.of(PERCENT_TYPE_TWENTY_FIVE), List.of(OnOffType.ON), BigDecimal.TEN,
                        BIGDECIMAL_FOURTY, false) }, //
                { new ParameterSet(List.of(PERCENT_TYPE_TEN), List.of(OnOffType.ON), BigDecimal.TEN, BIGDECIMAL_FOURTY,
                        false) }, //
                { new ParameterSet(List.of(PercentType.ZERO), List.of(OnOffType.OFF), BigDecimal.TEN, BIGDECIMAL_FOURTY,
                        false) }, //
                // lower bound = 10, upper bound = 40 (as BigDecimal), one state update / command (QuantityType), not
                // inverted
                { new ParameterSet(List.of(QuantityType.valueOf("100 %")), List.of(OnOffType.OFF), BigDecimal.TEN,
                        BIGDECIMAL_FOURTY, false) }, //
                { new ParameterSet(List.of(QuantityType.valueOf("25 %")), List.of(OnOffType.ON), BigDecimal.TEN,
                        BIGDECIMAL_FOURTY, false) }, //
                { new ParameterSet(List.of(QuantityType.valueOf(QUANTITY_STRING_TEN)), List.of(OnOffType.ON),
                        BigDecimal.TEN, BIGDECIMAL_FOURTY, false) }, //
                { new ParameterSet(List.of(QuantityType.valueOf("0 %")), List.of(OnOffType.OFF), BigDecimal.TEN,
                        BIGDECIMAL_FOURTY, false) }, //
                // lower bound = 10, upper bound = 40 (as QuantityType), one state update / command (QuantityType), not
                // inverted
                { new ParameterSet(List.of(QuantityType.valueOf("100 %")), List.of(OnOffType.OFF), QUANTITY_STRING_TEN,
                        QUANTITY_STRING_FOURTY, false) }, //
                { new ParameterSet(List.of(QuantityType.valueOf("25 %")), List.of(OnOffType.ON), QUANTITY_STRING_TEN,
                        QUANTITY_STRING_FOURTY, false) }, //
                { new ParameterSet(List.of(QuantityType.valueOf(QUANTITY_STRING_TEN)), List.of(OnOffType.ON),
                        QUANTITY_STRING_TEN, QUANTITY_STRING_FOURTY, false) }, //
                { new ParameterSet(List.of(QuantityType.valueOf("0 %")), List.of(OnOffType.OFF), QUANTITY_STRING_TEN,
                        QUANTITY_STRING_FOURTY, false) }, //
                // lower bound = 10, upper bound = 40 (as QuantityType), one state update / command (QuantityType) ->
                // values
                // are converted to the same unit, not inverted
                { new ParameterSet(List.of(QuantityType.valueOf("10 m")), List.of(OnOffType.OFF), "25 cm", "100cm",
                        false) }, //
                // lower bound = upper bound = 10 (as QuantityType), one state update / command (QuantityType) ->
                // incompatible units cannot be compared
                { new ParameterSet(List.of(QuantityType.valueOf("10 m")), List.of(UnDefType.UNDEF), "25 °C", "30 °C",
                        false) }, //
                // lower bound = 10, upper bound = 40 (as BigDecimal), one state update / command (PercentType),
                // inverted
                { new ParameterSet(List.of(PercentType.HUNDRED), List.of(OnOffType.ON), BigDecimal.TEN,
                        BIGDECIMAL_FOURTY, true) }, //
                { new ParameterSet(List.of(PERCENT_TYPE_TWENTY_FIVE), List.of(OnOffType.OFF), BigDecimal.TEN,
                        BIGDECIMAL_FOURTY, true) }, //
                { new ParameterSet(List.of(PERCENT_TYPE_TEN), List.of(OnOffType.OFF), BigDecimal.TEN, BIGDECIMAL_FOURTY,
                        true) }, //
                { new ParameterSet(List.of(PercentType.ZERO), List.of(OnOffType.ON), BigDecimal.TEN, BIGDECIMAL_FOURTY,
                        true) }, //
                // lower bound = 10, upper bound = 40 (as BigDecimal), one state update / command (QuantityType),
                // inverted
                { new ParameterSet(List.of(QuantityType.valueOf("100 %")), List.of(OnOffType.ON), BigDecimal.TEN,
                        BIGDECIMAL_FOURTY, true) }, //
                { new ParameterSet(List.of(QuantityType.valueOf("25 %")), List.of(OnOffType.OFF), BigDecimal.TEN,
                        BIGDECIMAL_FOURTY, true) }, //
                { new ParameterSet(List.of(QuantityType.valueOf(QUANTITY_STRING_TEN)), List.of(OnOffType.OFF),
                        BigDecimal.TEN, BIGDECIMAL_FOURTY, true) }, //
                { new ParameterSet(List.of(QuantityType.valueOf("0 %")), List.of(OnOffType.ON), BigDecimal.TEN,
                        BIGDECIMAL_FOURTY, true) }, //
                // lower bound = 10, upper bound = 40 (as QuantityType), one state update / command (QuantityType),
                // inverted
                { new ParameterSet(List.of(QuantityType.valueOf("100 %")), List.of(OnOffType.ON), QUANTITY_STRING_TEN,
                        QUANTITY_STRING_FOURTY, true) }, //
                { new ParameterSet(List.of(QuantityType.valueOf("25 %")), List.of(OnOffType.OFF), QUANTITY_STRING_TEN,
                        QUANTITY_STRING_FOURTY, true) }, //
                { new ParameterSet(List.of(QuantityType.valueOf(QUANTITY_STRING_TEN)), List.of(OnOffType.OFF),
                        QUANTITY_STRING_TEN, QUANTITY_STRING_FOURTY, true) }, //
                { new ParameterSet(List.of(QuantityType.valueOf("0 %")), List.of(OnOffType.ON), QUANTITY_STRING_TEN,
                        QUANTITY_STRING_FOURTY, true) }, //
        });
    }

    private AutoCloseable mocksCloseable;

    private @Mock ProfileCallback mockCallback;
    private @Mock ProfileContext mockContext;

    @BeforeEach
    public void setup() {
        mocksCloseable = openMocks(this);
    }

    @AfterEach
    public void afterEach() throws Exception {
        mocksCloseable.close();
    }

    @Test
    public void testWrongParameterLower() {
        assertThrows(IllegalArgumentException.class, () -> initProfile(null, null));
    }

    @Test
    public void testWrongParameterUpper() {
        assertThrows(IllegalArgumentException.class, () -> initProfile(QUANTITY_STRING_TEN, null));
    }

    @Test
    public void testWrongParameterUnits() {
        assertThrows(IllegalArgumentException.class, () -> initProfile(QUANTITY_STRING_TEN, "5 °C"));
    }

    @Test
    public void testWrongParameterUpperLessThanOrEqualsToLower() {
        assertThrows(IllegalArgumentException.class, () -> initProfile(QUANTITY_STRING_FOURTY, QUANTITY_STRING_TEN));
        assertThrows(IllegalArgumentException.class, () -> initProfile(QUANTITY_STRING_FOURTY, QUANTITY_STRING_FOURTY));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testOnCommandFromHandler(ParameterSet parameterSet) {
        final StateProfile profile = initProfile(parameterSet.lower, parameterSet.upper, parameterSet.inverted);
        for (int i = 0; i < parameterSet.commands.size(); i++) {
            verifySendCommand(profile, parameterSet.commands.get(i), parameterSet.resultingCommands.get(i));
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testOnStateUpdateFromHandler(ParameterSet parameterSet) {
        final StateProfile profile = initProfile(parameterSet.lower, parameterSet.upper, parameterSet.inverted);
        for (int i = 0; i < parameterSet.states.size(); i++) {
            verifySendUpdate(profile, parameterSet.states.get(i), parameterSet.resultingStates.get(i));
        }
    }

    private StateProfile initProfile(Object lower, Object upper) {
        return initProfile(lower, upper, false);
    }

    private StateProfile initProfile(Object lower, Object upper, boolean inverted) {
        final Map<String, @Nullable Object> properties = new HashMap<>(2);
        properties.put(SystemRangeStateProfile.LOWER_PARAM, lower);
        properties.put(SystemRangeStateProfile.UPPER_PARAM, upper);
        properties.put(SystemRangeStateProfile.INVERTED_PARAM, inverted);
        when(mockContext.getConfiguration()).thenReturn(new Configuration(properties));
        return new SystemRangeStateProfile(mockCallback, mockContext);
    }

    private void verifySendCommand(StateProfile profile, Command command, @Nullable Command expectedCommand) {
        reset(mockCallback);
        profile.onCommandFromHandler(command);
        Command eC = expectedCommand;
        if (eC == null) {
            verifyNoInteractions(mockCallback);
        } else {
            verify(mockCallback, times(1)).sendCommand(eq(eC));
        }
    }

    private void verifySendUpdate(StateProfile profile, State state, State expectedState) {
        reset(mockCallback);
        profile.onStateUpdateFromHandler(state);
        verify(mockCallback, times(1)).sendUpdate(eq(expectedState));
    }
}
