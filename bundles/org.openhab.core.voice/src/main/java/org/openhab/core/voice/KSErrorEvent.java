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
 * A {@link KSEvent} fired when the {@link KSService} encounters an error.
 *
 * @author Kelly Davis - Initial contribution
 */
public class KSErrorEvent implements KSEvent {
    /**
     * The message describing the error
     */
    private final String message;

    /**
     * Constructs an instance with the passed {@code message}.
     *
     * @param message The message describing the error
     */
    public KSErrorEvent(String message) {
        this.message = message;
    }

    /**
     * Gets the message describing this error
     *
     * @return The message describing this error
     */
    public String getMessage() {
        return this.message;
    }
}
