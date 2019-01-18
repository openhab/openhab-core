/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.internal.items;

import javax.measure.Quantity;
import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.i18n.UnitProvider;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemStateConverter;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.UnDefType;
import org.eclipse.smarthome.core.types.util.UnitUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convert a {@link State} to an {@link Item} accepted {@link State}.
 *
 * @author Henning Treu - initial contribution and API
 *
 */
@Component
public class ItemStateConverterImpl implements ItemStateConverter {

    private final Logger logger = LoggerFactory.getLogger(ItemStateConverterImpl.class);

    private UnitProvider unitProvider;

    @Override
    public @NonNull State convertToAcceptedState(@Nullable State state, @Nullable Item item) {
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
                Unit<? extends Quantity<?>> conversionUnit = unitProvider.getUnit((Class<? extends Quantity>) dimension);
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

    private @NonNull State convertOrUndef(QuantityType<?> quantityState, Unit<?> targetUnit) {
        QuantityType<?> converted = quantityState.toUnit(targetUnit);
        if (converted != null) {
            return converted;
        }
        return UnDefType.UNDEF;
    }

    private Unit<?> parseItemUnit(NumberItem numberItem) {
        StateDescription stateDescription = numberItem.getStateDescription();
        if (stateDescription == null) {
            return null;
        }

        String pattern = stateDescription.getPattern();
        return UnitUtils.parseUnit(pattern);
    }

    private boolean isAccepted(Item item, State state) {
        return item.getAcceptedDataTypes().contains(state.getClass());
    }

    @Reference
    public void setUnitProvider(UnitProvider unitProvider) {
        this.unitProvider = unitProvider;
    }

    protected void unsetUnitProvider(UnitProvider unitProvider) {
        this.unitProvider = null;
    }

}
