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
package org.eclipse.smarthome.core.voice;

/**
 * General purpose STT exception
 *
 * @author Kelly Davis - Initial contribution and API
 */
public class STTException extends Exception {

    private static final long serialVersionUID = 1L;

   /**
    * Constructs a new exception with null as its detail message.
    */
    public STTException() {
        super();
    }

   /**
    * Constructs a new exception with the specified detail message and cause.
    *
    * @param message Detail message
    * @param cause The cause
    */
    public STTException(String message, Throwable cause) {
        super(message, cause);
    }

   /**
    * Constructs a new exception with the specified detail message.
    *
    * @param message Detail message
    */
    public STTException(String message) {
        super(message);
    }

   /**
    * Constructs a new exception with the specified cause.
    *
    * @param cause The cause
    */
    public STTException(Throwable cause) {
        super(cause);
    }
}
