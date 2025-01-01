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

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.binding.generic.ChannelTransformation;
import org.openhab.core.thing.binding.generic.ChannelValueConverterConfig;
import org.openhab.core.thing.internal.binding.generic.converter.AbstractTransformingChannelHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * The {@link FixedValueMappingChannelHandler} implements mapping conversions for different item-types
 *
 * @author Jan N. Klug - Initial contribution
 */

@NonNullByDefault
public class FixedValueMappingChannelHandler extends AbstractTransformingChannelHandler {

    public FixedValueMappingChannelHandler(Consumer<State> updateState, Consumer<Command> postCommand,
            @Nullable Consumer<String> sendValue, ChannelTransformation stateTransformations,
            ChannelTransformation commandTransformations, ChannelValueConverterConfig channelConfig) {
        super(updateState, postCommand, sendValue, stateTransformations, commandTransformations, channelConfig);
    }

    @Override
    protected @Nullable Command toCommand(String value) {
        return null;
    }

    @Override
    public String toString(Command command) {
        String value = channelConfig.commandToFixedValue(command);
        if (value != null) {
            return value;
        }

        throw new IllegalArgumentException(
                "Command type '" + command.toString() + "' not supported or mapping not defined.");
    }

    @Override
    public Optional<State> toState(String string) {
        State state = channelConfig.fixedValueToState(string);

        return Optional.of(Objects.requireNonNullElse(state, UnDefType.UNDEF));
    }
}
