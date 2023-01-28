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
package org.openhab.core.library.items;

import java.util.List;
import java.util.Locale;

import javax.measure.Dimension;
import javax.measure.Quantity;
import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.Metadata;
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

    public static final String UNIT_METADATA_NAMESPACE = "unit";
    private static final List<Class<? extends State>> ACCEPTED_DATA_TYPES = List.of(DecimalType.class,
            QuantityType.class, UnDefType.class);
    private static final List<Class<? extends Command>> ACCEPTED_COMMAND_TYPES = List.of(DecimalType.class,
            QuantityType.class, RefreshType.class);

    @Nullable
    private Unit<?> unit;

    public NumberItem(String name) {
        super(CoreItemFactory.NUMBER, name);
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
        Unit<?> itemUnit = this.unit;
        if (itemUnit == null) {
            internalSend(command);
        } else {
            QuantityType<?> enrichedCommand = new QuantityType<>(command.toBigDecimal(), itemUnit);
            internalSend(enrichedCommand);
        }
    }

    public void send(QuantityType<?> command) {
        if (this.unit == null) {
            DecimalType strippedCommand = new DecimalType(command.toBigDecimal());
            internalSend(strippedCommand);
        } else {
            internalSend(command);
        }
    }

    @Override
    public @Nullable StateDescription getStateDescription(@Nullable Locale locale) {
        StateDescription stateDescription = super.getStateDescription(locale);
        if (unit == null && stateDescription != null) {
            // remove item placeholder when unit has no item
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
        return UnitUtils.parseDimension(UnitUtils.getDimensionName(unit));
    }

    @Override
    public void setState(State state) {
        Unit<?> itemUnit = unit;
        if (state instanceof QuantityType<?> quantityType) {
            if (itemUnit == null) {
                // QuantityType update to a NumberItem without unit, strip unit
                DecimalType plainState = new DecimalType(quantityType.toBigDecimal());
                super.setState(plainState);
            } else {
                // QuantityType update to a NumberItem with unit, convert to item unit (if possible)
                Unit<?> stateUnit = quantityType.getUnit();
                State convertedState = (stateUnit.isCompatible(itemUnit) || stateUnit.inverse().isCompatible(itemUnit))
                        ? quantityType.toInvertibleUnit(itemUnit)
                        : null;
                if (convertedState != null) {
                    super.setState(convertedState);
                } else {
                    // TODO: better logging needed?
                    logSetTypeError(state);
                }
            }
        } else if (state instanceof DecimalType decimalType) {
            if (itemUnit == null) {
                // DecimalType update to NumberItem with unit
                super.setState(decimalType);
            } else {
                // DecimalType update for a NumberItem with dimension, convert to QuantityType
                super.setState(new QuantityType<>(decimalType.doubleValue(), itemUnit));
            }
        } else if (state instanceof UnDefType) {
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
        Unit<?> itemUnit = unit;
        return itemUnit != null ? itemUnit.toString() : null;
    }

    /**
     * Get the {@link Unit} for this {@link NumberItem}.
     *
     * @return the {@link Unit} for this item if available, {@code null} otherwise.
     */
    public @Nullable Unit<? extends Quantity<?>> getUnit() {
        return unit;
    }

    @Override
    public void addedMetadata(Metadata metadata) {
        if (UNIT_METADATA_NAMESPACE.equals(metadata.getUID().getNamespace())) {
            unit = UnitUtils.parseUnit(metadata.getValue());
            if (unit == null) {
                // TODO: log error or throw exception?
            }
        }
    }

    @Override
    public void updatedMetadata(Metadata oldMetadata, Metadata newMetadata) {
        addedMetadata(newMetadata);
    }

    @Override
    public void removedMetadata(Metadata metadata) {
        if (UNIT_METADATA_NAMESPACE.equals(metadata.getUID().getNamespace())) {
            unit = null;
        }
    }
}
