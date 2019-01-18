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
package org.eclipse.smarthome.automation.module.core.handler;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.handler.BaseTriggerModuleHandler;
import org.eclipse.smarthome.automation.handler.TriggerHandlerCallback;
import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventFilter;
import org.eclipse.smarthome.core.events.EventSubscriber;
import org.eclipse.smarthome.core.items.events.GroupItemStateChangedEvent;
import org.eclipse.smarthome.core.items.events.ItemStateChangedEvent;
import org.eclipse.smarthome.core.items.events.ItemStateEvent;
import org.eclipse.smarthome.core.types.State;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an ModuleHandler implementation for Triggers which trigger the rule
 * if an item state event occurs. The eventType and state value can be set with the
 * configuration.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Simon Merschjohann
 *
 */
public class ItemStateTriggerHandler extends BaseTriggerModuleHandler implements EventSubscriber, EventFilter {
    private final Logger logger = LoggerFactory.getLogger(ItemStateTriggerHandler.class);

    private final String itemName;
    private final String state;
    private final String previousState;
    private Set<String> types;
    private final BundleContext bundleContext;

    public static final String UPDATE_MODULE_TYPE_ID = "core.ItemStateUpdateTrigger";
    public static final String CHANGE_MODULE_TYPE_ID = "core.ItemStateChangeTrigger";

    private static final String CFG_ITEMNAME = "itemName";
    private static final String CFG_STATE = "state";
    private static final String CFG_PREVIOUS_STATE = "previousState";

    @SuppressWarnings("rawtypes")
    private ServiceRegistration eventSubscriberRegistration;

    public ItemStateTriggerHandler(Trigger module, BundleContext bundleContext) {
        super(module);
        this.itemName = (String) module.getConfiguration().get(CFG_ITEMNAME);
        this.state = (String) module.getConfiguration().get(CFG_STATE);
        this.previousState = (String) module.getConfiguration().get(CFG_PREVIOUS_STATE);
        if (UPDATE_MODULE_TYPE_ID.equals(module.getTypeUID())) {
            this.types = Collections.singleton(ItemStateEvent.TYPE);
        } else {
            HashSet<String> set = new HashSet<>();
            set.add(ItemStateChangedEvent.TYPE);
            set.add(GroupItemStateChangedEvent.TYPE);
            this.types = Collections.unmodifiableSet(set);
        }
        this.bundleContext = bundleContext;
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("event.topics", "smarthome/items/" + itemName + "/*");
        eventSubscriberRegistration = this.bundleContext.registerService(EventSubscriber.class.getName(), this,
                properties);
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return types;
    }

    @Override
    public EventFilter getEventFilter() {
        return this;
    }

    @Override
    public void receive(Event event) {
        if (callback != null) {
            logger.trace("Received Event: Source: {} Topic: {} Type: {}  Payload: {}", event.getSource(),
                    event.getTopic(), event.getType(), event.getPayload());
            Map<String, Object> values = new HashMap<>();
            if (event instanceof ItemStateEvent && UPDATE_MODULE_TYPE_ID.equals(module.getTypeUID())) {
                State state = ((ItemStateEvent) event).getItemState();
                if ((this.state == null || this.state.equals(state.toFullString()))) {
                    values.put("state", state);
                }
            } else if (event instanceof ItemStateChangedEvent && CHANGE_MODULE_TYPE_ID.equals(module.getTypeUID())) {
                State state = ((ItemStateChangedEvent) event).getItemState();
                State oldState = ((ItemStateChangedEvent) event).getOldItemState();

                if (stateMatches(this.state, state) && stateMatches(this.previousState, oldState)) {
                    values.put("oldState", oldState);
                    values.put("newState", state);
                }
            }
            if (!values.isEmpty()) {
                values.put("event", event);
                ((TriggerHandlerCallback) callback).triggered(this.module, values);
            }
        }
    }

    private boolean stateMatches(String requiredState, State state) {
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
        if (eventSubscriberRegistration != null) {
            eventSubscriberRegistration.unregister();
            eventSubscriberRegistration = null;
        }
    }

    @Override
    public boolean apply(Event event) {
        logger.trace("->FILTER: {}:{}", event.getTopic(), itemName);
        return event.getTopic().contains("smarthome/items/" + itemName + "/");
    }

}
