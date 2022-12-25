/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.library.items;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

import javax.measure.Dimension;
import javax.measure.Quantity;
import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.ItemUtil;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.UnDefType;
import org.openhab.core.types.util.UnitUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A NumberItem has a decimal value and is usually used for all kinds
 * of sensors, like temperature, brightness, wind, etc.
 * It can also be used as a counter or as any other thing that can be expressed
 * as a number.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class NumberItem extends GenericItem {

    public static final String TYPE = "Number";
    private static final List<Class<? extends State>> ACCEPTED_DATA_TYPES = List.of(DecimalType.class,
            QuantityType.class, UnDefType.class);
    private static final List<Class<? extends Command>> ACCEPTED_COMMAND_TYPES = List.of(DecimalType.class,
            QuantityType.class, RefreshType.class);

    private final Logger logger = LoggerFactory.getLogger(NumberItem.class);

    @Nullable
    private final Class<? extends Quantity<?>> dimension;

    private final @Nullable UnitProvider unitProvider;
    private Supplier<@Nullable Unit<? extends Quantity<?>>> itemDefaultUnitProvider = () -> null;

    public NumberItem(String name) {
        this(CoreItemFactory.NUMBER, name, null);
    }

    public NumberItem(String type, String name, @Nullable UnitProvider unitProvider) {
        super(type, name);
        this.unitProvider = unitProvider;

        String itemTypeExtension = ItemUtil.getItemTypeExtension(getType());
        if (itemTypeExtension != null) {
            dimension = UnitUtils.parseDimension(itemTypeExtension);
            if (dimension == null) {
                logger.warn("Dimension '{}' defined for item '{}' is not known. Creating a plain Number item instead.",
                        itemTypeExtension, name);
            } else if (unitProvider == null) {
                throw new IllegalArgumentException("A unit provider is required for items with a dimension.");
            }
        } else {
            dimension = null;
        }
    }

    @Override
    public List<Class<? extends State>> getAcceptedDataTypes() {
        return ACCEPTED_DATA_TYPES;
    }

    @Override
    public List<Class<? extends Command>> getAcceptedCommandTypes() {
        return ACCEPTED_COMMAND_TYPES;
    }

    public void send(DecimalType command) {
        internalSend(command);
    }

    public void send(QuantityType<?> command) {
        if (dimension == null) {
            DecimalType strippedCommand = new DecimalType(command.toBigDecimal());
            internalSend(strippedCommand);
        } else {
            internalSend(command);
        }
    }

    @Override
    public @Nullable StateDescription getStateDescription(@Nullable Locale locale) {
        StateDescription stateDescription = super.getStateDescription(locale);
        if (getDimension() == null && stateDescription != null) {
            String pattern = stateDescription.getPattern();
            if (pattern != null && pattern.contains(UnitUtils.UNIT_PLACEHOLDER)) {
                return StateDescriptionFragmentBuilder.create(stateDescription)
                        .withPattern(pattern.replaceAll(UnitUtils.UNIT_PLACEHOLDER, "").trim()).build()
                        .toStateDescription();
            }
        }
        return stateDescription;
    }

    /**
     * Returns the {@link Dimension} associated with this {@link NumberItem}, may be null.
     *
     * @return the {@link Dimension} associated with this {@link NumberItem}, may be null.
     */
    public @Nullable Class<? extends Quantity<?>> getDimension() {
        return dimension;
    }

    @Override
    public void setState(State state) {
        // QuantityType update to a NumberItem without, strip unit
        if (dimension == null && state instanceof QuantityType<?> quantityType) {
            super.setState(new DecimalType(quantityType.toBigDecimal()));
            return;
        }

        // DecimalType update for a NumberItem with dimension, convert to QuantityType:
        if (dimension != null && state instanceof DecimalType decimalType) {
            Unit<?> unit = getUnit(Objects.requireNonNull(dimension));
            super.setState(new QuantityType<>(decimalType.toBigDecimal(), unit));
        }

        // QuantityType update, check unit and convert if necessary:
        if (dimension != null && state instanceof QuantityType<?> quantityState) {
            Unit<?> itemUnit = getUnit(Objects.requireNonNull(dimension));
            Unit<?> stateUnit = quantityState.getUnit();

            if (!itemUnit.isCompatible(stateUnit) && !itemUnit.inverse().isCompatible(stateUnit)) {
                logger.warn(
                        "Tried to set state '{}' on item '{}' but the unit is not compatible with dimension '{}', ignoring it",
                        state, getName(), dimension);
                return;
            }

            if (!stateUnit.getSystemUnit().equals(itemUnit.getSystemUnit())
                    || UnitUtils.isDifferentMeasurementSystem(itemUnit, stateUnit)) {
                QuantityType<?> convertedState = quantityState.toInvertibleUnit(itemUnit);
                if (convertedState != null) {
                    super.setState(convertedState);
                    return;
                }

                // the state could not be converted to an accepted unit.
                logger.warn("Tried to set state '{}' on item '{}' but it can't be converted to unit '{}', ignoring it",
                        state, getName(), itemUnit);
                return;
            }
        }

        if (isAcceptedState(ACCEPTED_DATA_TYPES, state)) {
            super.setState(state);
        } else {
            logSetTypeError(state);
        }
    }

    /**
     * Returns the optional unit symbol for this {@link NumberItem}.
     *
     * @return the optional unit symbol for this {@link NumberItem}.
     */
    public @Nullable String getUnitSymbol() {
        return (dimension != null) ? getUnit(Objects.requireNonNull(dimension)).toString() : null;
    }

    /**
     * Set a provider for retrieving the default unit from metadata
     *
     * @param defaultUnitProvider a {@link Supplier} that supplies the default unit from metadata (or <code>null</code>
     *            if not set)
     */
    public void setItemDefaultUnitProvider(Supplier<@Nullable Unit<? extends Quantity<?>>> defaultUnitProvider) {
        this.itemDefaultUnitProvider = defaultUnitProvider;
    }

    /**
     * Derive the unit for this item by the following priority:
     * <ul>
     * <li>the unit retrieved from the <code>defaultUnit</code> in the item's metadata</li>
     * <li>the default system unit from the item's dimension</li>
     * </ul>
     *
     * @return the {@link Unit} for this item if available, {@code null} otherwise.
     */
    public @Nullable Unit<? extends Quantity<?>> getUnit() {
        return (dimension != null) ? getUnit(Objects.requireNonNull(dimension)) : null;
    }

    /**
     * Try to convert a {@link DecimalType} into a new {@link QuantityType}. The unit is derived from the metadata
     * <code>defaultUnit</code> or from the system default unit of the given dimension.
     *
     * @param originalType the source {@link DecimalType}.
     * @param dimension the dimension to which the new {@link QuantityType} should adhere.
     * @return the new {@link QuantityType} from the given originalType
     */
    public @Nullable QuantityType<?> toQuantityType(DecimalType originalType, Class<? extends Quantity<?>> dimension) {
        Unit<? extends Quantity<?>> itemUnit = getUnit(dimension);
        return new QuantityType<>(originalType.toBigDecimal(), itemUnit);
    }

    /**
     * Derive the unit for this item by the following priority:
     *
     * <ul>
     * <li>the unit retrieved from the <code>defaultUnit</code> in the item's metadata</li>
     * <li>the default system unit from the (optional) dimension parameter</li>
     * </ul>
     *
     * @param dimension the dimension
     * @return the {@link Unit} for this item if available
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Unit<? extends Quantity<?>> getUnit(Class<? extends Quantity<?>> dimension) {
        assert unitProvider != null;

        Unit<?> systemDefaultUnit = unitProvider.getUnit((Class<? extends Quantity>) dimension);
        if (systemDefaultUnit == null) {
            throw new IllegalStateException(
                    "BUG! Could not determine system default unit for dimension " + dimension.getName());
        }

        Unit<?> itemDefaultUnit = itemDefaultUnitProvider.get();
        if (itemDefaultUnit != null) {
            if (itemDefaultUnit.isCompatible(systemDefaultUnit)
                    || itemDefaultUnit.inverse().isCompatible(systemDefaultUnit)) {
                return itemDefaultUnit;
            } else {
                logger.warn(
                        "Metadata 'defaultUnit' for item '{}' is set to '{}' which is incompatible with the dimension '{}'. Using system default '{}' instead.",
                        getName(), itemDefaultUnit, ItemUtil.getItemTypeExtension(type), systemDefaultUnit);
            }
        }

        return systemDefaultUnit;
    }
}
