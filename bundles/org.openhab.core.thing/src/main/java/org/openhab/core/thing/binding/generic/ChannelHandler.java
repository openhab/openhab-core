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
package org.openhab.core.thing.binding.generic;

import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

/**
 * The {@link ChannelHandler} defines the interface for converting received {@link ChannelHandlerContent}
 * to {@link org.openhab.core.types.State}s for posting updates to {@link org.openhab.core.thing.Channel}s and
 * {@link Command}s to values for sending
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface ChannelHandler {

    /**
     * called to process a given content for this channel
     *
     * @param content raw content to process (<code>null</code> results in
     *            {@link org.openhab.core.types.UnDefType#UNDEF})
     */
    void process(@Nullable ChannelHandlerContent content);

    /**
     * called to send a command to this channel
     *
     * @param command
     */
    void send(Command command);

    @FunctionalInterface
    interface Factory {
        ChannelHandler create(Consumer<State> updateState, Consumer<Command> postCommand,
                @Nullable Consumer<String> sendValue, ChannelTransformation stateTransformations,
                ChannelTransformation commandTransformations, ChannelValueConverterConfig channelConfig);
    }
}
