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
 * Thrown when a requested format is not supported by an {@link AudioSource}
 * or {@link AudioSink} implementation
 *
 * @author Harald Kuhn - Initial contribution
 * @author Kelly Davis - Modified to match discussion in #584
 */
@NonNullByDefault
public class UnsupportedAudioFormatException extends AudioException {

    private static final long serialVersionUID = 1L;

    /**
     * Unsupported {@link AudioFormat}
     */
    private @Nullable AudioFormat unsupportedFormat;

    /**
     * Constructs a new exception with the specified detail message, unsupported format, and cause.
     *
     * @param message Detail message
     * @param unsupportedFormat Unsupported format
     * @param cause The cause
     */
    public UnsupportedAudioFormatException(String message, @Nullable AudioFormat unsupportedFormat,
            @Nullable Throwable cause) {
        super(message, cause);
        this.unsupportedFormat = unsupportedFormat;
    }

    /**
     * Constructs a new exception with the specified detail message and unsupported format.
     *
     * @param message Detail message
     * @param unsupportedFormat Unsupported format
     */
    public UnsupportedAudioFormatException(String message, @Nullable AudioFormat unsupportedFormat) {
        this(message, unsupportedFormat, null);
    }

    /**
     * Gets the unsupported format
     *
     * @return The unsupported format
     */
    public @Nullable AudioFormat getUnsupportedFormat() {
        return unsupportedFormat;
    }
}
