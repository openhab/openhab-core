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
package org.openhab.core.auth;

/**
 * A dedicated exception thrown when extracted credentials can not be matched with any authentication provider.
 *
 * This can usually happen when configuration is somewhat wrong. In order to make debugging easier a separate exception
 * is created.
 *
 * @author Łukasz Dywicki - Initial contribution
 */
public class UnsupportedCredentialsException extends AuthenticationException {

    /**
     * Creates a new exception instance.
     *
     * @param message exception message
     */
    public UnsupportedCredentialsException(String message) {
        super(message);
    }

    /**
     * Creates a new exception instance.
     *
     * @param cause exception cause
     */
    public UnsupportedCredentialsException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new exception instance.
     *
     * @param message exception message
     * @param cause exception cause
     */
    public UnsupportedCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }

}
