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
 * Base type for exceptions thrown by authentication layer.
 *
 * @author Łukasz Dywicki - Initial contribution and API
 * @author Kai Kreuzer - Added JavaDoc and serial id
 *
 */
public class AuthenticationException extends SecurityException {

    private static final long serialVersionUID = 8063538216812770858L;

    /**
     * Creates a new exception instance.
     *
     * @param message exception message
     */
    public AuthenticationException(String message) {
        super(message);
    }

    /**
     * Creates a new exception instance.
     *
     * @param cause exception cause
     */
    public AuthenticationException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new exception instance.
     *
     * @param message exception message
     * @param cause exception cause
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

}
