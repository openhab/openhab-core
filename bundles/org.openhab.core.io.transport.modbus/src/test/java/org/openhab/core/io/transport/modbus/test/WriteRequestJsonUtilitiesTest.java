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
import static org.hamcrest.collection.ArrayMatching.arrayContaining;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openhab.core.io.transport.modbus.ModbusConstants.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.jdt.annotation.NonNull;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.openhab.core.io.transport.modbus.ModbusWriteFunctionCode;
import org.openhab.core.io.transport.modbus.ModbusWriteRequestBlueprint;
import org.openhab.core.io.transport.modbus.json.WriteRequestJsonUtilities;

/**
 * @author Sami Salonen - Initial contribution
 */
public class WriteRequestJsonUtilitiesTest {

    private static List<String> MAX_REGISTERS = IntStream.range(0, MAX_REGISTERS_WRITE_COUNT).mapToObj(i -> "1")
            .collect(Collectors.toList());
    private static List<String> OVER_MAX_REGISTERS = IntStream.range(0, MAX_REGISTERS_WRITE_COUNT + 1)
            .mapToObj(i -> "1").collect(Collectors.toList());

    private static List<String> MAX_COILS = IntStream.range(0, MAX_BITS_WRITE_COUNT).mapToObj(i -> "1")
            .collect(Collectors.toList());
    private static List<String> OVER_MAX_COILS = IntStream.range(0, MAX_BITS_WRITE_COUNT + 1).mapToObj(i -> "1")
            .collect(Collectors.toList());

    @Test
    public void testEmptyArray() {
        assertThat(WriteRequestJsonUtilities.fromJson(3, "[]").size(), is(equalTo(0)));
    }

    @Test
    public void testFC6NoRegister() {
        assertThrows(IllegalArgumentException.class, () -> WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 6,"//
                + "\"address\": 5412,"//
                + "\"value\": []"//
                + "}]"));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testFC6SingleRegister() {
        assertThat(WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 6,"//
                + "\"address\": 5412,"//
                + "\"value\": [3]"//
                + "}]").toArray(),
                arrayContaining((Matcher) new RegisterMatcher(55, 5412, WriteRequestJsonUtilities.DEFAULT_MAX_TRIES,
                        ModbusWriteFunctionCode.WRITE_SINGLE_REGISTER, 3)));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testFC6SingleRegisterMaxTries99() {
        assertThat(WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 6,"//
                + "\"address\": 5412,"//
                + "\"value\": [3],"//
                + "\"maxTries\": 99"//
                + "}]").toArray(),
                arrayContaining(
                        (Matcher) new RegisterMatcher(55, 5412, 99, ModbusWriteFunctionCode.WRITE_SINGLE_REGISTER, 3)));
    }

    @Test
    public void testFC6MultipleRegisters() {
        assertThrows(IllegalArgumentException.class, () -> WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 6,"//
                + "\"address\": 5412,"//
                + "\"value\": [3, 4]"//
                + "}]"));
    }

    @Test
    public void testFC16NoRegister() {
        assertThrows(IllegalArgumentException.class, () -> WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 16,"//
                + "\"address\": 5412,"//
                + "\"value\": []"//
                + "}]"));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testFC16SingleRegister() {
        assertThat(WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 16,"//
                + "\"address\": 5412,"//
                + "\"value\": [3]"//
                + "}]").toArray(),
                arrayContaining((Matcher) new RegisterMatcher(55, 5412, WriteRequestJsonUtilities.DEFAULT_MAX_TRIES,
                        ModbusWriteFunctionCode.WRITE_MULTIPLE_REGISTERS, 3)));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testFC16MultipleRegisters() {
        assertThat(WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 16,"//
                + "\"address\": 5412,"//
                + "\"value\": [3, 4, 2]"//
                + "}]").toArray(),
                arrayContaining((Matcher) new RegisterMatcher(55, 5412, WriteRequestJsonUtilities.DEFAULT_MAX_TRIES,
                        ModbusWriteFunctionCode.WRITE_MULTIPLE_REGISTERS, 3, 4, 2)));
    }

    @Test
    public void testFC16MultipleRegistersMaxRegisters() {
        Collection<@NonNull ModbusWriteRequestBlueprint> writes = WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 16,"//
                + "\"address\": 5412,"//
                + "\"value\": [" + String.join(",", MAX_REGISTERS) + "]"//
                + "}]");
        assertThat(writes.size(), is(equalTo(1)));
    }

    @Test
    public void testFC16MultipleRegistersTooManyRegisters() {
        assertThrows(IllegalArgumentException.class, () -> WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 16,"//
                + "\"address\": 5412,"//
                + "\"value\": [" + String.join(",", OVER_MAX_REGISTERS) + "]"//
                + "}]"));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testFC5SingeCoil() {
        assertThat(WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 5,"//
                + "\"address\": 5412,"//
                + "\"value\": [3]" // value 3 (!= 0) is converted to boolean true
                + "}]").toArray(),
                arrayContaining((Matcher) new CoilMatcher(55, 5412, WriteRequestJsonUtilities.DEFAULT_MAX_TRIES,
                        ModbusWriteFunctionCode.WRITE_COIL, true)));
    }

    @Test
    public void testFC5MultipleCoils() {
        assertThrows(IllegalArgumentException.class, () -> WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 5,"//
                + "\"address\": 5412,"//
                + "\"value\": [3, 4]"//
                + "}]"));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testFC15SingleCoil() {
        assertThat(WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 15,"//
                + "\"address\": 5412,"//
                + "\"value\": [3]"//
                + "}]").toArray(),
                arrayContaining((Matcher) new CoilMatcher(55, 5412, WriteRequestJsonUtilities.DEFAULT_MAX_TRIES,
                        ModbusWriteFunctionCode.WRITE_MULTIPLE_COILS, true)));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testFC15MultipleCoils() {
        assertThat(WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 15,"//
                + "\"address\": 5412,"//
                + "\"value\": [1, 0, 5]"//
                + "}]").toArray(),
                arrayContaining((Matcher) new CoilMatcher(55, 5412, WriteRequestJsonUtilities.DEFAULT_MAX_TRIES,
                        ModbusWriteFunctionCode.WRITE_MULTIPLE_COILS, true, false, true)));
    }

    @Test
    public void testFC15MultipleCoilsMaxCoils() {
        Collection<@NonNull ModbusWriteRequestBlueprint> writes = WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 15,"//
                + "\"address\": 5412,"//
                + "\"value\": [" + String.join(",", MAX_COILS) + "]"//
                + "}]");
        assertThat(writes.size(), is(equalTo(1)));
    }

    @Test
    public void testFC15MultipleCoilsTooManyCoils() {
        assertThrows(IllegalArgumentException.class, () -> WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 15,"//
                + "\"address\": 5412,"//
                + "\"value\": [" + String.join(",", OVER_MAX_COILS) + "]"//
                + "}]"));
    }

    @Test
    public void testEmptyObject() {
        // we are expecting list, not object -> error
        assertThrows(IllegalStateException.class, () -> WriteRequestJsonUtilities.fromJson(3, "{}"));
    }

    @Test
    public void testNumber() {
        // we are expecting list, not primitive (number) -> error
        assertThrows(IllegalStateException.class, () -> WriteRequestJsonUtilities.fromJson(3, "3"));
    }

    @Test
    public void testEmptyList() {
        assertThat(WriteRequestJsonUtilities.fromJson(3, "[]").size(), is(equalTo(0)));
    }
}
