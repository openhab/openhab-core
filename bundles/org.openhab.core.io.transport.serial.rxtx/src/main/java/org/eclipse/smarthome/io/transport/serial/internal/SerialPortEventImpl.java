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
package org.eclipse.smarthome.io.transport.serial.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.io.transport.serial.SerialPortEvent;

/**
 * Specific serial port event implementation.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class SerialPortEventImpl implements SerialPortEvent {

    private final gnu.io.SerialPortEvent event;

    /**
     * Constructor.
     *
     * @param event the underlying event implementation
     */
    public SerialPortEventImpl(final gnu.io.SerialPortEvent event) {
        this.event = event;
    }

    @Override
    public int getEventType() {
        return event.getEventType();
    }

    @Override
    public boolean getNewValue() {
        return event.getNewValue();
    }

}
