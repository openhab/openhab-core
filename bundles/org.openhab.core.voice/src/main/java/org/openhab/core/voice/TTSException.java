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
package org.openhab.core.voice;

/**
 * General purpose TTS exception
 *
 * @author Kelly Davis - Initial contribution
 */
public class TTSException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with null as its detail message.
     */
    public TTSException() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message Detail message
     * @param cause The cause
     */
    public TTSException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message Detail message
     */
    public TTSException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause The cause
     */
    public TTSException(Throwable cause) {
        super(cause);
    }
}
