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

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.io.transport.modbus.ModbusCommunicationInterface;
import org.openhab.core.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.core.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.core.io.transport.modbus.ModbusRegisterArray;
import org.openhab.core.io.transport.modbus.endpoint.ModbusTCPSlaveEndpoint;

/**
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
class RtuOverTcpTest extends IntegrationTestSupport {

    /*
     * Live functional test of RTU encoding over IP for a specific connected device
     * Do NOT 'runThisTest' unless you have the specific device connected!
     */
    @Test
    public void liveTestRtuOverTcp() throws Exception {
        boolean runThisTest = false;
        if (runThisTest) {
            // server parameters for the specific device
            final String ipAddress = "192.168.1.172";
            final int ipPort = 23;

            // encoding parameters
            final boolean rtuEncoded = true;

            // request parameters for the specific device
            final int slaveId = 1;
            final ModbusReadFunctionCode functionId = ModbusReadFunctionCode.READ_INPUT_REGISTERS;
            final int regStartNumber = 0;
            final int regRequestCount = 10;

            // execution parameters
            final int maxTries = 1;
            final int timeout = 60;

            AtomicInteger unexpectedCount = new AtomicInteger();
            CountDownLatch callbackCalled = new CountDownLatch(1);
            AtomicReference<Object> lastData = new AtomicReference<>();

            ModbusTCPSlaveEndpoint endpoint = new ModbusTCPSlaveEndpoint(ipAddress, ipPort, rtuEncoded);

            try (ModbusCommunicationInterface comms = modbusManager.newModbusCommunicationInterface(endpoint, null)) {
                comms.submitOneTimePoll(
                        new ModbusReadRequestBlueprint(slaveId, functionId, regStartNumber, regRequestCount, maxTries),
                        result -> {
                            Optional<ModbusRegisterArray> registersOptional = result.getRegisters();
                            if (registersOptional.isPresent()) {
                                lastData.set(registersOptional.get());
                            } else {
                                unexpectedCount.incrementAndGet();
                            }
                            callbackCalled.countDown();
                        }, failure -> {
                            unexpectedCount.incrementAndGet();
                            callbackCalled.countDown();
                        });

                assertTrue(callbackCalled.await(timeout, TimeUnit.SECONDS));
                assertThat(unexpectedCount.get(), is(equalTo(0)));
                ModbusRegisterArray registers = (ModbusRegisterArray) lastData.get();
                assertThat(registers.size(), is(equalTo(regRequestCount)));
            }
        }
    }
}
