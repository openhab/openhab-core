/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.thing.binding.generic.ChannelTransformation;
import org.openhab.core.thing.binding.generic.ChannelValueConverterConfig;
import org.openhab.core.thing.internal.binding.generic.converter.AbstractTransformingChannelHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * The {@link DimmerChannelHandler} implements {@link org.openhab.core.library.items.DimmerItem} conversions
 *
 * @author Jan N. Klug - Initial contribution
 */

@NonNullByDefault
public class DimmerChannelHandler extends AbstractTransformingChannelHandler {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private State state = UnDefType.UNDEF;

    public DimmerChannelHandler(Consumer<State> updateState, Consumer<Command> postCommand,
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
        String string = channelConfig.commandToFixedValue(command);
        if (string != null) {
            return string;
        }

        if (command instanceof PercentType percentCommand) {
            return percentCommand.toString();
        }

        throw new IllegalArgumentException("Command type '" + command.toString() + "' not supported");
    }

    @Override
    public Optional<State> toState(String string) {
        State newState = UnDefType.UNDEF;

        if (string.equals(channelConfig.onValue)) {
            newState = PercentType.HUNDRED;
        } else if (string.equals(channelConfig.offValue)) {
            newState = PercentType.ZERO;
        } else if (string.equals(channelConfig.increaseValue) && state instanceof PercentType percentState) {
            BigDecimal newBrightness = percentState.toBigDecimal().add(channelConfig.step);
            if (HUNDRED.compareTo(newBrightness) < 0) {
                newBrightness = HUNDRED;
            }
            newState = new PercentType(newBrightness);
        } else if (string.equals(channelConfig.decreaseValue) && state instanceof PercentType percentState) {
            BigDecimal newBrightness = percentState.toBigDecimal().subtract(channelConfig.step);
            if (BigDecimal.ZERO.compareTo(newBrightness) > 0) {
                newBrightness = BigDecimal.ZERO;
            }
            newState = new PercentType(newBrightness);
        } else {
            try {
                BigDecimal value = new BigDecimal(string);
                if (value.compareTo(PercentType.HUNDRED.toBigDecimal()) > 0) {
                    value = PercentType.HUNDRED.toBigDecimal();
                }
                if (value.compareTo(PercentType.ZERO.toBigDecimal()) < 0) {
                    value = PercentType.ZERO.toBigDecimal();
                }
                newState = new PercentType(value);
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        state = newState;
        return Optional.of(newState);
    }
}
