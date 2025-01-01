/**
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.thing.binding.generic.converter;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.binding.generic.ChannelTransformation;
import org.openhab.core.thing.binding.generic.ChannelValueConverterConfig;
import org.openhab.core.thing.internal.binding.generic.converter.AbstractTransformingChannelHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * The {@link GenericChannelHandler} implements simple conversions for different item types
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class GenericChannelHandler extends AbstractTransformingChannelHandler {
    private final Function<String, State> toState;

    public GenericChannelHandler(Function<String, State> toState, Consumer<State> updateState,
            Consumer<Command> postCommand, @Nullable Consumer<String> sendValue,
            ChannelTransformation stateTransformations, ChannelTransformation commandTransformations,
            ChannelValueConverterConfig channelConfig) {
        super(updateState, postCommand, sendValue, stateTransformations, commandTransformations, channelConfig);
        this.toState = toState;
    }

    protected Optional<State> toState(String value) {
        try {
            return Optional.of(toState.apply(value));
        } catch (IllegalArgumentException e) {
            return Optional.of(UnDefType.UNDEF);
        }
    }

    @Override
    protected @Nullable Command toCommand(String value) {
        return null;
    }

    protected String toString(Command command) {
        return command.toString();
    }
}
