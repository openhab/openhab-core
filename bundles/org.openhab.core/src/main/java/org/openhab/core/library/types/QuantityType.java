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

import static org.eclipse.jdt.annotation.DefaultLocation.*;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.IllegalFormatConversionException;
import java.util.Locale;

import javax.measure.Dimension;
import javax.measure.IncommensurableException;
import javax.measure.Quantity;
import javax.measure.Quantity.Scale;
import javax.measure.UnconvertibleException;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.format.MeasurementParseException;
import javax.measure.quantity.Dimensionless;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.internal.library.unit.UnitInitializer;
import org.openhab.core.library.unit.MetricPrefix;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.Command;
import org.openhab.core.types.PrimitiveType;
import org.openhab.core.types.State;
import org.openhab.core.types.util.UnitUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.units.indriya.AbstractUnit;
import tech.units.indriya.format.NumberDelimiterQuantityFormat;
import tech.units.indriya.format.SimpleUnitFormat;
import tech.units.indriya.quantity.Quantities;
import tech.uom.lib.common.function.QuantityFunctions;

/**
 * The measure type extends DecimalType to handle physical unit measurement
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault({ PARAMETER, RETURN_TYPE, FIELD, TYPE_ARGUMENT }) // TYPE_BOUNDS can not be used here since
                                                                    // javax.measure.quantity.* interfaces are not
                                                                    // annotated.
public class QuantityType<T extends Quantity<T>> extends Number
        implements PrimitiveType, State, Command, Comparable<QuantityType<T>> {

    private static final long serialVersionUID = 8828949721938234629L;
    private static final BigDecimal BIG_DECIMAL_HUNDRED = BigDecimal.valueOf(100);

    public static final QuantityType<Dimensionless> ZERO = new QuantityType<>(0, AbstractUnit.ONE);
    public static final QuantityType<Dimensionless> ONE = new QuantityType<>(1, AbstractUnit.ONE);

    // Regular expression to split unit from value
    // split on any blank character, even none (\\s*) which occurs after a digit (?<=\\d) and before
    // a "unit" character ?=[a-zA-Z°µ%'] which itself must not be preceded by plus/minus digit (?![\\+\\-]?\\d).
    // The later would be an exponent from the scalar value.
    private static final String UNIT_PATTERN = "(?<=\\d)\\s*(?=[a-zA-Z°µ%'](?![\\+\\-]?\\d))";

    static {
        UnitInitializer.init();
    }

    private final transient Logger logger = LoggerFactory.getLogger(QuantityType.class);

    private final Quantity<T> quantity;

    /**
     * Creates a dimensionless {@link QuantityType} with scalar 0 and unit {@link AbstractUnit#ONE}.
     * A default constructor is needed by {@link org.openhab.core.internal.items.ItemUpdater#receiveUpdate})
     */
    @SuppressWarnings("unchecked")
    public QuantityType() {
        this.quantity = (Quantity<T>) ZERO.quantity;
    }

    /**
     * Creates a new {@link QuantityType} with the given value. The value may contain a unit. The specific
     * {@link Quantity} is obtained by {@link NumberDelimiterQuantityFormat#parse(CharSequence)}.
     * The English locale is used to determine (decimal/grouping) separator characters.
     *
     * @param value the non null value representing a quantity with an optional unit.
     * @throws NumberFormatException when a quantity without a unit could not be parsed
     * @throws IllegalArgumentException when a quantity with a unit could not be parsed
     */
    public QuantityType(String value) {
        this(value, Locale.ENGLISH);
    }

    /**
     * Creates a new {@link QuantityType} with the given value. The value may contain a unit. The specific
     * {@link Quantity} is obtained by {@link NumberDelimiterQuantityFormat#parse(CharSequence)}.
     *
     * @param value the non null value representing a quantity with an optional unit.
     * @param locale the locale used to determine (decimal/grouping) separator characters.
     * @throws NumberFormatException when a quantity without a unit could not be parsed
     * @throws IllegalArgumentException when a quantity with a unit could not be parsed
     */
    @SuppressWarnings("unchecked")
    public QuantityType(String value, Locale locale) {
        String[] constituents = value.split(UNIT_PATTERN);

        // getQuantity needs a space between numeric value and unit
        String formatted = String.join(" ", constituents);
        if (!formatted.contains(" ")) {
            DecimalFormat df = (DecimalFormat) NumberFormat.getInstance(locale);
            df.setParseBigDecimal(true);
            ParsePosition position = new ParsePosition(0);
            BigDecimal parsedValue = (BigDecimal) df.parseObject(value, position);
            if (parsedValue == null || position.getErrorIndex() != -1 || position.getIndex() < value.length()) {
                throw new NumberFormatException("Invalid BigDecimal value: " + value);
            }
            quantity = (Quantity<T>) Quantities.getQuantity(parsedValue, AbstractUnit.ONE, Scale.RELATIVE);
        } else {
            SimpleUnitFormat unitFormat = SimpleUnitFormat.getInstance();
            NumberDelimiterQuantityFormat quantityFormat = new NumberDelimiterQuantityFormat.Builder()
                    .setNumberFormat(NumberFormat.getInstance(locale)).setUnitFormat(unitFormat)
                    .setLocaleSensitive(true).build();
            ParsePosition position = new ParsePosition(0);
            try {
                Quantity<T> absoluteQuantity = (Quantity<T>) quantityFormat.parse(formatted, position);
                Unit<T> unit = absoluteQuantity.getUnit();
                if (position.getErrorIndex() != -1 || (position.getIndex() < value.length())) {
                    // The position is now at the end of the parsed number. Because it does not always fully
                    // parse the whole number, an exception is thrown if the remaining string cannot be
                    // parsed to a unit that equals the parsed unit.
                    if (!unit.equals(unitFormat.parse(value.substring(position.getIndex()).trim()))) {
                        throw new IllegalArgumentException("Invalid Quantity value: " + value);
                    }
                }
                quantity = Quantities.getQuantity(absoluteQuantity.getValue(), absoluteQuantity.getUnit(),
                        Scale.RELATIVE);
            } catch (MeasurementParseException e) {
                throw new IllegalArgumentException("Invalid Quantity value: " + value, e);
            }
        }
    }

    /**
     * Creates a new {@link QuantityType} with the given value and {@link Unit}.
     *
     * @param value the non null measurement value.
     * @param unit the non null measurement unit.
     */
    public QuantityType(Number value, Unit<T> unit) {
        // Avoid scientific notation for double
        BigDecimal bd = new BigDecimal(value.toString());
        quantity = (Quantity<T>) Quantities.getQuantity(bd, unit, Scale.RELATIVE);
    }

    /**
     * Private constructor for arithmetic operations.
     *
     * @param quantity the {@link Quantity} for the new {@link QuantityType}.
     */
    private QuantityType(Quantity<T> quantity) {
        this.quantity = quantity;
    }

    /**
     * Static access to {@link QuantityType#QuantityType(double, Unit)}.
     *
     * @param value the non null measurement value.
     * @param unit the non null measurement unit.
     * @return a new {@link QuantityType}
     */
    public static <T extends Quantity<T>> QuantityType<T> valueOf(double value, Unit<T> unit) {
        return new QuantityType<T>(value, unit);
    }

    @Override
    public String toString() {
        return toFullString();
    }

    /**
     * Static access to {@link QuantityType#QuantityType(String)}.
     *
     * @param value the non null value representing a quantity with an optional unit
     * @return a new {@link QuantityType}
     * @throws NumberFormatException when a quantity without a unit could not be parsed
     * @throws IllegalArgumentException when a quantity with a unit could not be parsed
     */
    public static QuantityType<? extends Quantity<?>> valueOf(String value) {
        return new QuantityType<>(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof QuantityType)) {
            return false;
        }
        QuantityType<?> other = (QuantityType<?>) obj;
        if (!quantity.getUnit().isCompatible(other.quantity.getUnit())
                && !quantity.getUnit().inverse().isCompatible(other.quantity.getUnit())) {
            return false;
        } else if (internalCompareTo(other) != 0) {
            return false;
        }

        return true;
    }

    @Override
    public int compareTo(QuantityType<T> o) {
        return internalCompareTo((QuantityType<?>) o);
    }

    private int internalCompareTo(QuantityType<?> o) {
        if (quantity.getUnit().isCompatible(o.quantity.getUnit())) {
            QuantityType<T> v1 = this.toUnit(getUnit().getSystemUnit());
            QuantityType<?> v2 = o.toUnit(o.getUnit().getSystemUnit());
            if (v1 != null && v2 != null) {
                return Double.compare(v1.doubleValue(), v2.doubleValue());
            } else {
                throw new IllegalArgumentException("Unable to convert to system unit during compare.");
            }
        } else if (quantity.getUnit().inverse().isCompatible(o.quantity.getUnit())) {
            return inverse().internalCompareTo(o);
        } else {
            throw new IllegalArgumentException("Can not compare incompatible units.");
        }
    }

    public Unit<T> getUnit() {
        return quantity.getUnit();
    }

    public Dimension getDimension() {
        return getUnit().getDimension();
    }

    /**
     * Convert this QuantityType to a new {@link QuantityType} using the given target unit.
     *
     * @param targetUnit the unit to which this {@link QuantityType} will be converted to.
     * @return the new {@link QuantityType} in the given {@link Unit} or {@code null} in case of an error.
     */
    @SuppressWarnings("unchecked")
    public @Nullable QuantityType<T> toUnit(Unit<?> targetUnit) {
        if (!targetUnit.equals(getUnit())) {
            try {
                UnitConverter uc = getUnit().getConverterToAny(targetUnit);
                Quantity<?> result = Quantities.getQuantity(uc.convert(quantity.getValue()), targetUnit);

                return new QuantityType<T>(result.getValue(), (Unit<T>) targetUnit);
            } catch (UnconvertibleException | IncommensurableException e) {
                logger.debug("Unable to convert unit from {} to {}", getUnit(), targetUnit);
                return null;
            }
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public @Nullable QuantityType<T> toUnit(String targetUnit) {
        Unit<T> unit = (Unit<T>) AbstractUnit.parse(targetUnit);
        if (unit != null) {
            return toUnit(unit);
        }

        return null;
    }

    /**
     * Convert this QuantityType to a new {@link QuantityType} using the given target unit.
     *
     * Implicit conversions using inverse units are allowed (i.e. mired <=> Kelvin). This may
     * change the dimension.
     *
     * @param targetUnit the unit to which this {@link QuantityType} will be converted to.
     * @return the new {@link QuantityType} in the given {@link Unit} or {@code null} in case of an erro.
     */
    public @Nullable QuantityType<?> toInvertibleUnit(Unit<?> targetUnit) {
        if (!targetUnit.equals(getUnit()) && getUnit().inverse().isCompatible(targetUnit)) {
            return inverse().toUnit(targetUnit);
        }
        return toUnit(targetUnit);
    }

    @SuppressWarnings("unchecked")
    public @Nullable QuantityType<?> toInvertibleUnit(String targetUnit) {
        Unit<?> unit = AbstractUnit.parse(targetUnit);
        if (unit != null) {
            return toInvertibleUnit(unit);
        }

        return null;
    }

    /**
     * Convert this QuantityType to a new {@link QuantityType} using the given target unit.
     *
     * Similar to {@link toUnit}, except that it treats the values as relative instead of absolute.
     * This means that any offsets in the conversion of absolute values are ignored.
     * This is useful when your quantity represents a delta, and not necessarily a measured
     * value itself. For example, 32 °F, when converted with toUnit to Celsius, it will become 0 °C.
     * But when converted with toUnitRelative, it will become 17.8 °C.
     *
     * @param targetUnit the unit to which this {@link QuantityType} will be converted to.
     * @return the new {@link QuantityType} in the given {@link Unit} or {@code null} in case of an error.
     */
    @SuppressWarnings("unchecked")
    public @Nullable QuantityType<T> toUnitRelative(Unit<T> targetUnit) {
        if (targetUnit.equals(getUnit())) {
            return this;
        }
        if (!quantity.getUnit().isCompatible(targetUnit)) {
            return null;
        }
        Quantity<?> result = quantity.to(targetUnit);

        return new QuantityType<T>(result.getValue(), targetUnit);
    }

    public BigDecimal toBigDecimal() {
        return new BigDecimal(quantity.getValue().toString());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int tmp = prime * getUnit().hashCode();
        tmp += prime * (quantity.getValue() == null ? 0 : quantity.getValue().hashCode());
        return tmp;
    }

    @Override
    public String format(String pattern) {
        boolean unitPlaceholder = pattern.contains(UnitUtils.UNIT_PLACEHOLDER);
        final String formatPattern;

        if (unitPlaceholder) {
            String unitSymbol = getUnit().equals(Units.PERCENT) ? "%%" : getUnit().toString();
            formatPattern = pattern.replace(UnitUtils.UNIT_PLACEHOLDER, unitSymbol);
        } else {
            formatPattern = pattern;
        }

        // The dimension could be a time value thus we want to support patterns to format datetime
        if (quantity.getUnit().isCompatible(Units.SECOND) && !unitPlaceholder) {
            QuantityType<T> millis = toUnit(MetricPrefix.MILLI(Units.SECOND));
            if (millis != null) {
                try {
                    return String.format(formatPattern,
                            ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis.longValue()), ZoneOffset.UTC));
                } catch (IllegalFormatConversionException ifce) {
                    // The conversion is not valid for the type ZonedDateTime. This happens, if the format is like
                    // "%.1f". Fall through to default behavior.
                }
            }
        }

        // The value could be an integer value. Try to convert to BigInteger in
        // order to have access to more conversion formats.
        BigDecimal bd = toBigDecimal();
        try {
            return String.format(formatPattern, bd.toBigIntegerExact());
        } catch (ArithmeticException ae) {
            // Could not convert to integer value without loss of
            // information. Fall through to default behavior.
        } catch (IllegalFormatConversionException ifce) {
            // The conversion is not valid for the type BigInteger. This
            // happens, if the format is like "%.1f" but the value is an
            // integer. Fall through to default behavior.
        }

        return String.format(formatPattern, bd);
    }

    @Override
    public int intValue() {
        return quantity.getValue().intValue();
    }

    @Override
    public long longValue() {
        return quantity.getValue().longValue();
    }

    @Override
    public float floatValue() {
        return quantity.getValue().floatValue();
    }

    @Override
    public double doubleValue() {
        return quantity.getValue().doubleValue();
    }

    @Override
    public String toFullString() {
        if (AbstractUnit.ONE.equals(quantity.getUnit())) {
            return quantity.getValue().toString();
        } else {
            return quantity.toString();
        }
    }

    @Override
    public <U extends State> @Nullable U as(@Nullable Class<U> target) {
        if (target == OnOffType.class) {
            if (intValue() == 0) {
                return target.cast(OnOffType.OFF);
            } else if (Units.PERCENT.equals(getUnit())) {
                return target.cast(toBigDecimal().compareTo(BigDecimal.ZERO) > 0 ? OnOffType.ON : OnOffType.OFF);
            } else if (toBigDecimal().compareTo(BigDecimal.ONE) == 0) {
                return target.cast(OnOffType.ON);
            } else {
                return null;
            }
        } else if (target == UpDownType.class) {
            if (doubleValue() == 0) {
                return target.cast(UpDownType.UP);
            } else if (toBigDecimal().compareTo(BigDecimal.ONE) == 0) {
                return target.cast(UpDownType.DOWN);
            } else {
                return null;
            }
        } else if (target == OpenClosedType.class) {
            if (doubleValue() == 0) {
                return target.cast(OpenClosedType.CLOSED);
            } else if (toBigDecimal().compareTo(BigDecimal.ONE) == 0) {
                return target.cast(OpenClosedType.OPEN);
            } else {
                return null;
            }
        } else if (target == HSBType.class) {
            return target.cast(new HSBType(DecimalType.ZERO, PercentType.ZERO,
                    new PercentType(toBigDecimal().multiply(BIG_DECIMAL_HUNDRED))));
        } else if (target == PercentType.class) {
            QuantityType<T> inPercent = toUnit(Units.PERCENT);
            if (inPercent == null) {
                // incompatible unit
                return null;
            } else {
                return target.cast(new PercentType(inPercent.toBigDecimal()));
            }
        } else if (target == DecimalType.class) {
            return target.cast(new DecimalType(toBigDecimal()));
        } else {
            return State.super.as(target);
        }
    }

    /**
     * Returns the sum of the given {@link QuantityType} with this QuantityType.
     *
     * @param state the {@link QuantityType} to add to this QuantityType.
     * @return the sum of the given {@link QuantityType} with this QuantityType.
     */
    public QuantityType<T> add(QuantityType<T> state) {
        return new QuantityType<T>(this.quantity.add(state.quantity));
    }

    /**
     * Negates the value of this QuantityType leaving its unit untouched.
     *
     * @return the negated value of this QuantityType.
     */
    public QuantityType<T> negate() {
        return new QuantityType<>(quantity.negate());
    }

    /**
     * Subtract the given {@link QuantityType} from this QuantityType.
     *
     * @param state the {@link QuantityType} to subtract from this QuantityType.
     * @return the difference by subtracting the given {@link QuantityType} from this QuantityType.
     */
    public QuantityType<T> subtract(QuantityType<T> state) {
        return new QuantityType<T>(this.quantity.subtract(state.quantity));
    }

    /**
     * Multiply this {@link QuantityType} by the given value.
     *
     * @param value the value this {@link QuantityType} should be multiplied with.
     * @return the product of the given value with this {@link QuantityType}.
     */
    public QuantityType<?> multiply(BigDecimal value) {
        return new QuantityType<>(this.quantity.multiply(value));
    }

    /**
     * Multiply this QuantityType by the given {@link QuantityType}.
     *
     * @param state the {@link QuantityType} with which this QuantityType should be multiplied with.
     * @return the product of the given {@link QuantityType} and this QuantityType.
     */
    public QuantityType<?> multiply(QuantityType<?> state) {
        return new QuantityType<>(this.quantity.multiply(state.quantity));
    }

    /**
     * Divide this QuantityType by the given value.
     *
     * @param value the value this {@link QuantityType} should be divided by.
     * @return the quotient from this QuantityType and the given value.
     */
    public QuantityType<?> divide(BigDecimal value) {
        return new QuantityType<>(this.quantity.divide(value));
    }

    /**
     * Divide this QuantityType by the given {@link QuantityType}.
     *
     * @param state the {@link QuantityType} this QuantityType should be divided by.
     * @return the quotient from this QuantityType and the given {@link QuantityType}.
     */
    public QuantityType<?> divide(QuantityType<?> state) {
        return new QuantityType<>(this.quantity.divide(state.quantity));
    }

    /**
     * Apply a given offset to this QuantityType
     *
     * @param offset the offset to apply
     * @return changed QuantityType by offset
     */
    public QuantityType<T> offset(QuantityType<T> offset, Unit<T> unit) {
        final Quantity<T> sum = Arrays.asList(quantity, offset.quantity).stream().reduce(QuantityFunctions.sum(unit))
                .get();
        return new QuantityType<T>(sum);
    }

    /**
     * Return the reciprocal of this QuantityType.
     * 
     * @return a QuantityType with both the value and unit reciprocated
     */
    public QuantityType<?> inverse() {
        return new QuantityType<>(this.quantity.inverse());
    }
}
