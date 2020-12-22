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
import static org.openhab.core.io.transport.modbus.ModbusConstants.ValueType.*;

import java.math.BigDecimal;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.core.io.transport.modbus.ModbusBitUtilities;
import org.openhab.core.io.transport.modbus.ModbusConstants.ValueType;
import org.openhab.core.io.transport.modbus.ModbusRegisterArray;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.Command;

/**
 * @author Sami Salonen - Initial contribution
 */
@NonNullByDefault
public class BitUtilitiesCommandToRegistersTest {

    private static final int UINT16_MAX = 0xFFFF;

    /**
     * Convert given registers as shorts
     *
     * @param ints registers given as signed value representing the 16bit register values
     * @return
     */
    private static short[] shorts(int... ints) {
        short[] shorts = new short[ints.length];
        for (int i = 0; i < ints.length; i++) {
            int possiblyUnsigned = ints[i];
            if (possiblyUnsigned > UINT16_MAX) {
                throw new RuntimeException(
                        "One of the register values, " + possiblyUnsigned + ", is too large (max " + UINT16_MAX + ")");
            } else if (possiblyUnsigned < Short.MIN_VALUE) {
                throw new RuntimeException("One of the register values, " + possiblyUnsigned + ", is too small (min "
                        + Short.MIN_VALUE + "), would truncate to \" + low16Signed");
            }
            short low16Signed = (short) possiblyUnsigned;
            shorts[i] = low16Signed;

        }
        return shorts;
    }

    private static class Args implements Arguments {

        private Command command;
        private ValueType valueType;
        private Object expected;

        public Args(Object command, ValueType valueType, Object expected) {
            this.command = command instanceof String ? new DecimalType((String) command) : (Command) command;
            this.valueType = valueType;
            // validating type with cast
            if (expected instanceof Integer) {
                this.expected = shorts((int) expected);
            } else if (expected instanceof Class<?>) {
                this.expected = expected;
            } else {
                this.expected = shorts((int[]) expected);
            }
        }

        @Override
        public Object[] get() {
            return new Object[] { command, valueType, expected };
        }
    }

    private static class ArgsTemplate {
        public Object command;
        public Object expected;

        public ArgsTemplate(Object command, Object expected) {
            this.command = command;
            this.expected = expected;
        }
    }

    private static Stream<Args> parameterize(ValueType[] valueTypes, ArgsTemplate... templates) {
        Builder<Args> builder = Stream.builder();
        for (ValueType valueType : valueTypes) {
            for (ArgsTemplate template : templates) {
                builder.add(new Args(template.command, valueType, template.expected));
            }
        }
        return builder.build();
    }

    private static Stream<Object> concatTestArgs(Object... objects) {
        Builder<Object> builder = Stream.builder();
        for (Object obj : objects) {
            if (obj instanceof Args) {
                builder.add(obj);
            } else if (obj instanceof Stream) {
                ((Stream<?>) obj).forEach(builder::add);
            } else {
                throw new RuntimeException("Illegal parameter " + obj.toString());
            }
        }
        return builder.build();
    }

    private static ArgsTemplate a(Object command, Object expected) {
        return new ArgsTemplate(command, expected);
    }

    private static ArgsTemplate a(Object command, int... expected) {
        return new ArgsTemplate(command, expected);
    }

    private static Args a(Object command, ValueType valueType, Object expected) {
        return new Args(command, valueType, expected);
    }

    private static Args a(Object command, ValueType valueType, int... expected) {
        return new Args(command, valueType, expected);
    }

    /**
     * 2^power + increment
     */
    private static String powerOfTwo(int power, int increment) {
        return powerOfTwo(1, power, increment);
    }

    /**
     * (sign)*2^power + increment
     */
    private static String powerOfTwo(int sign, int power, int increment) {
        BigDecimal dec = new BigDecimal(2).pow(power);
        if (sign < 0) {
            dec = dec.negate();
        }
        dec = dec.add(new BigDecimal(increment));
        return new DecimalType(dec).toString();
    }

    /**
     * https://www.rapidtables.com/convert/number/decimal-to-hex.html
     *
     * @return
     */
    public static Stream<Object> data() {

        return concatTestArgs(//

                a("1.0", BIT, IllegalArgumentException.class), a("1.0", INT8, IllegalArgumentException.class),
                a("1.0", UINT8, IllegalArgumentException.class),
                //
                // INT16
                //
                parameterize(new ValueType[] { INT16, UINT16 }, a("1.0", 1), a("1.6", 1), a("2.6", 2),
                        a("-1004.4", 0xFC14),
                        // corner cases around signed max value
                        a(powerOfTwo(15, -2), 0x7ffe), // =sint_max-1.
                        a(powerOfTwo(15, -1), 0x7fff), // =sint_max. Only high bit set
                        a(powerOfTwo(15, 0), 0x8000), // = sint_max +1. Wraps to negative when truncated
                        a(powerOfTwo(15, 1), 0x8001), // = sint_max +2. Wraps to negative when truncated
                        // corner cases around signed min value
                        a(powerOfTwo(-1, 15, 1), 0x8001), // =sint_min+1
                        a(powerOfTwo(-1, 15, 0), 0x8000), // =sint_min. Only high bit set
                        a(powerOfTwo(-1, 15, -1), 0x7fff), // = sint_min -1. Wraps to positive when truncated
                        a(powerOfTwo(-1, 15, -2), 0x7ffe), // = sint_min -2. Wraps to positive when truncated
                        // corner cases around unsigned max value
                        a(powerOfTwo(16, -2), 0xfffe), // =uint_max-1.
                        a(powerOfTwo(16, -1), 0xffff), // =uint_max.
                        a(powerOfTwo(16, 0), 0x0000), // = uint_max+1.
                        a(powerOfTwo(16, 1), 0x0001) // = uint_max+2.
                ),

                //
                // INT32 and UINT32
                //
                parameterize(new ValueType[] { INT32, UINT32 }, a("1.0", 0, 1), a("1.6", 0, 1), a("2.6", 0, 2),
                        a("-1004.4", 0xFFFF, 0xFC14),
                        // corner cases around signed max value
                        a(powerOfTwo(31, -2), 0x7fff, 0xfffe), // =sint_max-1.
                        a(powerOfTwo(31, -1), 0x7fff, 0xffff), // =sint_max. Only high bit set
                        a(powerOfTwo(31, 0), 0x8000, 0x0000), // = sint_max +1. Wraps to negative when truncated
                        a(powerOfTwo(31, 1), 0x8000, 0x0001), // = sint_max +2. Wraps to negative when truncated
                        // corner cases around signed min value
                        a(powerOfTwo(-1, 31, 1), 0x8000, 0x0001), // =sint_min+1
                        a(powerOfTwo(-1, 31, 0), 0x8000, 0x0000), // =sint_min. Only high bit set
                        a(powerOfTwo(-1, 31, -1), 0x7fff, 0xffff), // = sint_min -1. Wraps to positive when truncated
                        a(powerOfTwo(-1, 31, -2), 0x7fff, 0xfffe), // = sint_min -2. Wraps to positive when truncated
                        // corner cases around unsigned max value
                        a(powerOfTwo(32, -2), 0xffff, 0xfffe), // =uint_max-1.
                        a(powerOfTwo(32, -1), 0xffff, 0xffff), // =uint_max.
                        a(powerOfTwo(32, 0), 0x0000, 0x0000), // = uint_max+1.
                        a(powerOfTwo(32, 1), 0x0000, 0x0001) // = uint_max+2.

                ),
                //
                // INT32_SWAP and UINT32_SWAP
                //
                parameterize(new ValueType[] { INT32_SWAP, UINT32_SWAP }, a("1.0", 1, 0), a("1.6", 1, 0),
                        a("2.6", 2, 0), a("-1004.4", 0xFC14, 0xFFFF),
                        // corner cases around signed max value
                        a(powerOfTwo(31, -2), 0xfffe, 0x7fff), // =sint_max-1.
                        a(powerOfTwo(31, -1), 0xffff, 0x7fff), // =sint_max. Only high bit set
                        a(powerOfTwo(31, 0), 0x0000, 0x8000), // = sint_max +1. Wraps to negative when truncated
                        a(powerOfTwo(31, 1), 0x0001, 0x8000), // = sint_max +2. Wraps to negative when truncated
                        // corner cases around signed min value
                        a(powerOfTwo(-1, 31, 1), 0x0001, 0x8000), // =sint_min+1
                        a(powerOfTwo(-1, 31, 0), 0x0000, 0x8000), // =sint_min. Only high bit set
                        a(powerOfTwo(-1, 31, -1), 0xffff, 0x7fff), // = sint_min -1. Wraps to positive when truncated
                        a(powerOfTwo(-1, 31, -2), 0xfffe, 0x7fff), // = sint_min -2. Wraps to positive when truncated
                        // corner cases around unsigned max value
                        a(powerOfTwo(32, -2), 0xfffe, 0xffff), // =uint_max-1.
                        a(powerOfTwo(32, -1), 0xffff, 0xffff), // =uint_max.
                        a(powerOfTwo(32, 0), 0x0000, 0x0000), // = uint_max+1.
                        a(powerOfTwo(32, 1), 0x0001, 0x0000) // = uint_max+2.

                ),
                //
                // INT64 and UINT64
                //
                parameterize(new ValueType[] { INT64, UINT64 }, a("1.0", 0, 0, 0, 1), a("1.6", 0, 0, 0, 1),
                        a("2.6", 0, 0, 0, 2), a("-1004.4", 0xFFFF, 0xFFFF, 0xFFFF, 0xFC14),
                        // corner cases around signed max value
                        a(powerOfTwo(63, -2), 0x7fff, 0xffff, 0xffff, 0xfffe), // =sint_max-1.
                        a(powerOfTwo(63, -1), 0x7fff, 0xffff, 0xffff, 0xffff), // =sint_max. Only high bit set
                        a(powerOfTwo(63, 0), 0x8000, 0x0000, 0x0000, 0x0000), // = sint_max +1. Wraps to negative when
                                                                              // truncated
                        a(powerOfTwo(63, 1), 0x8000, 0x0000, 0x0000, 0x0001), // = sint_max +2. Wraps to negative when
                                                                              // truncated
                        // corner cases around signed min value
                        a(powerOfTwo(-1, 63, 1), 0x8000, 0x0000, 0x0000, 0x0001), // =sint_min+1
                        a(powerOfTwo(-1, 63, 0), 0x8000, 0x0000, 0x0000, 0x0000), // =sint_min. Only high bit set
                        a(powerOfTwo(-1, 63, -1), 0x7fff, 0xffff, 0xffff, 0xffff), // = sint_min -1. Wraps to positive
                                                                                   // when truncated
                        a(powerOfTwo(-1, 63, -2), 0x7fff, 0xffff, 0xffff, 0xfffe), // = sint_min -2. Wraps to positive
                                                                                   // when truncated
                        // corner cases around unsigned max value
                        a(powerOfTwo(64, -2), 0xffff, 0xffff, 0xffff, 0xfffe), // =uint_max-1.
                        a(powerOfTwo(64, -1), 0xffff, 0xffff, 0xffff, 0xffff), // =uint_max.
                        a(powerOfTwo(64, 0), 0x0000, 0x0000, 0x0000, 0x0000), // = uint_max+1.
                        a(powerOfTwo(64, 1), 0x0000, 0x0000, 0x0000, 0x0001) // = uint_max+2.

                ),
                //
                // INT64_SWAP and UINT64_SWAP
                //
                parameterize(new ValueType[] { INT64_SWAP, UINT64_SWAP }, a("1.0", 1, 0, 0, 0), a("1.6", 1, 0, 0, 0),
                        a("2.6", 2, 0, 0, 0), a("-1004.4", 0xFC14, 0xFFFF, 0xFFFF, 0xFFFF),
                        // corner cases around signed max value
                        a(powerOfTwo(63, -2), 0xfffe, 0xffff, 0xffff, 0x7fff), // =sint_max-1.
                        a(powerOfTwo(63, -1), 0xffff, 0xffff, 0xffff, 0x7fff), // =sint_max. Only high bit set
                        a(powerOfTwo(63, 0), 0x0000, 0x0000, 0x0000, 0x8000), // = sint_max +1. Wraps to negative when
                                                                              // truncated
                        a(powerOfTwo(63, 1), 0x0001, 0x0000, 0x0000, 0x8000), // = sint_max +2. Wraps to negative when
                                                                              // truncated
                        // corner cases around signed min value
                        a(powerOfTwo(-1, 63, 1), 0x0001, 0x0000, 0x0000, 0x8000), // =sint_min+1
                        a(powerOfTwo(-1, 63, 0), 0x0000, 0x0000, 0x0000, 0x8000), // =sint_min. Only high bit set
                        a(powerOfTwo(-1, 63, -1), 0xffff, 0xffff, 0xffff, 0x7fff), // = sint_min -1. Wraps to positive
                                                                                   // when truncated
                        a(powerOfTwo(-1, 63, -2), 0xfffe, 0xffff, 0xffff, 0x7fff), // = sint_min -2. Wraps to positive
                                                                                   // when truncated
                        // corner cases around unsigned max value
                        a(powerOfTwo(64, -2), 0xfffe, 0xffff, 0xffff, 0xffff), // =uint_max-1.
                        a(powerOfTwo(64, -1), 0xffff, 0xffff, 0xffff, 0xffff), // =uint_max.
                        a(powerOfTwo(64, 0), 0x0000, 0x0000, 0x0000, 0x0000), // = uint_max+1.
                        a(powerOfTwo(64, 1), 0x0001, 0x0000, 0x0000, 0x0000) // = uint_max+2.

                ),
                //
                // FLOAT32
                //
                a("1.0", FLOAT32, 0x3F80, 0x0000), a("1.6", FLOAT32, 0x3FCC, 0xCCCD), a("2.6", FLOAT32, 0x4026, 0x6666),
                a("-1004.4", FLOAT32, 0xC47B, 0x199A), a("64000", FLOAT32, 0x477A, 0x0000),
                a("70004.4", FLOAT32, 0x4788, 0xBA33), a("5000000000", FLOAT32, 0x4F95, 0x02F9),
                //
                // FLOAT32_SWAP
                //
                a("1.0", FLOAT32_SWAP, 0x0000, 0x3F80), a("1.6", FLOAT32_SWAP, 0xCCCD, 0x3FCC),
                a("2.6", FLOAT32_SWAP, 0x6666, 0x4026), a("-1004.4", FLOAT32_SWAP, 0x199A, 0xC47B),
                a("64000", FLOAT32_SWAP, 0x0000, 0x477A), a("70004.4", FLOAT32_SWAP, 0xBA33, 0x4788),
                a("5000000000", FLOAT32_SWAP, 0x02F9, 0x4F95),
                // ON/OFF
                a(OnOffType.ON, FLOAT32_SWAP, 0x0000, 0x3F80), a(OnOffType.OFF, FLOAT32_SWAP, 0x0000, 0x0000),
                // OPEN
                a(OpenClosedType.OPEN, FLOAT32_SWAP, 0x0000, 0x3F80), a(OpenClosedType.OPEN, INT16, 1),
                // CLOSED
                a(OpenClosedType.CLOSED, FLOAT32_SWAP, 0x0000, 0x0000), a(OpenClosedType.CLOSED, INT16, 0x0000),
                // QuantityType, dimensionless units are converted to unit of 1. e.g. 500% -> 5
                a(QuantityType.valueOf(500, Units.PERCENT), INT16, 0x0005),
                // 50% = 0.5 truncated to zero
                a(QuantityType.valueOf(50, Units.PERCENT), INT16, 0x0000),
                a(QuantityType.valueOf(6, Units.ONE), INT16, 0x0006),
                // QuantityType, non-dimensionless not supported
                a(QuantityType.valueOf(5, Units.KELVIN), INT16, IllegalArgumentException.class),
                // Unsupported command
                a(IncreaseDecreaseType.INCREASE, FLOAT32_SWAP, IllegalArgumentException.class)

        );
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
