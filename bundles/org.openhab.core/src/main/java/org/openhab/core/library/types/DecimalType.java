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
package org.openhab.core.library.types;

import java.math.BigDecimal;
import java.util.IllegalFormatConversionException;

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

    public static final DecimalType ZERO = new DecimalType(0);

    protected BigDecimal value;

    public DecimalType() {
        this.value = BigDecimal.ZERO;
    }

    public DecimalType(BigDecimal value) {
        this.value = value;
    }

    public DecimalType(long value) {
        this.value = BigDecimal.valueOf(value);
    }

    public DecimalType(double value) {
        this.value = BigDecimal.valueOf(value);
    }

    public DecimalType(String value) {
        this.value = new BigDecimal(value);
    }

    @Override
    public String toString() {
        return toFullString();
    }

    @Override
    public String toFullString() {
        return value.toPlainString();
    }

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
            return target.cast(new PercentType(toBigDecimal().multiply(BigDecimal.valueOf(100))));
        } else if (target == UpDownType.class) {
            if (equals(ZERO)) {
                return target.cast(UpDownType.UP);
            } else if (toBigDecimal().compareTo(BigDecimal.valueOf(1)) == 0) {
                return target.cast(UpDownType.DOWN);
            } else {
                return null;
            }
        } else if (target == OpenClosedType.class) {
            if (equals(ZERO)) {
                return target.cast(OpenClosedType.CLOSED);
            } else if (toBigDecimal().compareTo(BigDecimal.valueOf(1)) == 0) {
                return target.cast(OpenClosedType.OPEN);
            } else {
                return null;
            }
        } else if (target == HSBType.class) {
            return target.cast(new HSBType(DecimalType.ZERO, PercentType.ZERO,
                    new PercentType(this.toBigDecimal().multiply(BigDecimal.valueOf(100)))));
        } else if (target == DateTimeType.class) {
            return target.cast(new DateTimeType(value.toString()));
        } else {
            return defaultConversion(target);
        }
    }
}
