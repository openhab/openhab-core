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
package org.openhab.core.io.transport.mqtt;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Thrown if an error occurs communicating with the server. The exception contains a reason code. The semantic of the
 * reason code depends on the underlying implementation.
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public class MqttException extends Exception {
    private static final long serialVersionUID = 301L;
    private int reasonCode;
    private Throwable cause;

    /**
     * Constructs a new <code>MqttException</code> with the specified code
     * as the underlying reason.
     *
     * @param reasonCode the reason code for the exception.
     */
    @Deprecated
    public MqttException(int reasonCode) {
        this.cause = new Exception();
        this.reasonCode = reasonCode;
    }

    /**
     * Constructs a new <code>MqttException</code> with the specified reason
     *
     * @param reason the reason for the exception.
     */
    public MqttException(String reason) {
        this.cause = new Exception("reason");
        this.reasonCode = Integer.MIN_VALUE;
    }

    /**
     * Constructs a new <code>MqttException</code> with the specified
     * <code>Throwable</code> as the underlying reason.
     *
     * @param cause the underlying cause of the exception.
     */
    public MqttException(Throwable cause) {
        this.reasonCode = Integer.MIN_VALUE;
        this.cause = cause;
    }

    /**
     * Constructs a new <code>MqttException</code> with the specified
     * <code>Throwable</code> as the underlying reason.
     *
     * @param reason the reason code for the exception.
     * @param cause the underlying cause of the exception.
     */
    @Deprecated
    public MqttException(int reason, Throwable cause) {
        this.reasonCode = reason;
        this.cause = cause;
    }

    /**
     * Returns the reason code for this exception.
     *
     * @return the code representing the reason for this exception.
     */
    @Deprecated
    public int getReasonCode() {
        return reasonCode;
    }

    /**
     * Returns the underlying cause of this exception, if available.
     *
     * @return the Throwable that was the root cause of this exception,
     *         which may be <code>null</code>.
     */
    @Override
    public Throwable getCause() {
        return cause;
    }

    /**
     * Returns the detail message for this exception. May be null.
     */
    @Override
    public String getMessage() {
        return cause.getMessage();
    }

    /**
     * Returns a <code>String</code> representation of this exception.
     */
    @Override
    public String toString() {
        return cause.toString();
    }
}
