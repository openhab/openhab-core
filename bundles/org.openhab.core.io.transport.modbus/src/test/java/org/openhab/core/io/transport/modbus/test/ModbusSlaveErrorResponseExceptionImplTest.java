/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.io.transport.modbus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.io.transport.modbus.internal.ModbusSlaveErrorResponseExceptionImpl;

import net.wimpi.modbus.ModbusSlaveException;

/**
 * @author Sami Salonen - Initial contribution
 */
@NonNullByDefault
public class ModbusSlaveErrorResponseExceptionImplTest {

    @Test
    public void testKnownCode1() {
        assertEquals("Slave responded with error=1 (ILLEGAL_FUNCTION)",
                new ModbusSlaveErrorResponseExceptionImpl(new ModbusSlaveException(1)).getMessage());
    }

    @Test
    public void testKnownCode2() {
        assertEquals("Slave responded with error=2 (ILLEGAL_DATA_ACCESS)",
                new ModbusSlaveErrorResponseExceptionImpl(new ModbusSlaveException(2)).getMessage());
    }

    @Test
    public void testUnknownCode() {
        assertEquals("Slave responded with error=99 (unknown error code)",
                new ModbusSlaveErrorResponseExceptionImpl(new ModbusSlaveException(99)).getMessage());
    }
}
