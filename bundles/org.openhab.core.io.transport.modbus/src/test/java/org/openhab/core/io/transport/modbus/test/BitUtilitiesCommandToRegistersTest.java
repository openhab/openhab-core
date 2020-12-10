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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.NotImplementedException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.core.io.transport.modbus.ModbusBitUtilities;
import org.openhab.core.io.transport.modbus.ModbusConstants.ValueType;
import org.openhab.core.io.transport.modbus.ModbusRegisterArray;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.types.Command;

/**
 * @author Sami Salonen - Initial contribution
 */
public class BitUtilitiesCommandToRegistersTest {

    private static short[] shorts(int... ints) {
        short[] shorts = new short[ints.length];
        for (int i = 0; i < ints.length; i++) {
            short s = (short) ints[i];
            shorts[i] = s;
        }
        return shorts;
    }

    public static Collection<Object[]> data() {
        return Collections.unmodifiableList(Stream
                .of(new Object[] { new DecimalType("1.0"), ValueType.BIT, IllegalArgumentException.class },
                        new Object[] { new DecimalType("1.0"), ValueType.INT8, IllegalArgumentException.class },
                        //
                        // INT16
                        //
                        new Object[] { new DecimalType("1.0"), ValueType.INT16, shorts(1) },
                        new Object[] { new DecimalType("1.6"), ValueType.INT16, shorts(1) },
                        new Object[] { new DecimalType("2.6"), ValueType.INT16, shorts(2) },
                        new Object[] { new DecimalType("-1004.4"), ValueType.INT16, shorts(-1004), },
                        // within bounds for signed int16
                        new Object[] { new DecimalType("32000"), ValueType.INT16, shorts(32000), },
                        new Object[] { new DecimalType("-32000"), ValueType.INT16, shorts(-32000), },
                        // out bounds for signed int16, but not for uint16
                        new Object[] { new DecimalType("60000"), ValueType.INT16, shorts(60000), },
                        new Object[] { new DecimalType("64000"), ValueType.INT16, shorts(64000), }, //
                        new Object[] {
                                // out of bounds of unsigned 16bit (0 to 65,535)
                                new DecimalType("70004.4"),
                                // 70004 -> 0x00011174 (int) -> 0x1174 (short) = 4468
                                ValueType.INT16, shorts(4468), },
                        //
                        // UINT16 (same as INT16)
                        //
                        new Object[] { new DecimalType("1.0"), ValueType.UINT16, shorts(1) },
                        new Object[] { new DecimalType("1.6"), ValueType.UINT16, shorts(1) },
                        new Object[] { new DecimalType("2.6"), ValueType.UINT16, shorts(2) },
                        new Object[] { new DecimalType("-1004.4"), ValueType.UINT16, shorts(-1004), },
                        // within bounds for signed int16
                        new Object[] { new DecimalType("32000"), ValueType.UINT16, shorts(32000), },
                        new Object[] { new DecimalType("-32000"), ValueType.UINT16, shorts(-32000), },
                        // out bounds for signed int16, but not for uint16
                        new Object[] { new DecimalType("60000"), ValueType.UINT16, shorts(60000), },
                        new Object[] { new DecimalType("64000"), ValueType.UINT16, shorts(64000), }, //
                        new Object[] {
                                // out of bounds of unsigned 16bit (0 to 65,535)
                                new DecimalType("70004.4"),
                                // 70004 -> 0x00011174 (32bit) -> 0x1174 (16bit)
                                ValueType.UINT16, shorts(0x1174), },
                        //
                        // INT32
                        //
                        new Object[] { new DecimalType("1.0"), ValueType.INT32, shorts(0, 1) },
                        new Object[] { new DecimalType("1.6"), ValueType.INT32, shorts(0, 1) },
                        new Object[] { new DecimalType("2.6"), ValueType.INT32, shorts(0, 2) },
                        new Object[] { new DecimalType("-1004.4"), ValueType.INT32,
                                // -1004 = 0xFFFFFC14 (32bit) =
                                shorts(0xFFFF, 0xFC14), },
                        new Object[] { new DecimalType("64000"), ValueType.INT32, shorts(0, 64000), }, //
                        // within signed int32 range: +-2,000,000,00
                        new Object[] { new DecimalType("-2000000000"), ValueType.INT32, shorts(0x88CA, 0x6C00), },
                        new Object[] { new DecimalType("2000000000"), ValueType.INT32, shorts(0x7735, 0x9400), },
                        // out bounds for signed int32, but not for uint32
                        new Object[] { new DecimalType("3000000000"), ValueType.INT32, shorts(0xB2D0, 0x5E00), }, //
                        new Object[] {
                                // out of bounds of unsigned 32bit (0 to 4,294,967,295)
                                new DecimalType("5000000000"),
                                // 5000000000 -> 0x12a05f200 () -> 0x1174 (16bit)
                                ValueType.INT32, shorts(0x2a05, 0xf200), },
                        //
                        // UINT32 (same as INT32)
                        //
                        new Object[] { new DecimalType("1.0"), ValueType.UINT32, shorts(0, 1) },
                        new Object[] { new DecimalType("1.6"), ValueType.UINT32, shorts(0, 1) },
                        new Object[] { new DecimalType("2.6"), ValueType.UINT32, shorts(0, 2) },
                        new Object[] { new DecimalType("-1004.4"), ValueType.UINT32,
                                // -1004 = 0xFFFFFC14 (32bit) =
                                shorts(0xFFFF, 0xFC14), },
                        new Object[] { new DecimalType("64000"), ValueType.UINT32, shorts(0, 64000), }, //
                        // within signed int32 range: +-2,000,000,00
                        new Object[] { new DecimalType("-2000000000"), ValueType.UINT32, shorts(0x88CA, 0x6C00), },
                        new Object[] { new DecimalType("2000000000"), ValueType.UINT32, shorts(0x7735, 0x9400), },
                        // out bounds for signed int32, but not for uint32
                        new Object[] { new DecimalType("3000000000"), ValueType.UINT32, shorts(0xB2D0, 0x5E00), }, //
                        new Object[] {
                                // out of bounds of unsigned 32bit (0 to 4,294,967,295)
                                new DecimalType("5000000000"),
                                // 5000000000 -> 0x12a05f200 () -> 0x1174 (16bit)
                                ValueType.UINT32, shorts(0x2a05, 0xf200), },
                        //
                        // INT32_SWAP
                        //
                        new Object[] { new DecimalType("1.0"), ValueType.INT32_SWAP, shorts(1, 0) },
                        new Object[] { new DecimalType("1.6"), ValueType.INT32_SWAP, shorts(1, 0) },
                        new Object[] { new DecimalType("2.6"), ValueType.INT32_SWAP, shorts(2, 0) },
                        new Object[] { new DecimalType("-1004.4"), ValueType.INT32_SWAP,
                                // -1004 = 0xFFFFFC14 (32bit)
                                shorts(0xFC14, 0xFFFF), },
                        new Object[] { new DecimalType("64000"), ValueType.INT32_SWAP, shorts(64000, 0), },
                        // within signed int32 range: +-2,000,000,00
                        new Object[] { new DecimalType("-2000000000"), ValueType.INT32_SWAP, shorts(0x6C00, 0x88CA), },
                        new Object[] { new DecimalType("2000000000"), ValueType.INT32_SWAP, shorts(0x9400, 0x7735), },
                        // out bounds for signed int32, but not for uint32
                        new Object[] { new DecimalType("3000000000"), ValueType.INT32_SWAP, shorts(0x5E00, 0xB2D0), }, //
                        new Object[] {
                                // out of bounds of unsigned 32bit (0 to 4,294,967,295)
                                new DecimalType("5000000000"),
                                // 5000000000 -> 0x12a05f200
                                ValueType.INT32_SWAP, shorts(0xf200, 0x2a05), },
                        //
                        // UINT32_SWAP (same as INT32_SWAP)
                        //
                        new Object[] { new DecimalType("1.0"), ValueType.UINT32_SWAP, shorts(1, 0) },
                        new Object[] { new DecimalType("1.6"), ValueType.UINT32_SWAP, shorts(1, 0) },
                        new Object[] { new DecimalType("2.6"), ValueType.UINT32_SWAP, shorts(2, 0) },
                        new Object[] { new DecimalType("-1004.4"), ValueType.UINT32_SWAP,
                                // -1004 = 0xFFFFFC14 (32bit)
                                shorts(0xFC14, 0xFFFF), },
                        new Object[] { new DecimalType("64000"), ValueType.UINT32_SWAP, shorts(64000, 0), },
                        // within signed int32 range: +-2,000,000,00
                        new Object[] { new DecimalType("-2000000000"), ValueType.UINT32_SWAP, shorts(0x6C00, 0x88CA), },
                        new Object[] { new DecimalType("2000000000"), ValueType.UINT32_SWAP, shorts(0x9400, 0x7735), },
                        // out bounds for signed int32, but not for uint32
                        new Object[] { new DecimalType("3000000000"), ValueType.UINT32_SWAP, shorts(0x5E00, 0xB2D0), }, //
                        new Object[] {
                                // out of bounds of unsigned 32bit (0 to 4,294,967,295)
                                new DecimalType("5000000000"),
                                // 5000000000 -> 0x12a05f200
                                ValueType.UINT32_SWAP, shorts(0xf200, 0x2a05), },
                        //
                        // FLOAT32
                        //
                        new Object[] { new DecimalType("1.0"), ValueType.FLOAT32, shorts(0x3F80, 0x0000) },
                        new Object[] { new DecimalType("1.6"), ValueType.FLOAT32, shorts(0x3FCC, 0xCCCD) },
                        new Object[] { new DecimalType("2.6"), ValueType.FLOAT32, shorts(0x4026, 0x6666) },
                        new Object[] { new DecimalType("-1004.4"), ValueType.FLOAT32, shorts(0xC47B, 0x199A), },
                        new Object[] { new DecimalType("64000"), ValueType.FLOAT32, shorts(0x477A, 0x0000), },
                        new Object[] {
                                // out of bounds of unsigned 16bit (0 to 65,535)
                                new DecimalType("70004.4"), ValueType.FLOAT32, shorts(0x4788, 0xBA33), },
                        new Object[] {
                                // out of bounds of unsigned 32bit (0 to 4,294,967,295)
                                new DecimalType("5000000000"), ValueType.FLOAT32, shorts(0x4F95, 0x02F9), },
                        //
                        // FLOAT32_SWAP
                        //
                        new Object[] { new DecimalType("1.0"), ValueType.FLOAT32_SWAP, shorts(0x0000, 0x3F80) },
                        new Object[] { new DecimalType("1.6"), ValueType.FLOAT32_SWAP, shorts(0xCCCD, 0x3FCC) },
                        new Object[] { new DecimalType("2.6"), ValueType.FLOAT32_SWAP, shorts(0x6666, 0x4026) },
                        new Object[] { new DecimalType("-1004.4"), ValueType.FLOAT32_SWAP, shorts(0x199A, 0xC47B), },
                        new Object[] { new DecimalType("64000"), ValueType.FLOAT32_SWAP, shorts(0x0000, 0x477A), },
                        new Object[] {
                                // out of bounds of unsigned 16bit (0 to 65,535)
                                new DecimalType("70004.4"), ValueType.FLOAT32_SWAP, shorts(0xBA33, 0x4788), },
                        new Object[] {
                                // out of bounds of unsigned 32bit (0 to 4,294,967,295)
                                new DecimalType("5000000000"), ValueType.FLOAT32_SWAP, shorts(0x02F9, 0x4F95) },
                        // ON/OFF
                        new Object[] { OnOffType.ON, ValueType.FLOAT32_SWAP, shorts(0x0000, 0x3F80) },
                        new Object[] { OnOffType.OFF, ValueType.FLOAT32_SWAP, shorts(0x0000, 0x0000) },
                        // OPEN
                        new Object[] { OpenClosedType.OPEN, ValueType.FLOAT32_SWAP, shorts(0x0000, 0x3F80) },
                        new Object[] { OpenClosedType.OPEN, ValueType.INT16, shorts(1) },
                        // CLOSED
                        new Object[] { OpenClosedType.CLOSED, ValueType.FLOAT32_SWAP, shorts(0x0000, 0x0000) },
                        new Object[] { OpenClosedType.CLOSED, ValueType.INT16, shorts(0x0000) },
                        // Unsupported command
                        new Object[] { IncreaseDecreaseType.INCREASE, ValueType.FLOAT32_SWAP,
                                NotImplementedException.class },

                        //
                        // INT64
                        //
                        new Object[] { new DecimalType("1.0"), ValueType.INT64, shorts(0, 0, 0, 1) },
                        new Object[] { new DecimalType("1.6"), ValueType.INT64, shorts(0, 0, 0, 1) },
                        new Object[] { new DecimalType("2.6"), ValueType.INT64, shorts(0, 0, 0, 2) },
                        new Object[] { new DecimalType("-1004.4"), ValueType.INT64,
                                shorts(0xFFFF, 0xFFFF, 0xFFFF, 0xFC14), },
                        new Object[] { new DecimalType("64000"), ValueType.INT64, shorts(0, 0, 0, 64000), },
                        new Object[] {
                                // out of bounds of unsigned 32bit
                                new DecimalType("34359738368"),
                                // 70004 -> 0x00011174 (32bit) -> 0x1174 (16bit)
                                ValueType.INT64, shorts(0x0, 0x8, 0x0, 0x0), },
                        // within signed int64 range: +-9,200,000,000,000,000,000
                        new Object[] { new DecimalType("-9200000000000000000"), ValueType.INT64,
                                shorts(0x8053, 0x08BE, 0x6268, 0x0000), },
                        new Object[] { new DecimalType("9200000000000000000"), ValueType.INT64,
                                shorts(0x7FAC, 0xF741, 0x9D98, 0x0000), },
                        // within unsigned int64 range (but out of range for signed int64)
                        new Object[] { new DecimalType("18200000000000000000"), ValueType.INT64,
                                shorts(0xFC93, 0x6392, 0x801C, 0x0000), },
                        new Object[] {
                                // out of bounds of unsigned 64bit
                                new DecimalType("3498348904359085439088905"),
                                // should pick the low 64 bits
                                ValueType.INT64, shorts(0xDFC5, 0xBBB7, 0x772E, 0x7909), },

                        //
                        // UINT64 (same as INT64)
                        //
                        new Object[] { new DecimalType("1.0"), ValueType.UINT64, shorts(0, 0, 0, 1) },
                        new Object[] { new DecimalType("1.6"), ValueType.UINT64, shorts(0, 0, 0, 1) },
                        new Object[] { new DecimalType("2.6"), ValueType.UINT64, shorts(0, 0, 0, 2) },
                        new Object[] { new DecimalType("-1004.4"), ValueType.UINT64,
                                shorts(0xFFFF, 0xFFFF, 0xFFFF, 0xFC14), },
                        new Object[] { new DecimalType("64000"), ValueType.UINT64, shorts(0, 0, 0, 64000), },
                        new Object[] {
                                // out of bounds of unsigned 32bit
                                new DecimalType("34359738368"),
                                // 70004 -> 0x00011174 (32bit) -> 0x1174 (16bit)
                                ValueType.UINT64, shorts(0x0, 0x8, 0x0, 0x0), },
                        // within signed int64 range: +-9,200,000,000,000,000,000
                        new Object[] { new DecimalType("-9200000000000000000"), ValueType.UINT64,
                                shorts(0x8053, 0x08BE, 0x6268, 0x0000), },
                        new Object[] { new DecimalType("9200000000000000000"), ValueType.UINT64,
                                shorts(0x7FAC, 0xF741, 0x9D98, 0x0000), },
                        // within unsigned int64 range (but out of range for signed int64)
                        new Object[] { new DecimalType("18200000000000000000"), ValueType.UINT64,
                                shorts(0xFC93, 0x6392, 0x801C, 0x0000), },
                        new Object[] {
                                // out of bounds of unsigned 64bit
                                new DecimalType("3498348904359085439088905"),
                                // should pick the low 64 bits
                                ValueType.UINT64, shorts(0xDFC5, 0xBBB7, 0x772E, 0x7909), },

                        //
                        // INT64_SWAP
                        //
                        new Object[] { new DecimalType("1.0"), ValueType.INT64_SWAP, shorts(1, 0, 0, 0) },
                        new Object[] { new DecimalType("1.6"), ValueType.INT64_SWAP, shorts(1, 0, 0, 0) },
                        new Object[] { new DecimalType("2.6"), ValueType.INT64_SWAP, shorts(2, 0, 0, 0) },
                        new Object[] { new DecimalType("-1004.4"), ValueType.INT64_SWAP,
                                shorts(0xFC14, 0xFFFF, 0xFFFF, 0xFFFF), },
                        new Object[] { new DecimalType("64000"), ValueType.INT64_SWAP, shorts(64000, 0, 0, 0), },
                        new Object[] {
                                // out of bounds of unsigned 32bit
                                new DecimalType("34359738368"),
                                // 70004 -> 0x00011174 (32bit) -> 0x1174 (16bit)
                                ValueType.INT64_SWAP, shorts(0x0, 0x0, 0x8, 0x0), },
                        new Object[] {
                                // out of bounds of unsigned 64bit
                                new DecimalType("3498348904359085439088905"),
                                // should pick the low 64 bits
                                ValueType.INT64_SWAP, shorts(0x7909, 0x772E, 0xBBB7, 0xDFC5), },

                        //
                        // UINT64_SWAP (same as INT64_SWAP)
                        //
                        new Object[] { new DecimalType("1.0"), ValueType.UINT64_SWAP, shorts(1, 0, 0, 0) },
                        new Object[] { new DecimalType("1.6"), ValueType.UINT64_SWAP, shorts(1, 0, 0, 0) },
                        new Object[] { new DecimalType("2.6"), ValueType.UINT64_SWAP, shorts(2, 0, 0, 0) },
                        new Object[] { new DecimalType("-1004.4"), ValueType.UINT64_SWAP,
                                shorts(0xFC14, 0xFFFF, 0xFFFF, 0xFFFF), },
                        new Object[] { new DecimalType("64000"), ValueType.UINT64_SWAP, shorts(64000, 0, 0, 0), },
                        new Object[] {
                                // out of bounds of unsigned 32bit
                                new DecimalType("34359738368"),
                                // 70004 -> 0x00011174 (32bit) -> 0x1174 (16bit)
                                ValueType.UINT64_SWAP, shorts(0x0, 0x0, 0x8, 0x0), },
                        new Object[] {
                                // out of bounds of unsigned 64bit
                                new DecimalType("3498348904359085439088905"),
                                // should pick the low 64 bits
                                ValueType.UINT64_SWAP, shorts(0x7909, 0x772E, 0xBBB7, 0xDFC5), })
                .collect(Collectors.toList()));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @ParameterizedTest
    @MethodSource("data")
    public void testCommandToRegisters(Command command, ValueType type, Object expectedResult) {
        if (expectedResult instanceof Class && Exception.class.isAssignableFrom((Class) expectedResult)) {
            assertThrows((Class) expectedResult, () -> ModbusBitUtilities.commandToRegisters(command, type));
            return;
        }

        ModbusRegisterArray registers = ModbusBitUtilities.commandToRegisters(command, type);
        short[] expectedRegisters = (short[]) expectedResult;

        assertThat(String.format("register index command=%s, type=%s", command, type), registers.size(),
                is(equalTo(expectedRegisters.length)));
        for (int i = 0; i < expectedRegisters.length; i++) {
            int expectedRegisterDataUnsigned = expectedRegisters[i] & 0xffff;
            int actualUnsigned = registers.getRegister(i);

            assertThat(String.format("register index i=%d, command=%s, type=%s", i, command, type), actualUnsigned,
                    is(equalTo(expectedRegisterDataUnsigned)));
        }
    }
}
