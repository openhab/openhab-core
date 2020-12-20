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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.handler.BaseActionModuleHandler;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.items.events.ItemStateEvent;
import org.openhab.core.types.State;
import org.openhab.core.types.TypeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an implementation of an ActionHandler. It sends state updates for items.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class ItemStateUpdateActionHandler extends BaseActionModuleHandler {

    public static final String ITEM_STATE_UPDATE_ACTION = "core.ItemStateUpdateAction";
    public static final String ITEM_NAME = "itemName";
    public static final String STATE = "state";

    private final Logger logger = LoggerFactory.getLogger(ItemStateUpdateActionHandler.class);

    private final EventPublisher eventPublisher;
    private final ItemRegistry itemRegistry;

    public ItemStateUpdateActionHandler(Action module, EventPublisher eventPublisher, ItemRegistry itemRegistry) {
        super(module);

        this.eventPublisher = eventPublisher;
        this.itemRegistry = itemRegistry;
    }

    @Override
    public @Nullable Map<String, Object> execute(Map<String, Object> inputs) {
        Configuration config = module.getConfiguration();
        String itemName = (String) config.get(ITEM_NAME);
        String state = (String) config.get(STATE);

        if (itemName != null) {
            try {
                Item item = itemRegistry.getItem(itemName);

                State stateObj = null;
                final Object st = inputs.get(STATE);

                if (st instanceof State) {
                    if (item.getAcceptedDataTypes().contains(st.getClass())) {
                        stateObj = (State) st;
                    }
                } else {
                    stateObj = TypeParser.parseState(item.getAcceptedDataTypes(), state);
                }
                if (stateObj != null) {
                    final ItemStateEvent itemStateEvent = (ItemStateEvent) ItemEventFactory.createStateEvent(itemName,
                            stateObj);
                    logger.debug("Executing ItemStateEvent on Item {} with State {}", itemStateEvent.getItemName(),
                            itemStateEvent.getItemState());
                    eventPublisher.post(itemStateEvent);
                } else {
                    logger.warn("State '{}' is not valid for item '{}'.", state, itemName);
                }
            } catch (ItemNotFoundException e) {
                logger.error("Item with name {} not found in ItemRegistry.", itemName);
            }
        } else {
            logger.error(
                    "Item state was not updated because the configuration was not correct: ItemName: {}, State: {}",
                    itemName, state);
        }
        return null;
    }
}
