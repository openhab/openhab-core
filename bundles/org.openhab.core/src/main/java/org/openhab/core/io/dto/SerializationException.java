/*
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
package org.openhab.core.io.dto;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Signals that a serialization or deserialization exception of some sort has occurred.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class SerializationException extends IOException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a {@code SerializationException} with the specified detail message.
     *
     * @param message the detail message.
     */
    public SerializationException(@Nullable String message) {
        super(message);
    }

    /**
     * Creates a {@code SerializationException} with the specified cause and a detail message of
     * {@code (cause==null ? null : cause.toString())}.
     *
     * @param cause the cause. A {@code null} value is permitted, and indicates that the cause is nonexistent or
     *            unknown.
     */
    public SerializationException(@Nullable Throwable cause) {
        super(cause);
    }

    /**
     * Creates a {@code SerializationException} with the specified cause and detail message.
     *
     * @param message the detail message.
     * @param cause the cause. A {@code null} value is permitted, and indicates that the cause is nonexistent or
     *            unknown.
     */
    public SerializationException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
