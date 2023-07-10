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
package org.openhab.core.thing.internal.binding.generic.converter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.binding.generic.ChannelHandlerContent;
import org.openhab.core.thing.binding.generic.ChannelTransformation;
import org.openhab.core.thing.binding.generic.ChannelValueConverterConfig;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * The {@link AbstractTransformingItemConverterTest} is a test class for the
 * {@link AbstractTransformingChannelHandler}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class AbstractTransformingItemConverterTest {

    @Mock
    private @NonNullByDefault({}) Consumer<String> sendHttpValue;

    @Mock
    private @NonNullByDefault({}) Consumer<State> updateState;

    @Mock
    private @NonNullByDefault({}) Consumer<Command> postCommand;

    private @NonNullByDefault({}) AutoCloseable closeable;

    @Spy
    private ChannelTransformation stateChannelTransformation = new ChannelTransformation(null);

    @Spy
    private ChannelTransformation commandChannelTransformation = new ChannelTransformation(null);

    @BeforeEach
    public void init() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void close() throws Exception {
        closeable.close();
    }

    @Test
    public void undefOnNullContentTest() {
        TestChannelHandler realConverter = new TestChannelHandler(updateState, postCommand, sendHttpValue,
                stateChannelTransformation, commandChannelTransformation, false);
        TestChannelHandler converter = spy(realConverter);

        converter.process(null);
        // make sure UNDEF is send as state update
        verify(updateState, only()).accept(UnDefType.UNDEF);
        verify(postCommand, never()).accept(any());
        verify(sendHttpValue, never()).accept(any());

        // make sure no other processing applies
        verify(converter, never()).toState(any());
        verify(converter, never()).toCommand(any());
        verify(converter, never()).toString(any());
    }

    @Test
    public void commandIsPostedAsCommand() {
        TestChannelHandler converter = new TestChannelHandler(updateState, postCommand, sendHttpValue,
                stateChannelTransformation, commandChannelTransformation, true);

        converter.process(new ChannelHandlerContent("TEST".getBytes(StandardCharsets.UTF_8), "", null));

        // check state transformation is applied
        verify(stateChannelTransformation).apply(any());
        verify(commandChannelTransformation, never()).apply(any());

        // check only postCommand is applied
        verify(updateState, never()).accept(any());
        verify(postCommand, only()).accept(new StringType("TEST"));
        verify(sendHttpValue, never()).accept(any());
    }

    @Test
    public void updateIsPostedAsUpdate() {
        TestChannelHandler converter = new TestChannelHandler(updateState, postCommand, sendHttpValue,
                stateChannelTransformation, commandChannelTransformation, false);

        converter.process(new ChannelHandlerContent("TEST".getBytes(StandardCharsets.UTF_8), "", null));

        // check state transformation is applied
        verify(stateChannelTransformation).apply(any());
        verify(commandChannelTransformation, never()).apply(any());

        // check only updateState is called
        verify(updateState, only()).accept(new StringType("TEST"));
        verify(postCommand, never()).accept(any());
        verify(sendHttpValue, never()).accept(any());
    }

    @Test
    public void sendCommandSendsCommand() {
        TestChannelHandler converter = new TestChannelHandler(updateState, postCommand, sendHttpValue,
                stateChannelTransformation, commandChannelTransformation, false);

        converter.send(new StringType("TEST"));

        // check command transformation is applied
        verify(stateChannelTransformation, never()).apply(any());
        verify(commandChannelTransformation).apply(any());

        // check only sendHttpValue is applied
        verify(updateState, never()).accept(any());
        verify(postCommand, never()).accept(any());
        verify(sendHttpValue, only()).accept("TEST");
    }

    private static class TestChannelHandler extends AbstractTransformingChannelHandler {
        private boolean hasCommand;

        public TestChannelHandler(Consumer<State> updateState, Consumer<Command> postCommand,
                @Nullable Consumer<String> sendValue, ChannelTransformation stateChannelTransformation,
                ChannelTransformation commandChannelTransformation, boolean hasCommand) {
            super(updateState, postCommand, sendValue, stateChannelTransformation, commandChannelTransformation,
                    new ChannelValueConverterConfig());
            this.hasCommand = hasCommand;
        }

        @Override
        protected @Nullable Command toCommand(String value) {
            return hasCommand ? new StringType(value) : null;
        }

        @Override
        protected Optional<State> toState(String value) {
            return Optional.of(new StringType(value));
        }

        @Override
        protected String toString(Command command) {
            return command.toString();
        }
    }
}
