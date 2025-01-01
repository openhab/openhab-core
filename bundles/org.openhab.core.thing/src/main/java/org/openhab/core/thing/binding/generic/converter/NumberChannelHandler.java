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

import javax.measure.format.MeasurementParseException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.thing.binding.generic.ChannelTransformation;
import org.openhab.core.thing.binding.generic.ChannelValueConverterConfig;
import org.openhab.core.thing.internal.binding.generic.converter.AbstractTransformingChannelHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * The {@link NumberChannelHandler} implements {@link org.openhab.core.library.items.NumberItem} conversions
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class NumberChannelHandler extends AbstractTransformingChannelHandler {

    public NumberChannelHandler(Consumer<State> updateState, Consumer<Command> postCommand,
            @Nullable Consumer<String> sendValue, ChannelTransformation stateTransformations,
            ChannelTransformation commandTransformations, ChannelValueConverterConfig channelConfig) {
        super(updateState, postCommand, sendValue, stateTransformations, commandTransformations, channelConfig);
    }

    @Override
    protected @Nullable Command toCommand(String value) {
        return null;
    }

    @Override
    protected Optional<State> toState(String value) {
        String trimmedValue = value.trim();
        State newState = UnDefType.UNDEF;
        if (!trimmedValue.isEmpty()) {
            try {
                if (channelConfig.unit != null) {
                    // we have a given unit - use that
                    newState = new QuantityType<>(trimmedValue + " " + channelConfig.unit);
                } else {
                    try {
                        // try if we have a simple number
                        newState = new DecimalType(trimmedValue);
                    } catch (IllegalArgumentException e1) {
                        // not a plain number, maybe with unit?
                        newState = new QuantityType<>(trimmedValue);
                    }
                }
            } catch (IllegalArgumentException | MeasurementParseException e) {
                // finally failed
            }
        }
        return Optional.of(newState);
    }

    @Override
    protected String toString(Command command) {
        return command.toString();
    }
}
