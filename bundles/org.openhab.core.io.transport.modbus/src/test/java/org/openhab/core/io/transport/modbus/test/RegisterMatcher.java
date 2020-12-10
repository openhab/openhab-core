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

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.hamcrest.Description;
import org.openhab.core.io.transport.modbus.ModbusRegisterArray;
import org.openhab.core.io.transport.modbus.ModbusWriteFunctionCode;
import org.openhab.core.io.transport.modbus.ModbusWriteRegisterRequestBlueprint;

/**
 * @author Sami Salonen - Initial contribution
 */
class RegisterMatcher extends AbstractRequestComparer<ModbusWriteRegisterRequestBlueprint> {

    private Integer[] expectedRegisterValues;

    public RegisterMatcher(int expectedUnitId, int expectedAddress, int expectedMaxTries,
            ModbusWriteFunctionCode expectedFunctionCode, Integer... expectedRegisterValues) {
        super(expectedUnitId, expectedAddress, expectedFunctionCode, expectedMaxTries);
        this.expectedRegisterValues = expectedRegisterValues;
    }

    @Override
    public void describeTo(Description description) {
        super.describeTo(description);
        description.appendText(" registers=");
        description.appendValue(Arrays.toString(expectedRegisterValues));
    }

    @Override
    protected boolean doMatchData(ModbusWriteRegisterRequestBlueprint item) {
        ModbusRegisterArray registers = item.getRegisters();
        Object[] actual = StreamSupport
                .stream(IntStream.range(0, registers.size()).mapToObj(registers::getRegister).spliterator(), false)
                .toArray();
        return Objects.deepEquals(actual, expectedRegisterValues);
    }
}
