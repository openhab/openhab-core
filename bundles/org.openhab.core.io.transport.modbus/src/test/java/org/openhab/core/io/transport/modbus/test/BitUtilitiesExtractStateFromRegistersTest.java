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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.core.io.transport.modbus.ModbusBitUtilities;
import org.openhab.core.io.transport.modbus.ModbusConstants.ValueType;
import org.openhab.core.io.transport.modbus.ModbusRegisterArray;
import org.openhab.core.library.types.DecimalType;

/**
 * @author Sami Salonen - Initial contribution
 */
public class BitUtilitiesExtractStateFromRegistersTest {

    private static ModbusRegisterArray shortArrayToRegisterArray(int... arr) {
        return new ModbusRegisterArray(arr);
    }

    public static Collection<Object[]> data() {
        return Collections.unmodifiableList(Stream.of(
                //
                // BIT
                //
                new Object[] { new DecimalType("1.0"), ValueType.BIT,
                        shortArrayToRegisterArray(1 << 5 | 1 << 4 | 1 << 15), 4 },
                new Object[] { new DecimalType("1.0"), ValueType.BIT,
                        shortArrayToRegisterArray(1 << 5 | 1 << 4 | 1 << 15), 15 },
                new Object[] { new DecimalType("0.0"), ValueType.BIT, shortArrayToRegisterArray(1 << 5), 7 },
                new Object[] { new DecimalType("1.0"), ValueType.BIT, shortArrayToRegisterArray(1 << 5), 5 },
                new Object[] { new DecimalType("0.0"), ValueType.BIT, shortArrayToRegisterArray(1 << 5), 4 },
                new Object[] { new DecimalType("0.0"), ValueType.BIT, shortArrayToRegisterArray(1 << 5), 0 },
                new Object[] { new DecimalType("0.0"), ValueType.BIT, shortArrayToRegisterArray(0, 0), 15 },
                new Object[] { new DecimalType("1.0"), ValueType.BIT, shortArrayToRegisterArray(1 << 5, 1 << 4), 5 },
                new Object[] { new DecimalType("1.0"), ValueType.BIT, shortArrayToRegisterArray(1 << 5, 1 << 4), 20 },
                new Object[] { IllegalArgumentException.class, ValueType.BIT, shortArrayToRegisterArray(1 << 5), 16 },
                new Object[] { IllegalArgumentException.class, ValueType.BIT, shortArrayToRegisterArray(1 << 5), 200 },
                new Object[] { IllegalArgumentException.class, ValueType.BIT, shortArrayToRegisterArray(), 0 },
                new Object[] { IllegalArgumentException.class, ValueType.BIT, shortArrayToRegisterArray(0, 0), 32 },
                //
                // INT8
                //
                new Object[] { new DecimalType("5.0"), ValueType.INT8, shortArrayToRegisterArray(5), 0 },
                new Object[] { new DecimalType("-5.0"), ValueType.INT8, shortArrayToRegisterArray(-5), 0 },
                new Object[] { new DecimalType("3.0"), ValueType.INT8,
                        shortArrayToRegisterArray(((byte) 6 << 8) | (byte) 3), 0 },
                new Object[] { new DecimalType("6.0"), ValueType.INT8,
                        shortArrayToRegisterArray(((byte) 6 << 8) | (byte) 3), 1 },
                new Object[] { new DecimalType("4.0"), ValueType.INT8,
                        shortArrayToRegisterArray(((byte) 6 << 8) | (byte) 3, 4), 2 },
                new Object[] { new DecimalType("6.0"), ValueType.INT8,
                        shortArrayToRegisterArray(55, ((byte) 6 << 8) | (byte) 3), 3 },
                new Object[] { IllegalArgumentException.class, ValueType.INT8, shortArrayToRegisterArray(1), 2 },
                new Object[] { IllegalArgumentException.class, ValueType.INT8, shortArrayToRegisterArray(1, 2), 4 },
                //
                // UINT8
                //
                new Object[] { new DecimalType("5.0"), ValueType.UINT8, shortArrayToRegisterArray(5), 0 },
                new Object[] { new DecimalType("251.0"), ValueType.UINT8, shortArrayToRegisterArray(-5), 0 },
                new Object[] { new DecimalType("3.0"), ValueType.UINT8,
                        shortArrayToRegisterArray(((byte) 6 << 8) | (byte) 3), 0 },
                new Object[] { new DecimalType("6.0"), ValueType.UINT8,
                        shortArrayToRegisterArray(((byte) 6 << 8) | (byte) 3), 1 },
                new Object[] { new DecimalType("4.0"), ValueType.UINT8,
                        shortArrayToRegisterArray(((byte) 6 << 8) | (byte) 3, 4), 2 },
                new Object[] { new DecimalType("6.0"), ValueType.UINT8,
                        shortArrayToRegisterArray(55, ((byte) 6 << 8) | (byte) 3), 3 },
                new Object[] { IllegalArgumentException.class, ValueType.UINT8, shortArrayToRegisterArray(1), 2 },
                new Object[] { IllegalArgumentException.class, ValueType.UINT8, shortArrayToRegisterArray(1, 2), 4 },

                //
                // INT16
                //
                new Object[] { new DecimalType("1.0"), ValueType.INT16, shortArrayToRegisterArray(1), 0 },
                new Object[] { new DecimalType("2.0"), ValueType.INT16, shortArrayToRegisterArray(2), 0 },
                new Object[] { new DecimalType("-1004"), ValueType.INT16, shortArrayToRegisterArray(-1004), 0 },
                new Object[] { new DecimalType("-1536"), ValueType.INT16, shortArrayToRegisterArray(64000), 0 },
                new Object[] { new DecimalType("-1004"), ValueType.INT16, shortArrayToRegisterArray(4, -1004), 1 },
                new Object[] { new DecimalType("-1004"), ValueType.INT16, shortArrayToRegisterArray(-1004, 4), 0 },
                new Object[] { IllegalArgumentException.class, ValueType.INT16, shortArrayToRegisterArray(4, -1004),
                        2 },
                //
                // UINT16
                //
                new Object[] { new DecimalType("1.0"), ValueType.UINT16, shortArrayToRegisterArray(1), 0 },
                new Object[] { new DecimalType("2.0"), ValueType.UINT16, shortArrayToRegisterArray(2), 0 },
                new Object[] { new DecimalType("64532"), ValueType.UINT16, shortArrayToRegisterArray(-1004), 0 },
                new Object[] { new DecimalType("64000"), ValueType.UINT16, shortArrayToRegisterArray(64000), 0 },
                new Object[] { new DecimalType("64532"), ValueType.UINT16, shortArrayToRegisterArray(4, -1004), 1 },
                new Object[] { new DecimalType("64532"), ValueType.UINT16, shortArrayToRegisterArray(-1004, 4), 0 },
                new Object[] { IllegalArgumentException.class, ValueType.UINT16, shortArrayToRegisterArray(4, -1004),
                        2 },
                //
                // INT32
                //
                new Object[] { new DecimalType("1.0"), ValueType.INT32, shortArrayToRegisterArray(0, 1), 0 },
                new Object[] { new DecimalType("2.0"), ValueType.INT32, shortArrayToRegisterArray(0, 2), 0 },
                new Object[] { new DecimalType("-1004"), ValueType.INT32,
                        // -1004 = 0xFFFFFC14 (32bit) =
                        shortArrayToRegisterArray(0xFFFF, 0xFC14), 0 },
                new Object[] { new DecimalType("64000"), ValueType.INT32, shortArrayToRegisterArray(0, 64000), 0 },
                new Object[] { new DecimalType("-1004"), ValueType.INT32,
                        // -1004 = 0xFFFFFC14 (32bit) =
                        shortArrayToRegisterArray(0x4, 0xFFFF, 0xFC14), 1 },
                new Object[] { new DecimalType("-1004"), ValueType.INT32,
                        // -1004 = 0xFFFFFC14 (32bit) =
                        shortArrayToRegisterArray(0xFFFF, 0xFC14, 0x4), 0 },
                new Object[] { IllegalArgumentException.class, ValueType.INT32, shortArrayToRegisterArray(4, -1004),
                        1 },
                new Object[] { IllegalArgumentException.class, ValueType.INT32, shortArrayToRegisterArray(4, -1004),
                        2 },
                new Object[] { IllegalArgumentException.class, ValueType.INT32, shortArrayToRegisterArray(0, 0, 0), 2 },
                //
                // UINT32
                //
                new Object[] { new DecimalType("1.0"), ValueType.UINT32, shortArrayToRegisterArray(0, 1), 0 },
                new Object[] { new DecimalType("2.0"), ValueType.UINT32, shortArrayToRegisterArray(0, 2), 0 },
                new Object[] { new DecimalType("4294966292"), ValueType.UINT32,
                        // 4294966292 = 0xFFFFFC14 (32bit) =
                        shortArrayToRegisterArray(0xFFFF, 0xFC14), 0 },
                new Object[] { new DecimalType("64000"), ValueType.UINT32, shortArrayToRegisterArray(0, 64000), 0 },
                new Object[] {
                        // out of bounds of unsigned 16bit (0 to 65,535)
                        new DecimalType("70004"),
                        // 70004 -> 0x00011174 (32bit) -> 0x1174 (16bit)
                        ValueType.UINT32, shortArrayToRegisterArray(1, 4468), 0 },
                new Object[] { new DecimalType("4294966292"), ValueType.UINT32,
                        // 4294966292 = 0xFFFFFC14 (32bit) =
                        shortArrayToRegisterArray(0xFFFF, 0xFC14, 0x5), 0 },
                new Object[] { new DecimalType("4294966292"), ValueType.UINT32,
                        // 4294966292 = 0xFFFFFC14 (32bit) =
                        shortArrayToRegisterArray(0x5, 0xFFFF, 0xFC14), 1 },
                new Object[] { IllegalArgumentException.class, ValueType.UINT32, shortArrayToRegisterArray(4, -1004),
                        1 },
                new Object[] { IllegalArgumentException.class, ValueType.UINT32, shortArrayToRegisterArray(4, -1004),
                        2 },
                new Object[] { IllegalArgumentException.class, ValueType.UINT32, shortArrayToRegisterArray(0, 0, 0),
                        2 },
                //
                // INT32_SWAP
                //
                new Object[] { new DecimalType("1.0"), ValueType.INT32_SWAP, shortArrayToRegisterArray(1, 0), 0 },
                new Object[] { new DecimalType("2.0"), ValueType.INT32_SWAP, shortArrayToRegisterArray(2, 0), 0 },
                new Object[] { new DecimalType("-1004"), ValueType.INT32_SWAP,
                        // -1004 = 0xFFFFFC14 (32bit) =
                        shortArrayToRegisterArray(0xFC14, 0xFFFF), 0 },
                new Object[] { new DecimalType("64000"), ValueType.INT32_SWAP, shortArrayToRegisterArray(64000, 0), 0 },
                new Object[] { new DecimalType("-1004"), ValueType.INT32_SWAP,
                        // -1004 = 0xFFFFFC14 (32bit) =
                        shortArrayToRegisterArray(0x4, 0xFC14, 0xFFFF), 1 },
                new Object[] { new DecimalType("-1004"), ValueType.INT32_SWAP,
                        // -1004 = 0xFFFFFC14 (32bit) =
                        shortArrayToRegisterArray(0xFC14, 0xFFFF, 0x4), 0 },
                new Object[] { IllegalArgumentException.class, ValueType.INT32_SWAP,
                        shortArrayToRegisterArray(4, -1004), 1 },
                new Object[] { IllegalArgumentException.class, ValueType.INT32_SWAP,
                        shortArrayToRegisterArray(4, -1004), 2 },
                new Object[] { IllegalArgumentException.class, ValueType.INT32_SWAP, shortArrayToRegisterArray(0, 0, 0),
                        2 },
                //
                // UINT32_SWAP
                //
                new Object[] { new DecimalType("1.0"), ValueType.UINT32_SWAP, shortArrayToRegisterArray(1, 0), 0 },
                new Object[] { new DecimalType("2.0"), ValueType.UINT32_SWAP, shortArrayToRegisterArray(2, 0), 0 },
                new Object[] { new DecimalType("4294966292"), ValueType.UINT32_SWAP,
                        // 4294966292 = 0xFFFFFC14 (32bit) =
                        shortArrayToRegisterArray(0xFC14, 0xFFFF), 0 },
                new Object[] { new DecimalType("64000"), ValueType.UINT32_SWAP, shortArrayToRegisterArray(64000, 0),
                        0 },
                new Object[] {
                        // out of bounds of unsigned 16bit (0 to 65,535)
                        new DecimalType("70004"),
                        // 70004 -> 0x00011174 (32bit) -> 0x1174 (16bit)
                        ValueType.UINT32_SWAP, shortArrayToRegisterArray(4468, 1), 0 },
                new Object[] { new DecimalType("4294966292"), ValueType.UINT32_SWAP,
                        // 4294966292 = 0xFFFFFC14 (32bit) =
                        shortArrayToRegisterArray(0xFC14, 0xFFFF, 0x5), 0 },
                new Object[] { new DecimalType("4294966292"), ValueType.UINT32_SWAP,
                        // 4294966292 = 0xFFFFFC14 (32bit) =
                        shortArrayToRegisterArray(0x5, 0xFC14, 0xFFFF), 1 },
                new Object[] { IllegalArgumentException.class, ValueType.UINT32_SWAP,
                        shortArrayToRegisterArray(4, -1004), 1 },
                new Object[] { IllegalArgumentException.class, ValueType.UINT32_SWAP,
                        shortArrayToRegisterArray(4, -1004), 2 },
                new Object[] { IllegalArgumentException.class, ValueType.UINT32_SWAP,
                        shortArrayToRegisterArray(0, 0, 0), 2 },
                //
                // FLOAT32
                //
                new Object[] { new DecimalType("1.0"), ValueType.FLOAT32, shortArrayToRegisterArray(0x3F80, 0x0000),
                        0 },
                new Object[] { new DecimalType(1.6f), ValueType.FLOAT32, shortArrayToRegisterArray(0x3FCC, 0xCCCD), 0 },
                new Object[] { new DecimalType(2.6f), ValueType.FLOAT32, shortArrayToRegisterArray(0x4026, 0x6666), 0 },
                new Object[] { new DecimalType(-1004.4f), ValueType.FLOAT32, shortArrayToRegisterArray(0xC47B, 0x199A),
                        0 },
                new Object[] { new DecimalType("64000"), ValueType.FLOAT32, shortArrayToRegisterArray(0x477A, 0x0000),
                        0 },
                new Object[] {
                        // out of bounds of unsigned 16bit (0 to 65,535)
                        new DecimalType(70004.4f), ValueType.FLOAT32, shortArrayToRegisterArray(0x4788, 0xBA33), 0 },
                new Object[] {
                        // out of bounds of unsigned 32bit (0 to 4,294,967,295)
                        new DecimalType("5000000000"), ValueType.FLOAT32, shortArrayToRegisterArray(0x4F95, 0x02F9),
                        0 },
                new Object[] { new DecimalType(-1004.4f), ValueType.FLOAT32,
                        shortArrayToRegisterArray(0x4, 0xC47B, 0x199A), 1 },
                new Object[] { new DecimalType(-1004.4f), ValueType.FLOAT32,
                        shortArrayToRegisterArray(0xC47B, 0x199A, 0x4), 0 },
                new Object[] { // equivalent of NaN
                        Optional.empty(), ValueType.FLOAT32, shortArrayToRegisterArray(0x7fc0, 0x0000), 0 },
                new Object[] { new DecimalType(-1004.4f), ValueType.FLOAT32,
                        shortArrayToRegisterArray(0x4, 0x0, 0x0, 0x0, 0xC47B, 0x199A), 4 },
                new Object[] { IllegalArgumentException.class, ValueType.FLOAT32, shortArrayToRegisterArray(4, -1004),
                        1 },
                new Object[] { IllegalArgumentException.class, ValueType.FLOAT32, shortArrayToRegisterArray(4, -1004),
                        2 },
                new Object[] { IllegalArgumentException.class, ValueType.FLOAT32, shortArrayToRegisterArray(0, 0, 0),
                        2 },
                //
                // FLOAT32_SWAP
                //
                new Object[] { new DecimalType("1.0"), ValueType.FLOAT32_SWAP,
                        shortArrayToRegisterArray(0x0000, 0x3F80), 0 },
                new Object[] { new DecimalType(1.6f), ValueType.FLOAT32_SWAP, shortArrayToRegisterArray(0xCCCD, 0x3FCC),
                        0 },
                new Object[] { new DecimalType(2.6f), ValueType.FLOAT32_SWAP, shortArrayToRegisterArray(0x6666, 0x4026),
                        0 },
                new Object[] { new DecimalType(-1004.4f), ValueType.FLOAT32_SWAP,
                        shortArrayToRegisterArray(0x199A, 0xC47B), 0 },
                new Object[] { new DecimalType("64000"), ValueType.FLOAT32_SWAP,
                        shortArrayToRegisterArray(0x0000, 0x477A), 0 },
                new Object[] { // equivalent of NaN
                        Optional.empty(), ValueType.FLOAT32_SWAP, shortArrayToRegisterArray(0x0000, 0x7fc0), 0 },
                new Object[] {
                        // out of bounds of unsigned 16bit (0 to 65,535)
                        new DecimalType(70004.4f), ValueType.FLOAT32_SWAP, shortArrayToRegisterArray(0xBA33, 0x4788),
                        0 },
                new Object[] {
                        // out of bounds of unsigned 32bit (0 to 4,294,967,295)
                        new DecimalType("5000000000"), ValueType.FLOAT32_SWAP,
                        shortArrayToRegisterArray(0x02F9, 0x4F95), 0 },
                new Object[] { new DecimalType(-1004.4f), ValueType.FLOAT32_SWAP,
                        shortArrayToRegisterArray(0x4, 0x199A, 0xC47B), 1 },
                new Object[] { new DecimalType(-1004.4f), ValueType.FLOAT32_SWAP,
                        shortArrayToRegisterArray(0x199A, 0xC47B, 0x4), 0 },
                new Object[] { IllegalArgumentException.class, ValueType.FLOAT32_SWAP,
                        shortArrayToRegisterArray(4, -1004), 1 },
                new Object[] { IllegalArgumentException.class, ValueType.FLOAT32_SWAP,
                        shortArrayToRegisterArray(4, -1004), 2 },
                new Object[] { IllegalArgumentException.class, ValueType.FLOAT32_SWAP,
                        shortArrayToRegisterArray(0, 0, 0), 2 },

                //
                // INT64
                //
                new Object[] { new DecimalType("1.0"), ValueType.INT64, shortArrayToRegisterArray(0, 0, 0, 1), 0 },
                new Object[] { new DecimalType("2.0"), ValueType.INT64, shortArrayToRegisterArray(0, 0, 0, 2), 0 },
                new Object[] { new DecimalType("-1004"), ValueType.INT64,
                        shortArrayToRegisterArray(0xFFFF, 0xFFFF, 0xFFFF, 0xFC14), 0 },
                new Object[] { new DecimalType("64000"), ValueType.INT64, shortArrayToRegisterArray(0, 0, 0, 64000),
                        0 },
                new Object[] {
                        // out of bounds of unsigned 32bit
                        new DecimalType("34359738368"), ValueType.INT64, shortArrayToRegisterArray(0x0, 0x8, 0x0, 0x0),
                        0 },
                new Object[] { new DecimalType("-2322243636186679031"), ValueType.INT64,
                        shortArrayToRegisterArray(0xDFC5, 0xBBB7, 0x772E, 0x7909), 0 },
                // would read over the registers
                new Object[] { IllegalArgumentException.class, ValueType.INT64,
                        shortArrayToRegisterArray(0xDFC5, 0xBBB7, 0x772E, 0x7909), 1 },
                // would read over the registers
                new Object[] { IllegalArgumentException.class, ValueType.INT64,
                        shortArrayToRegisterArray(0xDFC5, 0xBBB7, 0x772E, 0x7909), 2 },
                // 4 registers expected, only 3 available
                new Object[] { IllegalArgumentException.class, ValueType.INT64,
                        shortArrayToRegisterArray(0xDFC5, 0xBBB7, 0x772E), 0 },

                //
                // UINT64
                //
                new Object[] { new DecimalType("1.0"), ValueType.UINT64, shortArrayToRegisterArray(0, 0, 0, 1), 0 },
                new Object[] { new DecimalType("2.0"), ValueType.UINT64, shortArrayToRegisterArray(0, 0, 0, 2), 0 },
                new Object[] { new DecimalType("18446744073709550612"), ValueType.UINT64,
                        shortArrayToRegisterArray(0xFFFF, 0xFFFF, 0xFFFF, 0xFC14), 0 },
                new Object[] { new DecimalType("64000"), ValueType.UINT64, shortArrayToRegisterArray(0, 0, 0, 64000),
                        0 },
                new Object[] {
                        // out of bounds of unsigned 32bit
                        new DecimalType("34359738368"), ValueType.UINT64, shortArrayToRegisterArray(0x0, 0x8, 0x0, 0x0),
                        0 },
                new Object[] { new DecimalType("16124500437522872585"), ValueType.UINT64,
                        shortArrayToRegisterArray(0xDFC5, 0xBBB7, 0x772E, 0x7909), 0 },

                //
                // INT64_SWAP
                //
                new Object[] { new DecimalType("1.0"), ValueType.INT64_SWAP, shortArrayToRegisterArray(1, 0, 0, 0), 0 },
                new Object[] { new DecimalType("2.0"), ValueType.INT64_SWAP, shortArrayToRegisterArray(2, 0, 0, 0), 0 },
                new Object[] { new DecimalType("-1004"), ValueType.INT64_SWAP,
                        shortArrayToRegisterArray(0xFC14, 0xFFFF, 0xFFFF, 0xFFFF), 0 },
                new Object[] { new DecimalType("64000"), ValueType.INT64_SWAP,
                        shortArrayToRegisterArray(64000, 0, 0, 0), 0 },
                new Object[] {
                        // out of bounds of unsigned 32bit
                        new DecimalType("34359738368"),
                        // 70004 -> 0x00011174 (32bit) -> 0x1174 (16bit)
                        ValueType.INT64_SWAP, shortArrayToRegisterArray(0x0, 0x0, 0x8, 0x0), 0 },
                new Object[] { new DecimalType("-2322243636186679031"), ValueType.INT64_SWAP,

                        shortArrayToRegisterArray(0x7909, 0x772E, 0xBBB7, 0xDFC5), 0 },

                //
                // UINT64_SWAP
                //
                new Object[] { new DecimalType("1.0"), ValueType.UINT64_SWAP, shortArrayToRegisterArray(1, 0, 0, 0),
                        0 },
                new Object[] { new DecimalType("2.0"), ValueType.UINT64_SWAP, shortArrayToRegisterArray(2, 0, 0, 0),
                        0 },
                new Object[] { new DecimalType("18446744073709550612"), ValueType.UINT64_SWAP,
                        shortArrayToRegisterArray(0xFC14, 0xFFFF, 0xFFFF, 0xFFFF), 0 },
                new Object[] { new DecimalType("64000"), ValueType.UINT64_SWAP,
                        shortArrayToRegisterArray(64000, 0, 0, 0), 0 },
                new Object[] {
                        // out of bounds of unsigned 32bit
                        new DecimalType("34359738368"), ValueType.UINT64_SWAP,
                        shortArrayToRegisterArray(0x0, 0x0, 0x8, 0x0), 0 },
                new Object[] {
                        // out of bounds of unsigned 64bit
                        new DecimalType("16124500437522872585"), ValueType.UINT64_SWAP,
                        shortArrayToRegisterArray(0x7909, 0x772E, 0xBBB7, 0xDFC5), 0 })
                .collect(Collectors.toList()));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @ParameterizedTest
    @MethodSource("data")
    public void testextractStateFromRegisters(Object expectedResult, ValueType type, ModbusRegisterArray registers,
            int index) {
        if (expectedResult instanceof Class && Exception.class.isAssignableFrom((Class) expectedResult)) {
            assertThrows((Class) expectedResult,
                    () -> ModbusBitUtilities.extractStateFromRegisters(registers, index, type));
            return;
        }

        Optional<@NonNull DecimalType> actualState = ModbusBitUtilities.extractStateFromRegisters(registers, index,
                type);
        // Wrap given expectedResult to Optional, if necessary
        Optional<@NonNull DecimalType> expectedStateWrapped = expectedResult instanceof DecimalType
                ? Optional.of((DecimalType) expectedResult)
                : (Optional<@NonNull DecimalType>) expectedResult;
        assertThat(String.format("registers=%s, index=%d, type=%s", registers, index, type), actualState,
                is(equalTo(expectedStateWrapped)));
    }
}
