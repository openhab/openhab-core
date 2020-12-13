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
package org.openhab.core.io.transport.modbus;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Interface for read callbacks
 *
 * @author Sami Salonen - Initial contribution
 */
@FunctionalInterface
@NonNullByDefault
public interface ModbusReadCallback extends ModbusResultCallback {

    /**
     * Callback handling response data
     *
     * @param result result of the read operation
     */
    void handle(AsyncModbusReadResult result);
}
