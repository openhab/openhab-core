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
 * General purpose audio exception
 *
 * @author Harald Kuhn - Initial contribution
 * @author Kelly Davis - Modified to match discussion in #584
 */
@NonNullByDefault
public class AudioException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with null as its detail message.
     */
    public AudioException() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message Detail message
     * @param cause The cause
     */
    public AudioException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message Detail message
     */
    public AudioException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause The cause
     */
    public AudioException(Throwable cause) {
        super(cause);
    }
}
