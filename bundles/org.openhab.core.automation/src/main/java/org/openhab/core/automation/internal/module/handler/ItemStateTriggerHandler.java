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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.handler.BaseTriggerModuleHandler;
import org.openhab.core.automation.handler.TriggerHandlerCallback;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
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
 * if an item state event occurs. The eventType and state value can be set with the
 * configuration.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Simon Merschjohann - Initial contribution
 */
public class ItemStateTriggerHandler extends BaseTriggerModuleHandler implements EventSubscriber, EventFilter {

    public static final String UPDATE_MODULE_TYPE_ID = "core.ItemStateUpdateTrigger";
    public static final String CHANGE_MODULE_TYPE_ID = "core.ItemStateChangeTrigger";

    public static final String CFG_ITEMNAME = "itemName";
    public static final String CFG_STATE = "state";
    public static final String CFG_PREVIOUS_STATE = "previousState";

    private final Logger logger = LoggerFactory.getLogger(ItemStateTriggerHandler.class);

    private final String itemName;
    private final String state;
    private final String previousState;
    private Set<String> types;
    private final BundleContext bundleContext;

    private ServiceRegistration<?> eventSubscriberRegistration;

    public ItemStateTriggerHandler(Trigger module, BundleContext bundleContext) {
        super(module);
        this.itemName = (String) module.getConfiguration().get(CFG_ITEMNAME);
        this.state = (String) module.getConfiguration().get(CFG_STATE);
        this.previousState = (String) module.getConfiguration().get(CFG_PREVIOUS_STATE);
        if (UPDATE_MODULE_TYPE_ID.equals(module.getTypeUID())) {
            this.types = Set.of(ItemStateEvent.TYPE);
        } else {
            this.types = Set.of(ItemStateChangedEvent.TYPE, GroupItemStateChangedEvent.TYPE);
        }
        this.bundleContext = bundleContext;
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("event.topics", "openhab/items/" + itemName + "/*");
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
        return event.getTopic().contains("openhab/items/" + itemName + "/");
    }
}
