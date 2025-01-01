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

import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.RawType;
import org.openhab.core.thing.binding.generic.ChannelHandler;
import org.openhab.core.thing.binding.generic.ChannelHandlerContent;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * The {@link ImageChannelHandler} implements {@link org.openhab.core.library.items.ImageItem} conversions
 *
 * @author Jan N. Klug - Initial contribution
 */

@NonNullByDefault
public class ImageChannelHandler implements ChannelHandler {
    private final Consumer<State> updateState;

    public ImageChannelHandler(Consumer<State> updateState) {
        this.updateState = updateState;
    }

    @Override
    public void process(@Nullable ChannelHandlerContent content) {
        if (content == null) {
            updateState.accept(UnDefType.UNDEF);
            return;
        }
        String mediaType = content.getMediaType();
        updateState.accept(
                new RawType(content.getRawContent(), mediaType != null ? mediaType : RawType.DEFAULT_MIME_TYPE));
    }

    @Override
    public void send(Command command) {
        throw new IllegalStateException("Read-only channel");
    }
}
