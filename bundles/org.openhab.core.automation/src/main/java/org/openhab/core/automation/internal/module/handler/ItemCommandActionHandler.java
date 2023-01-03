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
package org.openhab.core.automation.internal.module.handler;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.handler.BaseActionModuleHandler;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.types.Command;
import org.openhab.core.types.TypeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an implementation of an ActionHandler. It sends commands for items.
 *
 * @author Benedikt Niehues - Initial contribution
 * @author Kai Kreuzer - refactored and simplified customized module handling
 * @author Stefan Triller - use command from input first and if not set, use command from configuration
 */
@NonNullByDefault
public class ItemCommandActionHandler extends BaseActionModuleHandler {

    public static final String ITEM_COMMAND_ACTION = "core.ItemCommandAction";
    public static final String ITEM_NAME = "itemName";
    public static final String COMMAND = "command";

    private final Logger logger = LoggerFactory.getLogger(ItemCommandActionHandler.class);

    private final EventPublisher eventPublisher;
    private final ItemRegistry itemRegistry;

    /**
     * constructs a new ItemCommandActionHandler
     *
     * @param module
     * @param moduleTypes
     */
    public ItemCommandActionHandler(Action module, EventPublisher eventPublisher, ItemRegistry itemRegistry) {
        super(module);
        this.eventPublisher = eventPublisher;
        this.itemRegistry = itemRegistry;
    }

    @Override
    public @Nullable Map<String, Object> execute(Map<String, Object> inputs) {
        String itemName = (String) module.getConfiguration().get(ITEM_NAME);
        String command = (String) module.getConfiguration().get(COMMAND);

        if (itemName != null) {
            try {
                Item item = itemRegistry.getItem(itemName);

                Command commandObj = null;
                if (command != null) {
                    commandObj = TypeParser.parseCommand(item.getAcceptedCommandTypes(), command);
                } else {
                    Object cmd = inputs.get(COMMAND);

                    if (cmd instanceof Command) {
                        if (item.getAcceptedCommandTypes().contains(cmd.getClass())) {
                            commandObj = (Command) cmd;
                        }
                    }
                }
                if (commandObj != null) {
                    ItemCommandEvent itemCommandEvent = ItemEventFactory.createCommandEvent(itemName, commandObj);
                    logger.debug("Executing ItemCommandAction on Item {} with Command {}",
                            itemCommandEvent.getItemName(), itemCommandEvent.getItemCommand());
                    eventPublisher.post(itemCommandEvent);
                } else {
                    logger.debug("Command '{}' is not valid for item '{}'.", command, itemName);
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
