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
package org.openhab.core.automation.internal.module.handler;

import java.util.Map;

import org.openhab.core.automation.Condition;
import org.openhab.core.automation.handler.BaseConditionModuleHandler;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.DecimalType;
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
public class ItemStateConditionHandler extends BaseConditionModuleHandler {

    private final Logger logger = LoggerFactory.getLogger(ItemStateConditionHandler.class);

    public static final String ITEM_STATE_CONDITION = "core.ItemStateCondition";

    private ItemRegistry itemRegistry;

    /**
     * Constants for Config-Parameters corresponding to Definition in
     * ItemModuleTypeDefinition.json
     */
    private static final String ITEM_NAME = "itemName";
    private static final String OPERATOR = "operator";
    private static final String STATE = "state";

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
            logger.debug("ItemStateCondition '{}'checking if {} (State={}) {} {}", module.getId(), itemName, itemState,
                    operator, compareState);
            switch (operator) {
                case "=":
                    logger.debug("ConditionSatisfied --> {}", itemState.equals(compareState));
                    return itemState.equals(compareState);
                case "!=":
                    return !itemState.equals(compareState);
                case "<":
                    if (itemState instanceof DecimalType && compareState instanceof DecimalType) {
                        return ((DecimalType) itemState).compareTo((DecimalType) compareState) < 0;
                    }
                    break;
                case "<=":
                case "=<":
                    if (itemState instanceof DecimalType && compareState instanceof DecimalType) {
                        return ((DecimalType) itemState).compareTo((DecimalType) compareState) <= 0;
                    }
                    break;
                case ">":
                    if (itemState instanceof DecimalType && compareState instanceof DecimalType) {
                        return ((DecimalType) itemState).compareTo((DecimalType) compareState) > 0;
                    }
                    break;
                case ">=":
                case "=>":
                    if (itemState instanceof DecimalType && compareState instanceof DecimalType) {
                        return ((DecimalType) itemState).compareTo((DecimalType) compareState) >= 0;
                    }
                    break;
                default:
                    break;
            }
        } catch (ItemNotFoundException e) {
            logger.error("Item with Name {} not found in itemRegistry", itemName);
            return false;
        }
        return false;
    }

}
