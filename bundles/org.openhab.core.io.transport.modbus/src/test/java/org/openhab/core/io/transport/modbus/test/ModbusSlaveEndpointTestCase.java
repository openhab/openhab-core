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
package org.openhab.core.io.transport.modbus.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.openhab.core.io.transport.modbus.endpoint.ModbusSerialSlaveEndpoint;
import org.openhab.core.io.transport.modbus.endpoint.ModbusTCPSlaveEndpoint;
import org.openhab.core.io.transport.modbus.endpoint.ModbusUDPSlaveEndpoint;

import gnu.io.SerialPort;
import net.wimpi.modbus.Modbus;

/**
 * @author Sami Salonen - Initial contribution
 */
public class ModbusSlaveEndpointTestCase {

    @Test
    public void testEqualsSameTcp() {
        ModbusTCPSlaveEndpoint e1 = new ModbusTCPSlaveEndpoint("127.0.0.1", 500, false);
        ModbusTCPSlaveEndpoint e2 = new ModbusTCPSlaveEndpoint("127.0.0.1", 500, false);
        assertEquals(e1, e2);
    }

    @Test
    public void testEqualsSameSerial2() {
        ModbusSerialSlaveEndpoint e1 = new ModbusSerialSlaveEndpoint("port1", 9600, SerialPort.FLOWCONTROL_NONE,
                SerialPort.FLOWCONTROL_NONE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE,
                Modbus.DEFAULT_SERIAL_ENCODING, true, 500);
        ModbusSerialSlaveEndpoint e2 = new ModbusSerialSlaveEndpoint("port1", 9600, SerialPort.FLOWCONTROL_NONE,
                SerialPort.FLOWCONTROL_NONE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE,
                Modbus.DEFAULT_SERIAL_ENCODING, true, 500);
        assertEquals(e1, e2);
    }

    /**
     * even though different echo parameter & baud rate, the endpoints are considered the same due to same port
     */
    @Test
    public void testEqualsSameSerial3() {
        ModbusSerialSlaveEndpoint e1 = new ModbusSerialSlaveEndpoint("port1", 9600, SerialPort.FLOWCONTROL_NONE,
                SerialPort.FLOWCONTROL_NONE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE,
                Modbus.DEFAULT_SERIAL_ENCODING, true, 500);
        ModbusSerialSlaveEndpoint e2 = new ModbusSerialSlaveEndpoint("port1", 9600, SerialPort.FLOWCONTROL_NONE,
                SerialPort.FLOWCONTROL_NONE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE,
                Modbus.DEFAULT_SERIAL_ENCODING, false, 500);
        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    public void testEqualsDifferentSerial() {
        ModbusSerialSlaveEndpoint e1 = new ModbusSerialSlaveEndpoint("port1", 9600, SerialPort.FLOWCONTROL_NONE,
                SerialPort.FLOWCONTROL_NONE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE,
                Modbus.DEFAULT_SERIAL_ENCODING, true, 500);
        ModbusSerialSlaveEndpoint e2 = new ModbusSerialSlaveEndpoint("port2", 9600, SerialPort.FLOWCONTROL_NONE,
                SerialPort.FLOWCONTROL_NONE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE,
                Modbus.DEFAULT_SERIAL_ENCODING, true, 500);
        assertNotEquals(e1, e2);
        assertNotEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    public void testEqualsDifferentTCPPort() {
        ModbusTCPSlaveEndpoint e1 = new ModbusTCPSlaveEndpoint("127.0.0.1", 500, false);
        ModbusTCPSlaveEndpoint e2 = new ModbusTCPSlaveEndpoint("127.0.0.1", 501, false);
        assertNotEquals(e1, e2);
        assertNotEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    public void testEqualsDifferentTCPHost() {
        ModbusTCPSlaveEndpoint e1 = new ModbusTCPSlaveEndpoint("127.0.0.1", 500, false);
        ModbusTCPSlaveEndpoint e2 = new ModbusTCPSlaveEndpoint("127.0.0.2", 501, false);
        assertNotEquals(e1, e2);
        assertNotEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    public void testEqualsDifferentProtocol() {
        ModbusTCPSlaveEndpoint e1 = new ModbusTCPSlaveEndpoint("127.0.0.1", 500, false);
        ModbusUDPSlaveEndpoint e2 = new ModbusUDPSlaveEndpoint("127.0.0.1", 500);
        assertNotEquals(e1, e2);
        assertNotEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    public void testEqualsDifferentProtocol2() {
        ModbusTCPSlaveEndpoint e1 = new ModbusTCPSlaveEndpoint("127.0.0.1", 500, false);
        ModbusSerialSlaveEndpoint e2 = new ModbusSerialSlaveEndpoint("port2", 9600, SerialPort.FLOWCONTROL_NONE,
                SerialPort.FLOWCONTROL_NONE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE,
                Modbus.DEFAULT_SERIAL_ENCODING, true, 500);
        assertNotEquals(e1, e2);
        assertNotEquals(e1.hashCode(), e2.hashCode());
    }

    /*
     * TCP slaves pointing to same host & port are considered equal even rtu encodinng differs.
     * Thus ensures correct connection pooling and connection sharing
     */
    @Test
    public void testEqualsSameTcpDifferentEncoding() {
        ModbusTCPSlaveEndpoint e1 = new ModbusTCPSlaveEndpoint("127.0.0.1", 500, false);
        ModbusTCPSlaveEndpoint e2 = new ModbusTCPSlaveEndpoint("127.0.0.1", 500, true);
        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
    }
}
