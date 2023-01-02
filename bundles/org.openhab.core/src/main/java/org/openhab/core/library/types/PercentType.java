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
import java.math.RoundingMode;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.State;

/**
 * The PercentType extends the {@link DecimalType} by putting constraints for its value on top (0-100).
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class PercentType extends DecimalType {

    private static final long serialVersionUID = -9066279845951780879L;

    public static final PercentType ZERO = new PercentType(0);
    public static final PercentType HUNDRED = new PercentType(100);

    /**
     * Creates a new {@link PercentType} with 0 as value.
     */
    public PercentType() {
        this(0);
    }

    /**
     * Creates a new {@link PercentType} with the given value.
     *
     * @param value the value representing a percentage
     * @throws IllegalArgumentException when the value is not between 0 and 100
     */
    public PercentType(int value) {
        super(value);
        validateValue(this.value);
    }

    /**
     * Creates a new {@link PercentType} with the given value.
     * The English locale is used to determine (decimal/grouping) separator characters.
     *
     * @param value the non null value representing a percentage
     * @throws NumberFormatException when the number could not be parsed to a {@link BigDecimal}
     * @throws IllegalArgumentException when the value is not between 0 and 100
     */
    public PercentType(String value) {
        this(value, Locale.ENGLISH);
    }

    /**
     * Creates a new {@link PercentType} with the given value.
     *
     * @param value the non null value representing a percentage
     * @param locale the locale used to determine (decimal/grouping) separator characters
     * @throws NumberFormatException when the number could not be parsed to a {@link BigDecimal}
     * @throws IllegalArgumentException when the value is not between 0 and 100
     */
    public PercentType(String value, Locale locale) {
        super(value, locale);
        validateValue(this.value);
    }

    /**
     * Creates a new {@link PercentType} with the given value.
     *
     * @param value the value representing a percentage.
     * @throws IllegalArgumentException when the value is not between 0 and 100
     */
    public PercentType(BigDecimal value) {
        super(value);
        validateValue(this.value);
    }

    private void validateValue(BigDecimal value) {
        if (BigDecimal.ZERO.compareTo(value) > 0 || BIG_DECIMAL_HUNDRED.compareTo(value) < 0) {
            throw new IllegalArgumentException("Value must be between 0 and 100");
        }
    }

    /**
     * Static access to {@link PercentType#PercentType(String)}.
     *
     * @param value the non null value representing a percentage
     * @return new {@link PercentType}
     * @throws NumberFormatException when the number could not be parsed to a {@link BigDecimal}
     * @throws IllegalArgumentException when the value is not between 0 and 100
     */
    public static PercentType valueOf(String value) {
        return new PercentType(value);
    }

    @Override
    public <T extends State> @Nullable T as(@Nullable Class<T> target) {
        if (target == OnOffType.class) {
            return target.cast(equals(ZERO) ? OnOffType.OFF : OnOffType.ON);
        } else if (target == DecimalType.class) {
            return target.cast(new DecimalType(toBigDecimal().divide(BIG_DECIMAL_HUNDRED, 8, RoundingMode.UP)));
        } else if (target == UpDownType.class) {
            if (equals(ZERO)) {
                return target.cast(UpDownType.UP);
            } else if (equals(HUNDRED)) {
                return target.cast(UpDownType.DOWN);
            } else {
                return null;
            }
        } else if (target == OpenClosedType.class) {
            if (equals(ZERO)) {
                return target.cast(OpenClosedType.CLOSED);
            } else if (equals(HUNDRED)) {
                return target.cast(OpenClosedType.OPEN);
            } else {
                return null;
            }
        } else if (target == HSBType.class) {
            return target.cast(new HSBType(DecimalType.ZERO, PercentType.ZERO, this));
        } else if (target == QuantityType.class) {
            return target.cast(new QuantityType<>(toBigDecimal().doubleValue(), Units.PERCENT));
        } else {
            return defaultConversion(target);
        }
    }
}
