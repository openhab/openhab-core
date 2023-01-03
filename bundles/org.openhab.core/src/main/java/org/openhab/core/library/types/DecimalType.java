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
package org.openhab.core.library.types;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.IllegalFormatConversionException;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.Command;
import org.openhab.core.types.PrimitiveType;
import org.openhab.core.types.State;

/**
 * The decimal type uses a BigDecimal internally and thus can be used for
 * integers, longs and floating point numbers alike.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class DecimalType extends Number implements PrimitiveType, State, Command, Comparable<DecimalType> {

    private static final long serialVersionUID = 4226845847123464690L;
    protected static final BigDecimal BIG_DECIMAL_HUNDRED = BigDecimal.valueOf(100);

    public static final DecimalType ZERO = new DecimalType(0);

    protected BigDecimal value;

    public DecimalType() {
        this(BigDecimal.ZERO);
    }

    /**
     * Creates a new {@link DecimalType} with the given value.
     *
     * @param value a number
     */
    public DecimalType(Number value) {
        if (value instanceof QuantityType) {
            this.value = ((QuantityType<?>) value).toBigDecimal();
        } else if (value instanceof HSBType) {
            this.value = ((HSBType) value).toBigDecimal();
        } else {
            this.value = new BigDecimal(value.toString());
        }
    }

    /**
     * Creates a new {@link DecimalType} with the given value.
     * The English locale is used to determine (decimal/grouping) separator characters.
     *
     * @param value the value representing a number
     * @throws NumberFormatException when the number could not be parsed to a {@link BigDecimal}
     */
    public DecimalType(String value) {
        this(value, Locale.ENGLISH);
    }

    /**
     * Creates a new {@link DecimalType} with the given value.
     *
     * @param value the value representing a number
     * @param locale the locale used to determine (decimal/grouping) separator characters
     * @throws NumberFormatException when the number could not be parsed to a {@link BigDecimal}
     */
    public DecimalType(String value, Locale locale) {
        DecimalFormat df = (DecimalFormat) NumberFormat.getInstance(locale);
        df.setParseBigDecimal(true);
        ParsePosition position = new ParsePosition(0);
        BigDecimal parsedValue = (BigDecimal) df.parseObject(value, position);
        if (parsedValue == null || position.getErrorIndex() != -1 || position.getIndex() < value.length()) {
            throw new NumberFormatException("Invalid BigDecimal value: " + value);
        }
        this.value = parsedValue;
    }

    @Override
    public String toString() {
        return toFullString();
    }

    @Override
    public String toFullString() {
        return value.toPlainString();
    }

    /**
     * Static access to {@link DecimalType#DecimalType(String)}.
     *
     * @param value the non null value representing a number
     * @return a new {@link DecimalType}
     * @throws NumberFormatException when the number could not be parsed to a {@link BigDecimal}
     */
    public static DecimalType valueOf(String value) {
        return new DecimalType(value);
    }

    @Override
    public String format(String pattern) {
        // The value could be an integer value. Try to convert to BigInteger in
        // order to have access to more conversion formats.
        try {
            return String.format(pattern, value.toBigIntegerExact());
        } catch (ArithmeticException ae) {
            // Could not convert to integer value without loss of
            // information. Fall through to default behavior.
        } catch (IllegalFormatConversionException ifce) {
            // The conversion is not valid for the type BigInteger. This
            // happens, if the format is like "%.1f" but the value is an
            // integer. Fall through to default behavior.
        }

        return String.format(pattern, value);
    }

    public BigDecimal toBigDecimal() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + value.hashCode();
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DecimalType)) {
            return false;
        }
        DecimalType other = (DecimalType) obj;
        return value.compareTo(other.value) == 0;
    }

    @Override
    public int compareTo(DecimalType o) {
        return value.compareTo(o.toBigDecimal());
    }

    @Override
    public double doubleValue() {
        return value.doubleValue();
    }

    @Override
    public float floatValue() {
        return value.floatValue();
    }

    @Override
    public int intValue() {
        return value.intValue();
    }

    @Override
    public long longValue() {
        return value.longValue();
    }

    protected <T extends State> @Nullable T defaultConversion(@Nullable Class<T> target) {
        return State.super.as(target);
    }

    @Override
    public <T extends State> @Nullable T as(@Nullable Class<T> target) {
        if (target == OnOffType.class) {
            return target.cast(equals(ZERO) ? OnOffType.OFF : OnOffType.ON);
        } else if (target == PercentType.class) {
            return target.cast(new PercentType(toBigDecimal().multiply(BIG_DECIMAL_HUNDRED)));
        } else if (target == UpDownType.class) {
            if (equals(ZERO)) {
                return target.cast(UpDownType.UP);
            } else if (toBigDecimal().compareTo(BigDecimal.ONE) == 0) {
                return target.cast(UpDownType.DOWN);
            } else {
                return null;
            }
        } else if (target == OpenClosedType.class) {
            if (equals(ZERO)) {
                return target.cast(OpenClosedType.CLOSED);
            } else if (toBigDecimal().compareTo(BigDecimal.ONE) == 0) {
                return target.cast(OpenClosedType.OPEN);
            } else {
                return null;
            }
        } else if (target == HSBType.class) {
            return target.cast(new HSBType(DecimalType.ZERO, PercentType.ZERO,
                    new PercentType(this.toBigDecimal().multiply(BIG_DECIMAL_HUNDRED))));
        } else if (target == DateTimeType.class) {
            return target.cast(new DateTimeType(value.toString()));
        } else {
            return defaultConversion(target);
        }
    }
}
