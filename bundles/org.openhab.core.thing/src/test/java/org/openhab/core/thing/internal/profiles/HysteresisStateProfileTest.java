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
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.openhab.core.types.UnDefType;

/**
 * Basic unit tests for {@link HysteresisStateProfile}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class HysteresisStateProfileTest {

    private static final BigDecimal BIGDECIMAL_TEN = BigDecimal.valueOf(10);
    private static final BigDecimal BIGDECIMAL_FOURTY = BigDecimal.valueOf(40);
    private static final PercentType PERCENT_TYPE_TEN = new PercentType(BIGDECIMAL_TEN);
    private static final PercentType PERCENT_TYPE_TWENTY_FIVE = new PercentType(BigDecimal.valueOf(25));

    @NonNullByDefault
    public static class ParameterSet {
        public final List<State> states;
        public final List<State> resultingStates;
        public final List<Command> commands;
        public final List<@Nullable Command> resultingCommands;
        public final BigDecimal lower;
        public final @Nullable BigDecimal upper;

        public ParameterSet(List<? extends Type> sources, List<? extends Type> results, BigDecimal lower,
                @Nullable BigDecimal upper) {
            this.states = (List<State>) sources;
            this.resultingStates = (List<State>) results;
            this.commands = (List<Command>) sources;
            this.resultingCommands = new ArrayList<>(results.size());
            results.forEach(result -> {
                resultingCommands.add(result instanceof Command ? (Command) result : null);
            });
            this.lower = lower;
            this.upper = upper;
        }
    }

    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][] { //
                // lower bound = upper bound = 10, one state update / command
                { new ParameterSet(List.of(PercentType.HUNDRED), List.of(OnOffType.ON), BIGDECIMAL_TEN, null) }, //
                { new ParameterSet(List.of(PERCENT_TYPE_TWENTY_FIVE), List.of(OnOffType.ON), BIGDECIMAL_TEN, null) }, //
                { new ParameterSet(List.of(PERCENT_TYPE_TEN), List.of(OnOffType.OFF), BIGDECIMAL_TEN, null) }, //
                { new ParameterSet(List.of(PercentType.ZERO), List.of(OnOffType.OFF), BIGDECIMAL_TEN, null) }, //
                // lower bound = upper bound = 40, one state update / command
                { new ParameterSet(List.of(PercentType.HUNDRED), List.of(OnOffType.ON), BIGDECIMAL_FOURTY, null) }, //
                { new ParameterSet(List.of(new PercentType(BIGDECIMAL_FOURTY)), List.of(OnOffType.OFF),
                        BIGDECIMAL_FOURTY, null) }, //
                { new ParameterSet(List.of(PERCENT_TYPE_TWENTY_FIVE), List.of(OnOffType.OFF), BIGDECIMAL_FOURTY,
                        null) }, //
                { new ParameterSet(List.of(PercentType.ZERO), List.of(OnOffType.OFF), BIGDECIMAL_FOURTY, null) }, //
                // lower bound = 10; upper bound = 40, one state update / command
                { new ParameterSet(List.of(PercentType.HUNDRED), List.of(OnOffType.ON), BIGDECIMAL_TEN,
                        BIGDECIMAL_FOURTY) }, //
                { new ParameterSet(List.of(PERCENT_TYPE_TWENTY_FIVE), List.of(UnDefType.UNDEF), BIGDECIMAL_TEN,
                        BIGDECIMAL_FOURTY) }, //
                { new ParameterSet(List.of(PercentType.ZERO), List.of(OnOffType.OFF), BIGDECIMAL_TEN,
                        BIGDECIMAL_FOURTY) }, //
                // lower bound = 10; upper bound = 40, two state updates / commands results in changes
                { new ParameterSet(List.of(PercentType.HUNDRED, PercentType.HUNDRED),
                        List.of(OnOffType.ON, OnOffType.ON), BIGDECIMAL_TEN, BIGDECIMAL_FOURTY) }, //
                { new ParameterSet(List.of(PercentType.HUNDRED, PERCENT_TYPE_TWENTY_FIVE),
                        List.of(OnOffType.ON, OnOffType.ON), BIGDECIMAL_TEN, BIGDECIMAL_FOURTY) }, //
                { new ParameterSet(List.of(PercentType.HUNDRED, PercentType.ZERO), List.of(OnOffType.ON, OnOffType.OFF),
                        BIGDECIMAL_TEN, BIGDECIMAL_FOURTY) }, //
                // lower bound = 10; upper bound = 40, two state updates / commands results in changes
                { new ParameterSet(List.of(PERCENT_TYPE_TWENTY_FIVE, PercentType.HUNDRED),
                        List.of(UnDefType.UNDEF, OnOffType.ON), BIGDECIMAL_TEN, BIGDECIMAL_FOURTY) }, //
                { new ParameterSet(List.of(PERCENT_TYPE_TWENTY_FIVE, PERCENT_TYPE_TWENTY_FIVE),
                        List.of(UnDefType.UNDEF, UnDefType.UNDEF), BIGDECIMAL_TEN, BIGDECIMAL_FOURTY) }, //
                { new ParameterSet(List.of(PERCENT_TYPE_TWENTY_FIVE, PercentType.ZERO),
                        List.of(UnDefType.UNDEF, OnOffType.OFF), BIGDECIMAL_TEN, BIGDECIMAL_FOURTY) }, //
                // lower bound = 10; upper bound = 40, two state updates / commands results in changes
                { new ParameterSet(List.of(PercentType.ZERO, PercentType.HUNDRED), List.of(OnOffType.OFF, OnOffType.ON),
                        BIGDECIMAL_TEN, BIGDECIMAL_FOURTY) }, //
                { new ParameterSet(List.of(PercentType.ZERO, PERCENT_TYPE_TWENTY_FIVE),
                        List.of(OnOffType.OFF, OnOffType.OFF), BIGDECIMAL_TEN, BIGDECIMAL_FOURTY) }, //
                { new ParameterSet(List.of(PercentType.ZERO, PercentType.ZERO), List.of(OnOffType.OFF, OnOffType.OFF),
                        BIGDECIMAL_TEN, BIGDECIMAL_FOURTY) }, //
                // lower bound = 10; upper bound = 40, three state updates / commands -> anti-flapping
                { new ParameterSet(List.of(PercentType.HUNDRED, PERCENT_TYPE_TWENTY_FIVE, PercentType.HUNDRED),
                        List.of(OnOffType.ON, OnOffType.ON, OnOffType.ON), BIGDECIMAL_TEN, BIGDECIMAL_FOURTY) }, //
                { new ParameterSet(List.of(PercentType.ZERO, PERCENT_TYPE_TWENTY_FIVE, PercentType.ZERO),
                        List.of(OnOffType.OFF, OnOffType.OFF, OnOffType.OFF), BIGDECIMAL_TEN, BIGDECIMAL_FOURTY) } //
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

    @ParameterizedTest
    @MethodSource("parameters")
    public void testOnCommandFromHandler(ParameterSet parameterSet) {
        final StateProfile profile = initProfile(parameterSet.lower, parameterSet.upper);
        for (int i = 0; i < parameterSet.commands.size(); i++) {
            verifySendCommand(profile, parameterSet.commands.get(i), parameterSet.resultingCommands.get(i));
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testOnStateUpdateFromHandler(ParameterSet parameterSet) {
        final StateProfile profile = initProfile(parameterSet.lower, parameterSet.upper);
        for (int i = 0; i < parameterSet.states.size(); i++) {
            verifySendUpdate(profile, parameterSet.states.get(i), parameterSet.resultingStates.get(i));
        }
    }

    private StateProfile initProfile(BigDecimal lower, @Nullable BigDecimal upper) {
        final Map<String, @Nullable Object> properties = new HashMap<>(2);
        properties.put("lower", lower);
        properties.put("upper", upper);
        when(mockContext.getConfiguration()).thenReturn(new Configuration(properties));
        return new HysteresisStateProfile(mockCallback, mockContext);
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
