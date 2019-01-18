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

import org.eclipse.smarthome.core.events.EventSubscriber;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.GroupItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.events.AbstractItemEventSubscriber;
import org.eclipse.smarthome.core.items.events.ItemCommandEvent;
import org.eclipse.smarthome.core.items.events.ItemStateEvent;
import org.eclipse.smarthome.core.types.State;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ItemUpdater listens on the event bus and passes any received status update
 * to the item registry.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Stefan Bußweiler - Migration to new ESH event concept
 */
@Component(immediate = true, service = EventSubscriber.class)
public class ItemUpdater extends AbstractItemEventSubscriber {

    private final Logger logger = LoggerFactory.getLogger(ItemUpdater.class);

    private ItemRegistry itemRegistry;

    @Reference
    protected void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    protected void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    @Override
    protected void receiveUpdate(ItemStateEvent updateEvent) {
        String itemName = updateEvent.getItemName();
        State newState = updateEvent.getItemState();
        try {
            GenericItem item = (GenericItem) itemRegistry.getItem(itemName);
            boolean isAccepted = false;
            if (item.getAcceptedDataTypes().contains(newState.getClass())) {
                isAccepted = true;
            } else {
                // Look for class hierarchy
                for (Class<? extends State> state : item.getAcceptedDataTypes()) {
                    try {
                        if (!state.isEnum() && state.newInstance().getClass().isAssignableFrom(newState.getClass())) {
                            isAccepted = true;
                            break;
                        }
                    } catch (InstantiationException e) {
                        logger.warn("InstantiationException on {}", e.getMessage()); // Should never happen
                    } catch (IllegalAccessException e) {
                        logger.warn("IllegalAccessException on {}", e.getMessage()); // Should never happen
                    }
                }
            }
            if (isAccepted) {
                item.setState(newState);
            } else {
                logger.debug("Received update of a not accepted type ({}) for item {}",
                        newState.getClass().getSimpleName(), itemName);
            }
        } catch (ItemNotFoundException e) {
            logger.debug("Received update for non-existing item: {}", e.getMessage());
        }
    }

    @Override
    protected void receiveCommand(ItemCommandEvent commandEvent) {
        try {
            Item item = itemRegistry.getItem(commandEvent.getItemName());
            if (item instanceof GroupItem) {
                GroupItem groupItem = (GroupItem) item;
                groupItem.send(commandEvent.getItemCommand());
            }
        } catch (ItemNotFoundException e) {
            logger.debug("Received command for non-existing item: {}", e.getMessage());
        }
    }

}
