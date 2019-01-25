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
package org.openhab.core.automation.internal.module.handler;

import java.util.Map;

import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.events.ItemCommandEvent;
import org.eclipse.smarthome.core.items.events.ItemEventFactory;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.TypeParser;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.handler.ActionHandler;
import org.openhab.core.automation.handler.BaseModuleHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an implementation of an ActionHandler. It sends commands for items.
 *
 * @author Benedikt Niehues - Initial contribution and API
 * @author Kai Kreuzer - refactored and simplified customized module handling
 * @author Stefan Triller - use command from input first and if not set, use command from configuration
 *
 */
public class ItemCommandActionHandler extends BaseModuleHandler<Action> implements ActionHandler {

    private final Logger logger = LoggerFactory.getLogger(ItemCommandActionHandler.class);

    public static final String ITEM_COMMAND_ACTION = "core.ItemCommandAction";
    private static final String ITEM_NAME = "itemName";
    private static final String COMMAND = "command";

    private ItemRegistry itemRegistry;
    private EventPublisher eventPublisher;

    /**
     * constructs a new ItemCommandActionHandler
     *
     * @param module
     * @param moduleTypes
     */
    public ItemCommandActionHandler(Action module) {
        super(module);
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
     * unsetter for itemRegistry, used by DS
     *
     * @param itemRegistry
     */
    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    /**
     * setter for eventPublisher used by DS
     *
     * @param eventPublisher
     */
    public void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * unsetter for eventPublisher used by DS
     *
     * @param eventPublisher
     */
    public void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }

    @Override
    public void dispose() {
        this.eventPublisher = null;
        this.itemRegistry = null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> inputs) {
        String itemName = (String) module.getConfiguration().get(ITEM_NAME);
        String command = (String) module.getConfiguration().get(COMMAND);

        if (itemName != null && eventPublisher != null && itemRegistry != null) {
            try {
                Item item = itemRegistry.getItem(itemName);

                Command commandObj = null;
                Object cmd = inputs.get(COMMAND);

                if (cmd instanceof Command) {
                    if (item.getAcceptedCommandTypes().contains(cmd.getClass())) {
                        commandObj = (Command) cmd;
                    }
                } else {
                    commandObj = TypeParser.parseCommand(item.getAcceptedCommandTypes(), command);
                }
                if (commandObj != null) {
                    ItemCommandEvent itemCommandEvent = ItemEventFactory.createCommandEvent(itemName, commandObj);
                    logger.debug("Executing ItemCommandAction on Item {} with Command {}",
                            itemCommandEvent.getItemName(), itemCommandEvent.getItemCommand());
                    eventPublisher.post(itemCommandEvent);
                } else {
                    logger.warn("Command '{}' is not valid for item '{}'.", command, itemName);
                }
            } catch (ItemNotFoundException e) {
                logger.error("Item with name {} not found in ItemRegistry.", itemName);
            }
        } else {
            logger.error(
                    "Command was not posted because either the configuration was not correct or a service was missing: ItemName: {}, Command: {}, eventPublisher: {}, ItemRegistry: {}",
                    itemName, command, eventPublisher, itemRegistry);
        }
        return null;
    }

}
