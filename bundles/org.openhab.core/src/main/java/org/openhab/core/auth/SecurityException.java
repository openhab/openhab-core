/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.auth;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Base type for exceptions reporting security concerns.
 *
 * @author Łukasz Dywicki - Initial contribution
 */
@SuppressWarnings("serial")
@NonNullByDefault
public class SecurityException extends Exception {

    /**
     * Creates a new exception instance.
     *
     * @param message exception message
     */
    public SecurityException(String message) {
        super(message);
    }

    /**
     * Creates a new exception instance.
     *
     * @param cause exception cause
     */
    public SecurityException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new exception instance.
     *
     * @param message exception message
     * @param cause exception cause
     */
    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
