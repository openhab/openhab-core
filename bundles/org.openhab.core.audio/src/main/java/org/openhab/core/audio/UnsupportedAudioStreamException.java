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
package org.openhab.core.audio;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Thrown when a requested {@link AudioStream} is not supported by an {@link AudioSource} or {@link AudioSink}
 * implementation
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class UnsupportedAudioStreamException extends AudioException {

    private static final long serialVersionUID = 1L;

    /**
     * Unsupported {@link AudioStream}
     */
    private @Nullable Class<? extends @Nullable AudioStream> unsupportedAudioStreamClass;

    /**
     * Constructs a new exception with the specified detail message, unsupported format, and cause.
     *
     * @param message The message
     * @param unsupportedAudioStreamClass The unsupported audio stream class
     * @param cause The cause
     */
    public UnsupportedAudioStreamException(String message,
            @Nullable Class<? extends @Nullable AudioStream> unsupportedAudioStreamClass, @Nullable Throwable cause) {
        super(message, cause);
        this.unsupportedAudioStreamClass = unsupportedAudioStreamClass;
    }

    /**
     * Constructs a new exception with the specified detail message and unsupported format.
     *
     * @param message The message
     * @param unsupportedAudioStreamClass The unsupported audio stream class
     */
    public UnsupportedAudioStreamException(String message,
            @Nullable Class<? extends @Nullable AudioStream> unsupportedAudioStreamClass) {
        this(message, unsupportedAudioStreamClass, null);
    }

    /**
     * Gets the unsupported audio stream class.
     *
     * @return The unsupported audio stream class
     */
    public @Nullable Class<? extends @Nullable AudioStream> getUnsupportedAudioStreamClass() {
        return unsupportedAudioStreamClass;
    }
}
