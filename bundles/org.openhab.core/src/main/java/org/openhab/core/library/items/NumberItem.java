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
import java.util.Objects;

import javax.measure.Dimension;
import javax.measure.Quantity;
import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.ItemUtil;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataAwareItem;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.TimeSeries;
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
public class NumberItem extends GenericItem implements MetadataAwareItem {
    public static final String UNIT_METADATA_NAMESPACE = "unit";
    private static final List<Class<? extends State>> ACCEPTED_DATA_TYPES = List.of(DecimalType.class,
            QuantityType.class, UnDefType.class);
    private static final List<Class<? extends Command>> ACCEPTED_COMMAND_TYPES = List.of(DecimalType.class,
            QuantityType.class, RefreshType.class);

    private final Logger logger = LoggerFactory.getLogger(NumberItem.class);

    private final @Nullable Class<? extends Quantity<?>> dimension;
    private Unit<?> unit = Units.ONE;
    private final @Nullable UnitProvider unitProvider;

    public NumberItem(String name) {
        this(CoreItemFactory.NUMBER, name, null);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public NumberItem(String type, String name, @Nullable UnitProvider unitProvider) {
        super(type, name);
        this.unitProvider = unitProvider;

        String itemTypeExtension = ItemUtil.getItemTypeExtension(getType());
        if (itemTypeExtension != null) {
            Class<? extends Quantity<?>> dimension = UnitUtils.parseDimension(itemTypeExtension);
            this.dimension = dimension;
            if (dimension == null) {
                throw new IllegalArgumentException("The given dimension " + itemTypeExtension + " is unknown.");
            } else if (unitProvider == null) {
                throw new IllegalArgumentException("A unit provider is required for items with a dimension.");
            }
            this.unit = unitProvider.getUnit((Class<? extends Quantity>) dimension);
            logger.trace("Item '{}' now has unit '{}'", name, unit);
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
        } else if (command.getUnit().isCompatible(unit) || command.getUnit().inverse().isCompatible(unit)) {
            internalSend(command);
        } else {
            logger.warn("Command '{}' to item '{}' was rejected because it is incompatible with the item unit '{}'",
                    command, name, unit);
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

    private @Nullable State getInternalState(State state) {
        if (state instanceof QuantityType<?> quantityType) {
            if (dimension == null) {
                // QuantityType update to a NumberItem without unit, strip unit
                DecimalType plainState = new DecimalType(quantityType.toBigDecimal());
                return plainState;
            } else {
                // QuantityType update to a NumberItem with unit, convert to item unit (if possible)
                Unit<?> stateUnit = quantityType.getUnit();
                State convertedState = (stateUnit.isCompatible(unit) || stateUnit.inverse().isCompatible(unit))
                        ? quantityType.toInvertibleUnit(unit)
                        : null;
                if (convertedState != null) {
                    return convertedState;
                } else {
                    logger.warn("Failed to update item '{}' because '{}' could not be converted to the item unit '{}'",
                            name, state, unit);
                }
            }
        } else if (state instanceof DecimalType decimalType) {
            if (dimension == null) {
                // DecimalType update to NumberItem with unit
                return decimalType;
            } else {
                // DecimalType update for a NumberItem with dimension, convert to QuantityType
                return new QuantityType<>(decimalType.doubleValue(), unit);
            }
        }
        return null;
    }

    @Override
    public void setState(State state) {
        if (state instanceof DecimalType || state instanceof QuantityType<?>) {
            State internalState = getInternalState(state);
            if (internalState != null) {
                applyState(internalState);
            }
        } else if (state instanceof UnDefType) {
            applyState(state);
        } else {
            logSetTypeError(state);
        }
    }

    @Override
    public void setTimeSeries(TimeSeries timeSeries) {
        TimeSeries internalSeries = new TimeSeries(timeSeries.getPolicy());
        timeSeries.getStates().forEach(s -> internalSeries.add(s.timestamp(),
                Objects.requireNonNullElse(getInternalState(s.state()), UnDefType.NULL)));

        if (dimension != null && internalSeries.getStates().allMatch(s -> s.state() instanceof QuantityType<?>)) {
            applyTimeSeries(internalSeries);
        } else if (internalSeries.getStates().allMatch(s -> s.state() instanceof DecimalType)) {
            applyTimeSeries(internalSeries);
        } else {
            logSetTypeError(timeSeries);
        }
    }

    /**
     * Returns the optional unit symbol for this {@link NumberItem}.
     *
     * @return the optional unit symbol for this {@link NumberItem}.
     */
    public @Nullable String getUnitSymbol() {
        return (dimension != null) ? unit.toString() : null;
    }

    /**
     * Get the unit for this item, either:
     *
     * <ul>
     * <li>the unit retrieved from the <code>unit</code> namespace in the item's metadata</li>
     * <li>the default system unit for the item's dimension</li>
     * </ul>
     *
     * @return the {@link Unit} for this item if available, {@code null} otherwise.
     */
    public @Nullable Unit<? extends Quantity<?>> getUnit() {
        return (dimension != null) ? unit : null;
    }

    @Override
    public void addedMetadata(Metadata metadata) {
        if (dimension != null && UNIT_METADATA_NAMESPACE.equals(metadata.getUID().getNamespace())) {
            Unit<?> unit = UnitUtils.parseUnit(metadata.getValue());
            if ((unit == null) || (!unit.isCompatible(this.unit) && !unit.inverse().isCompatible(this.unit))) {
                logger.warn("Unit '{}' could not be parsed to a known unit. Keeping old unit '{}' for item '{}'.",
                        metadata.getValue(), this.unit, name);
                return;
            }
            this.unit = unit;
            logger.trace("Item '{}' now has unit '{}'", name, unit);
        }
    }

    @Override
    public void updatedMetadata(Metadata oldMetadata, Metadata newMetadata) {
        addedMetadata(newMetadata);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void removedMetadata(Metadata metadata) {
        Class<? extends Quantity<?>> dimension = this.dimension;
        if (dimension != null && UNIT_METADATA_NAMESPACE.equals(metadata.getUID().getNamespace())) {
            assert unitProvider != null;
            unit = unitProvider.getUnit((Class<? extends Quantity>) dimension);
            logger.trace("Item '{}' now has unit '{}'", name, unit);
        }
    }
}
