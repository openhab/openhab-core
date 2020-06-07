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

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.handler.BaseTriggerModuleHandler;
import org.openhab.core.automation.handler.TriggerHandlerCallback;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.GroupItemStateChangedEvent;
import org.openhab.core.items.events.ItemStateChangedEvent;
import org.openhab.core.items.events.ItemStateEvent;
import org.openhab.core.types.State;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an ModuleHandler implementation for Triggers which trigger the rule
 * if state event of a member of an item group occurs.
 * The group name and state value can be set with the configuration.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class GroupStateTriggerHandler extends BaseTriggerModuleHandler implements EventSubscriber, EventFilter {

    public static final String UPDATE_MODULE_TYPE_ID = "core.GroupStateUpdateTrigger";
    public static final String CHANGE_MODULE_TYPE_ID = "core.GroupStateChangeTrigger";

    public static final String CFG_GROUPNAME = "groupName";
    public static final String CFG_STATE = "state";
    public static final String CFG_PREVIOUS_STATE = "previousState";

    private final Logger logger = LoggerFactory.getLogger(GroupStateTriggerHandler.class);

    private final String groupName;
    private final @Nullable String state;
    private final String previousState;
    private Set<String> types;
    private final BundleContext bundleContext;
    private @Nullable ItemRegistry itemRegistry;

    private ServiceRegistration<?> eventSubscriberRegistration;

    public GroupStateTriggerHandler(Trigger module, BundleContext bundleContext) {
        super(module);
        this.groupName = (String) module.getConfiguration().get(CFG_GROUPNAME);
        this.state = (String) module.getConfiguration().get(CFG_STATE);
        this.previousState = (String) module.getConfiguration().get(CFG_PREVIOUS_STATE);
        if (UPDATE_MODULE_TYPE_ID.equals(module.getTypeUID())) {
            this.types = Collections.singleton(ItemStateEvent.TYPE);
        } else {
            Set<String> set = new HashSet<>();
            set.add(ItemStateChangedEvent.TYPE);
            set.add(GroupItemStateChangedEvent.TYPE);
            this.types = Collections.unmodifiableSet(set);
        }
        this.bundleContext = bundleContext;
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("event.topics", "smarthome/items/*");
        eventSubscriberRegistration = this.bundleContext.registerService(EventSubscriber.class.getName(), this,
                properties);
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return types;
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return this;
    }

    @Override
    public void receive(Event event) {
        if (callback instanceof TriggerHandlerCallback) {
            TriggerHandlerCallback cb = (TriggerHandlerCallback) callback;
            logger.trace("Received Event: Source: {} Topic: {} Type: {}  Payload: {}", event.getSource(),
                    event.getTopic(), event.getType(), event.getPayload());
            if (event instanceof ItemStateEvent && UPDATE_MODULE_TYPE_ID.equals(module.getTypeUID())) {
                ItemStateEvent isEvent = (ItemStateEvent) event;
                String itemName = isEvent.getItemName();
                if (itemRegistry != null) {
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
                }
            } else if (event instanceof ItemStateChangedEvent && CHANGE_MODULE_TYPE_ID.equals(module.getTypeUID())) {
                ItemStateChangedEvent iscEvent = (ItemStateChangedEvent) event;
                String itemName = iscEvent.getItemName();
                if (itemRegistry != null) {
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

    @Override
    public boolean apply(Event event) {
        logger.trace("->FILTER: {}:{}", event.getTopic(), groupName);
        return event.getTopic().startsWith("smarthome/items/");
    }

    public void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }
}
