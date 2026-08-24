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
package org.openhab.core.io.bin2json;

import java.io.Serial;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * {@link ConversionException} generic exception for errors which occurs during conversion.
 *
 * @author Pauli Anttila - Initial contribution
 */
@NonNullByDefault
public class ConversionException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new ConversionException with no detail message.
     */
    public ConversionException() {
    }

    /**
     * Constructs a new ConversionException with the specified detail message.
     *
     * @param message the detail message describing the conversion error
     */
    public ConversionException(String message) {
        super(message);
    }

    /**
     * Constructs a new ConversionException with the specified detail message and cause.
     *
     * @param message the detail message describing the conversion error
     * @param cause the underlying cause of the conversion error
     */
    public ConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new ConversionException with the specified cause.
     *
     * @param cause the underlying cause of the conversion error
     */
    public ConversionException(Throwable cause) {
        super(cause);
    }
}
