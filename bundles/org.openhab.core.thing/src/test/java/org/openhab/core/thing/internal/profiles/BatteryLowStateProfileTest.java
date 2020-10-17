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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

/**
 * Basic unit tests for {@link BatteryLowStateProfile}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class BatteryLowStateProfileTest {

    public static class ParameterSet {
        public State state;
        public State resultingState;
        public Command command;
        public Command resultingCommand;
        public int treshold;

        public ParameterSet(Type source, Type result, int treshold) {
            this.state = (State) source;
            this.resultingState = (State) result;
            this.command = (Command) source;
            this.resultingCommand = (Command) result;
            this.treshold = treshold;
        }
    }

    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][] { //
                { new ParameterSet(PercentType.HUNDRED, OnOffType.OFF, 10) }, //
                { new ParameterSet(new PercentType(BigDecimal.valueOf(25)), OnOffType.OFF, 10) }, //
                { new ParameterSet(PercentType.ZERO, OnOffType.ON, 10) }, //
                { new ParameterSet(PercentType.HUNDRED, OnOffType.OFF, 40) }, //
                { new ParameterSet(new PercentType(BigDecimal.valueOf(25)), OnOffType.ON, 40) }, //
                { new ParameterSet(PercentType.ZERO, OnOffType.ON, 40) } //
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

    @ParameterizedTest
    @MethodSource("parameters")
    public void testOnCommandFromHandler(ParameterSet parameterSet) {
        final StateProfile profile = initProfile(parameterSet.treshold);
        verifySendCommand(profile, parameterSet.command, parameterSet.resultingCommand);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testOnStateUpdateFromHandler(ParameterSet parameterSet) {
        final StateProfile profile = initProfile(parameterSet.treshold);
        verifySendUpdate(profile, parameterSet.state, parameterSet.resultingState);
    }

    private StateProfile initProfile(int threshold) {
        when(mockContext.getConfiguration()).thenReturn(new Configuration(Map.of("threshold", threshold)));
        return new BatteryLowStateProfile(mockCallback, mockContext);
    }

    private void verifySendCommand(StateProfile profile, Command command, Command result) {
        reset(mockCallback);
        profile.onCommandFromHandler(command);
        verify(mockCallback, times(1)).sendCommand(eq(result));
    }

    private void verifySendUpdate(StateProfile profile, State state, State result) {
        reset(mockCallback);
        profile.onStateUpdateFromHandler(state);
        verify(mockCallback, times(1)).sendUpdate(eq(result));
    }
}
