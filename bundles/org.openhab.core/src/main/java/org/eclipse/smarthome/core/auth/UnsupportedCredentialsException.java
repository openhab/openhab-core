/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.auth;

/**
 * A dedicated exception thrown when extracted credentials can not be matched with any authentication provider.
 *
 * This can usually happen when configuration is somewhat wrong. In order to make debugging easier a separate exception
 * is created.
 *
 * @author Łukasz Dywicki - Initial contribution and API.
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
