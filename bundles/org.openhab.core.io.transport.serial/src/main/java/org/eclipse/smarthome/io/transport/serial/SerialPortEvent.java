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
package org.eclipse.smarthome.io.transport.serial;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Interface for a serial port event.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public interface SerialPortEvent {
    final int DATA_AVAILABLE = 1;
    final int OUTPUT_BUFFER_EMPTY = 2;
    final int CTS = 3;
    final int DSR = 4;
    final int RI = 5;
    final int CD = 6;
    final int OE = 7;
    final int PE = 8;
    final int FE = 9;
    final int BI = 10;

    /**
     * Get the type of the event.
     *
     * @return the event type
     */
    int getEventType();

    /**
     * Gets the new value of the state change that caused the SerialPortEvent to be propagated. For example, when the CD
     * bit changes, newValue reflects the new value of the CD bit.
     */
    boolean getNewValue();
}
