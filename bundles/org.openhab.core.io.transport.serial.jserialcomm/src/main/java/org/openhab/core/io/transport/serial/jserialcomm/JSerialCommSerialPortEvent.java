/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.io.transport.serial.jserialcomm;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.transport.serial.SerialPortEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specific OH serial transport SerialPortEvent implementation using com.fazecast.jSerialComm.SerialPortEvent
 *
 * @author Massimo Valla - Initial contribution
 */
@NonNullByDefault
public class JSerialCommSerialPortEvent implements SerialPortEvent {

    private final Logger logger = LoggerFactory.getLogger(JSerialCommSerialPortEvent.class);

    private final com.fazecast.jSerialComm.SerialPortEvent event;

    /**
     * Constructor.
     *
     * @param event the underlying event implementation
     */
    public JSerialCommSerialPortEvent(final com.fazecast.jSerialComm.SerialPortEvent event) {
        logger.debug("--------TRANSPORT-jSerialComm--- New event of type: {}", event.getEventType());
        this.event = event;
    }

    @Override
    public int getEventType() {
        // FIXME check event id mapping
        return event.getEventType();
    }

    @Override
    public boolean getNewValue() {
        // FIXME !!!!! placeholder implementation
        return false;
        // return event.getNewValue();
    }
}
