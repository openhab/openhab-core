/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.automation.internal.module.handler;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.handler.BaseConditionModuleHandler;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.types.State;
import org.openhab.core.types.TypeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConditionHandler implementation to check item state
 *
 * @author Benedikt Niehues - Initial contribution
 * @author Kai Kreuzer - refactored and simplified customized module handling
 */
@NonNullByDefault
public class ItemStateConditionHandler extends BaseConditionModuleHandler {

    /**
     * Constants for Config-Parameters corresponding to Definition in ItemModuleTypeDefinition.json
     */
    public static final String ITEM_NAME = "itemName";
    public static final String OPERATOR = "operator";
    public static final String STATE = "state";

    private final Logger logger = LoggerFactory.getLogger(ItemStateConditionHandler.class);

    public static final String ITEM_STATE_CONDITION = "core.ItemStateCondition";

    private @Nullable ItemRegistry itemRegistry;

    public ItemStateConditionHandler(Condition condition) {
        super(condition);
    }

    /**
     * setter for itemRegistry, used by DS
     *
     * @param itemRegistry
     */
    public void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    /**
     * unsetter for itemRegistry used by DS
     *
     * @param itemRegistry
     */
    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    @Override
    public void dispose() {
        itemRegistry = null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked", "null" })
    @Override
    public boolean isSatisfied(Map<String, Object> inputs) {
        String itemName = (String) module.getConfiguration().get(ITEM_NAME);
        String state = (String) module.getConfiguration().get(STATE);
        String operator = (String) module.getConfiguration().get(OPERATOR);
        if (operator == null || state == null || itemName == null) {
            logger.error("Module is not well configured: itemName={}  operator={}  state = {}", itemName, operator,
                    state);
            return false;
        }
        if (itemRegistry == null) {
            logger.error("The ItemRegistry is not available to evaluate the condition.");
            return false;
        }
        try {
            Item item = itemRegistry.getItem(itemName);
            State compareState = TypeParser.parseState(item.getAcceptedDataTypes(), state);
            State itemState = item.getState();
            logger.debug("ItemStateCondition '{}' checking if {} (State={}) {} {}", module.getId(), itemName, itemState,
                    operator, compareState);
            switch (operator) {
                case "=":
                    return itemState.equals(compareState);
                case "!=":
                    return !itemState.equals(compareState);
                case "<":
                    if (itemState instanceof QuantityType) {
                        QuantityType qtState = (QuantityType) itemState;
                        if (compareState instanceof DecimalType) {
                            // allow compareState without unit -> implicitly assume its the same as the one from the
                            // state, but warn the user
                            logger.warn(
                                    "Received a QuantityType state '{}' with unit for item {}, but the condition is defined as a plain number without unit ({}), please consider adding a unit to the condition.",
                                    qtState, itemName, state);
                            return qtState.compareTo(new QuantityType<>(((DecimalType) compareState).toBigDecimal(),
                                    qtState.getUnit())) < 0;
                        } else if (compareState instanceof QuantityType) {
                            return qtState.compareTo((QuantityType) compareState) < 0;
                        } else {
                            logger.warn(
                                    "Condition '{}' cannot be compared to the incompatible state '{}' from item {}.",
                                    state, qtState, itemName);
                        }
                    } else if (itemState instanceof DecimalType && null != compareState) {
                        DecimalType decimalState = compareState.as(DecimalType.class);
                        if (null != decimalState) {
                            return ((DecimalType) itemState).compareTo(decimalState) < 0;
                        }
                    }
                    break;
                case "<=":
                case "=<":
                    if (itemState instanceof QuantityType) {
                        QuantityType qtState = (QuantityType) itemState;
                        if (compareState instanceof DecimalType) {
                            // allow compareState without unit -> implicitly assume its the same as the one from the
                            // state, but warn the user
                            logger.warn(
                                    "Received a QuantityType state '{}' with unit for item {}, but the condition is defined as a plain number without unit ({}), please consider adding a unit to the condition.",
                                    qtState, itemName, state);
                            return qtState.compareTo(new QuantityType<>(((DecimalType) compareState).toBigDecimal(),
                                    qtState.getUnit())) <= 0;
                        } else if (compareState instanceof QuantityType) {
                            return qtState.compareTo((QuantityType) compareState) <= 0;
                        } else {
                            logger.warn(
                                    "Condition '{}' cannot be compared to the incompatible state '{}' from item {}.",
                                    state, qtState, itemName);
                        }
                    } else if (itemState instanceof DecimalType && null != compareState) {
                        DecimalType decimalState = compareState.as(DecimalType.class);
                        if (null != decimalState) {
                            return ((DecimalType) itemState).compareTo(decimalState) <= 0;
                        }
                    }
                    break;
                case ">":
                    if (itemState instanceof QuantityType) {
                        QuantityType qtState = (QuantityType) itemState;
                        if (compareState instanceof DecimalType) {
                            // allow compareState without unit -> implicitly assume its the same as the one from the
                            // state, but warn the user
                            logger.warn(
                                    "Received a QuantityType state '{}' with unit  for item {}, but the condition is defined as a plain number without unit ({}), please consider adding a unit to the condition.",
                                    qtState, itemName, state);
                            return qtState.compareTo(new QuantityType<>(((DecimalType) compareState).toBigDecimal(),
                                    qtState.getUnit())) > 0;
                        } else if (compareState instanceof QuantityType) {
                            return qtState.compareTo((QuantityType) compareState) > 0;
                        } else {
                            logger.warn(
                                    "Condition '{}' cannot be compared to the incompatible state '{}' from item {}.",
                                    state, qtState, itemName);
                        }
                    } else if (itemState instanceof DecimalType && null != compareState) {
                        DecimalType decimalState = compareState.as(DecimalType.class);
                        if (null != decimalState) {
                            return ((DecimalType) itemState).compareTo(decimalState) > 0;
                        }
                    }
                    break;
                case ">=":
                case "=>":
                    if (itemState instanceof QuantityType) {
                        QuantityType qtState = (QuantityType) itemState;
                        if (compareState instanceof DecimalType) {
                            // allow compareState without unit -> implicitly assume its the same as the one from the
                            // state, but warn the user
                            logger.warn(
                                    "Received a QuantityType state '{}' with unit for item {}, but the condition is defined as a plain number without unit ({}), please consider adding a unit to the condition.",
                                    qtState, itemName, state);
                            return qtState.compareTo(new QuantityType<>(((DecimalType) compareState).toBigDecimal(),
                                    qtState.getUnit())) >= 0;
                        } else if (compareState instanceof QuantityType) {
                            return qtState.compareTo((QuantityType) compareState) >= 0;
                        } else {
                            logger.warn(
                                    "Condition '{}' cannot be compared to the incompatible state '{}' from item {}.",
                                    state, qtState, itemName);
                        }
                    } else if (itemState instanceof DecimalType && null != compareState) {
                        DecimalType decimalState = compareState.as(DecimalType.class);
                        if (null != decimalState) {
                            return ((DecimalType) itemState).compareTo(decimalState) >= 0;
                        }
                    }
                    break;
            }
        } catch (ItemNotFoundException e) {
            logger.error("Item with name {} not found in ItemRegistry.", itemName);
        }
        return false;
    }
}
