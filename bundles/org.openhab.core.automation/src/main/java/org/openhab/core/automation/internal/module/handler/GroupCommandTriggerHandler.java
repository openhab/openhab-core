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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.handler.BaseTriggerModuleHandler;
import org.openhab.core.automation.handler.TriggerHandlerCallback;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemAddedEvent;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemRemovedEvent;
import org.openhab.core.types.Command;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a ModuleHandler implementation for Triggers which trigger the rule
 * if a member of an item group receives a command.
 * The group name and command value can be set with the configuration.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class GroupCommandTriggerHandler extends BaseTriggerModuleHandler implements EventSubscriber {

    private final Logger logger = LoggerFactory.getLogger(GroupCommandTriggerHandler.class);

    private final String groupName;
    private final @Nullable String command;

    private final Set<String> types;
    private final BundleContext bundleContext;
    private final ItemRegistry itemRegistry;

    public static final String MODULE_TYPE_ID = "core.GroupCommandTrigger";

    public static final String CFG_GROUPNAME = "groupName";
    public static final String CFG_COMMAND = "command";
    private final String ruleUID;

    private ServiceRegistration<?> eventSubscriberRegistration;

    public GroupCommandTriggerHandler(Trigger module, String ruleUID, BundleContext bundleContext,
            ItemRegistry itemRegistry) {
        super(module);
        this.groupName = (String) module.getConfiguration().get(CFG_GROUPNAME);
        this.command = (String) module.getConfiguration().get(CFG_COMMAND);
        this.types = Set.of(ItemCommandEvent.TYPE, ItemAddedEvent.TYPE, ItemRemovedEvent.TYPE);
        this.bundleContext = bundleContext;
        this.itemRegistry = itemRegistry;
        this.ruleUID = ruleUID;
        eventSubscriberRegistration = this.bundleContext.registerService(EventSubscriber.class.getName(), this, null);

        if (itemRegistry.get(groupName) == null) {
            logger.warn("Group '{}' needed for rule '{}' is missing. Trigger '{}' will not work.", groupName, ruleUID,
                    module.getId());
        }
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return types;
    }

    @Override
    public void receive(Event event) {
        if (event instanceof ItemAddedEvent) {
            if (groupName.equals(((ItemAddedEvent) event).getItem().name)) {
                logger.info("Group '{}' needed for rule '{}' added. Trigger '{}' will now work.", groupName, ruleUID,
                        module.getId());
                return;
            }
        } else if (event instanceof ItemRemovedEvent) {
            if (groupName.equals(((ItemRemovedEvent) event).getItem().name)) {
                logger.warn("Group '{}' needed for rule '{}' removed. Trigger '{}' will no longer work.", groupName,
                        ruleUID, module.getId());
                return;
            }
        }

        if (callback instanceof TriggerHandlerCallback) {
            TriggerHandlerCallback cb = (TriggerHandlerCallback) callback;
            logger.trace("Received Event: Source: {} Topic: {} Type: {}  Payload: {}", event.getSource(),
                    event.getTopic(), event.getType(), event.getPayload());
            Map<String, Object> values = new HashMap<>();
            if (event instanceof ItemCommandEvent) {
                ItemCommandEvent icEvent = (ItemCommandEvent) event;
                String itemName = icEvent.getItemName();
                Item item = itemRegistry.get(itemName);
                if (item != null && item.getGroupNames().contains(groupName)) {
                    String command = this.command;
                    Command itemCommand = icEvent.getItemCommand();
                    if (command == null || command.equals(itemCommand.toFullString())) {
                        values.put("triggeringItem", item);
                        values.put("command", itemCommand);
                        values.put("event", event);
                        cb.triggered(this.module, values);
                    }
                }
            }
        }
    }

    /**
     * do the cleanup: unregistering eventSubscriber...
     */
    @Override
    public void dispose() {
        super.dispose();
        eventSubscriberRegistration.unregister();
    }
}
