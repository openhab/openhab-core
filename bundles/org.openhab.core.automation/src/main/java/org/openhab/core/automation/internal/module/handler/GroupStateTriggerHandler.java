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
import org.openhab.core.items.events.GroupItemStateChangedEvent;
import org.openhab.core.items.events.ItemAddedEvent;
import org.openhab.core.items.events.ItemRemovedEvent;
import org.openhab.core.items.events.ItemStateChangedEvent;
import org.openhab.core.items.events.ItemStateEvent;
import org.openhab.core.types.State;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a ModuleHandler implementation for Triggers which trigger the rule
 * if state event of a member of an item group occurs.
 * The group name and state value can be set with the configuration.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class GroupStateTriggerHandler extends BaseTriggerModuleHandler implements EventSubscriber {

    public static final String UPDATE_MODULE_TYPE_ID = "core.GroupStateUpdateTrigger";
    public static final String CHANGE_MODULE_TYPE_ID = "core.GroupStateChangeTrigger";

    public static final String CFG_GROUPNAME = "groupName";
    public static final String CFG_STATE = "state";
    public static final String CFG_PREVIOUS_STATE = "previousState";

    private final Logger logger = LoggerFactory.getLogger(GroupStateTriggerHandler.class);

    private final String groupName;
    private final @Nullable String state;
    private final String previousState;
    private final String ruleUID;
    private Set<String> types;
    private final BundleContext bundleContext;
    private ItemRegistry itemRegistry;

    private ServiceRegistration<?> eventSubscriberRegistration;

    public GroupStateTriggerHandler(Trigger module, String ruleUID, BundleContext bundleContext,
            ItemRegistry itemRegistry) {
        super(module);
        this.groupName = (String) module.getConfiguration().get(CFG_GROUPNAME);
        this.state = (String) module.getConfiguration().get(CFG_STATE);
        this.previousState = (String) module.getConfiguration().get(CFG_PREVIOUS_STATE);
        if (UPDATE_MODULE_TYPE_ID.equals(module.getTypeUID())) {
            this.types = Set.of(ItemStateEvent.TYPE, ItemAddedEvent.TYPE, ItemRemovedEvent.TYPE);
        } else {
            this.types = Set.of(ItemStateChangedEvent.TYPE, GroupItemStateChangedEvent.TYPE, ItemAddedEvent.TYPE,
                    ItemRemovedEvent.TYPE);
        }
        this.bundleContext = bundleContext;
        this.ruleUID = ruleUID;
        this.itemRegistry = itemRegistry;
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
            if (event instanceof ItemStateEvent && UPDATE_MODULE_TYPE_ID.equals(module.getTypeUID())) {
                ItemStateEvent isEvent = (ItemStateEvent) event;
                String itemName = isEvent.getItemName();
                Item item = itemRegistry.get(itemName);
                if (item != null && item.getGroupNames().contains(groupName)) {
                    State state = isEvent.getItemState();
                    if ((this.state == null || state.toFullString().equals(this.state))) {
                        Map<String, Object> values = new HashMap<>();
                        values.put("triggeringItem", item);
                        values.put("state", state);
                        values.put("event", event);
                        cb.triggered(this.module, values);
                    }
                }
            } else if (event instanceof ItemStateChangedEvent && CHANGE_MODULE_TYPE_ID.equals(module.getTypeUID())) {
                ItemStateChangedEvent iscEvent = (ItemStateChangedEvent) event;
                String itemName = iscEvent.getItemName();
                Item item = itemRegistry.get(itemName);
                if (item != null && item.getGroupNames().contains(groupName)) {
                    State state = iscEvent.getItemState();
                    State oldState = iscEvent.getOldItemState();

                    if (stateMatches(this.state, state) && stateMatches(this.previousState, oldState)) {
                        Map<String, Object> values = new HashMap<>();
                        values.put("triggeringItem", item);
                        values.put("oldState", oldState);
                        values.put("newState", state);
                        values.put("event", event);
                        cb.triggered(this.module, values);
                    }
                }
            }
        }
    }

    private boolean stateMatches(@Nullable String requiredState, State state) {
        if (requiredState == null) {
            return true;
        }

        String reqState = requiredState.trim();
        return reqState.isEmpty() || reqState.equals(state.toFullString());
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
