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
package org.openhab.core.internal.items;

import javax.measure.Quantity;
import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemStateConverter;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.UnDefType;
import org.openhab.core.types.util.UnitUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convert a {@link State} to an {@link Item} accepted {@link State}.
 *
 * @author Henning Treu - Initial contribution
 */
@Component
@NonNullByDefault
public class ItemStateConverterImpl implements ItemStateConverter {

    private final Logger logger = LoggerFactory.getLogger(ItemStateConverterImpl.class);

    private final UnitProvider unitProvider;

    @Activate
    public ItemStateConverterImpl(final @Reference UnitProvider unitProvider) {
        this.unitProvider = unitProvider;
    }

    @Override
    public State convertToAcceptedState(@Nullable State state, @Nullable Item item) {
        if (state == null) {
            logger.error("A conversion of null was requested:",
                    new IllegalArgumentException("State must not be null."));
            return UnDefType.NULL;
        }

        if (item != null && !isAccepted(item, state)) {
            for (Class<? extends State> acceptedType : item.getAcceptedDataTypes()) {
                State convertedState = state.as(acceptedType);
                if (convertedState != null) {
                    logger.debug("Converting {} '{}' to {} '{}' for item '{}'", state.getClass().getSimpleName(), state,
                            convertedState.getClass().getSimpleName(), convertedState, item.getName());
                    return convertedState;
                }
            }
        }

        if (item instanceof NumberItem && state instanceof QuantityType) {
            NumberItem numberItem = (NumberItem) item;
            if (numberItem.getDimension() != null) {
                QuantityType<?> quantityState = (QuantityType<?>) state;

                // in case the item does define a unit it takes precedence over all other conversions:
                Unit<?> itemUnit = parseItemUnit(numberItem);
                if (itemUnit != null) {
                    if (!itemUnit.equals(quantityState.getUnit())) {
                        return convertOrUndef(quantityState, itemUnit);
                    }

                    return quantityState;
                }

                Class<? extends Quantity<?>> dimension = numberItem.getDimension();
                @SuppressWarnings({ "unchecked", "rawtypes" })
                // explicit cast to Class<? extends Quantity> as JDK compiler complains
                Unit<? extends Quantity<?>> conversionUnit = unitProvider
                        .getUnit((Class<? extends Quantity>) dimension);
                if (conversionUnit != null
                        && UnitUtils.isDifferentMeasurementSystem(conversionUnit, quantityState.getUnit())) {
                    return convertOrUndef(quantityState, conversionUnit);
                }

                return state;
            } else {
                State convertedState = state.as(DecimalType.class);
                if (convertedState != null) {
                    // convertedState is always returned because the state is an instance
                    // of QuantityType which never returns null for as(DecimalType.class)
                    return convertedState;
                }
            }
        }

        return state;
    }

    private State convertOrUndef(QuantityType<?> quantityState, Unit<?> targetUnit) {
        QuantityType<?> converted = quantityState.toUnit(targetUnit);
        if (converted != null) {
            return converted;
        }
        return UnDefType.UNDEF;
    }

    private @Nullable Unit<?> parseItemUnit(NumberItem numberItem) {
        StateDescription stateDescription = numberItem.getStateDescription();
        if (stateDescription == null) {
            return null;
        }
        String pattern = stateDescription.getPattern();
        if (pattern == null) {
            return null;
        } else {
            return UnitUtils.parseUnit(pattern);
        }
    }

    private boolean isAccepted(Item item, State state) {
        return item.getAcceptedDataTypes().contains(state.getClass());
    }
}
