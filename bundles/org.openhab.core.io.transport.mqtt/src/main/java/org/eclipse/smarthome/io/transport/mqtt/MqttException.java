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
package org.eclipse.smarthome.io.transport.mqtt;

/**
 * Thrown if an error occurs communicating with the server. The exception contains a reason code. The semantic of the
 * reason code depends on the underlying implementation.
 *
 * @author David Graeff - Initial contribution
 */
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
    public MqttException(int reasonCode) {
        this.reasonCode = reasonCode;
    }

    /**
     * Constructs a new <code>MqttException</code> with the specified
     * <code>Throwable</code> as the underlying reason.
     *
     * @param cause the underlying cause of the exception.
     */
    public MqttException(Throwable cause) {
        if (cause instanceof org.eclipse.paho.client.mqttv3.MqttException) {
            org.eclipse.paho.client.mqttv3.MqttException internalException = (org.eclipse.paho.client.mqttv3.MqttException) cause;
            this.reasonCode = internalException.getReasonCode();
        } else {
            this.reasonCode = org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_EXCEPTION;
        }
        this.cause = cause;
    }

    /**
     * Constructs a new <code>MqttException</code> with the specified
     * <code>Throwable</code> as the underlying reason.
     *
     * @param reason the reason code for the exception.
     * @param cause the underlying cause of the exception.
     */
    public MqttException(int reason, Throwable cause) {
        this.reasonCode = reason;
        this.cause = cause;
    }

    /**
     * Returns the reason code for this exception.
     *
     * @return the code representing the reason for this exception.
     */
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
        if (cause != null) {
            return cause.getMessage();
        }

        return "MqttException with reason " + String.valueOf(reasonCode);
    }

    /**
     * Returns a <code>String</code> representation of this exception.
     */
    @Override
    public String toString() {
        if (cause instanceof org.eclipse.paho.client.mqttv3.MqttException) {
            org.eclipse.paho.client.mqttv3.MqttException internalException = (org.eclipse.paho.client.mqttv3.MqttException) cause;
            return internalException.toString();
        }
        String result = getMessage() + " (" + reasonCode + ")";
        if (cause != null) {
            result = result + " - " + cause.toString();
        }
        return result;
    }
}
