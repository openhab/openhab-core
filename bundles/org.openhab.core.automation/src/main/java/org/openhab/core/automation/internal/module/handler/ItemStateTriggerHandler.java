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
import org.openhab.core.automation.ModuleHandlerCallback;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.handler.BaseTriggerModuleHandler;
import org.openhab.core.automation.handler.TriggerHandlerCallback;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.events.TopicPrefixEventFilter;
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
 * if an item state event occurs. The eventType and state value can be set with the
 * configuration.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Simon Merschjohann - Initial contribution
 */
@NonNullByDefault
public class ItemStateTriggerHandler extends BaseTriggerModuleHandler implements EventSubscriber {

    public static final String UPDATE_MODULE_TYPE_ID = "core.ItemStateUpdateTrigger";
    public static final String CHANGE_MODULE_TYPE_ID = "core.ItemStateChangeTrigger";

    public static final String CFG_ITEMNAME = "itemName";
    public static final String CFG_STATE = "state";
    public static final String CFG_PREVIOUS_STATE = "previousState";

    private final Logger logger = LoggerFactory.getLogger(ItemStateTriggerHandler.class);

    private final String itemName;
    private final @Nullable String state;
    private final String previousState;
    private final String ruleUID;
    private Set<String> types;
    private final BundleContext bundleContext;
    private final EventFilter eventFilter;

    private @Nullable ServiceRegistration<?> eventSubscriberRegistration;

    public ItemStateTriggerHandler(Trigger module, String ruleUID, BundleContext bundleContext,
            ItemRegistry itemRegistry) {
        super(module);
        this.itemName = (String) module.getConfiguration().get(CFG_ITEMNAME);
        this.eventFilter = new TopicPrefixEventFilter("openhab/items/" + itemName + "/");
        this.state = (String) module.getConfiguration().get(CFG_STATE);
        this.previousState = (String) module.getConfiguration().get(CFG_PREVIOUS_STATE);
        this.ruleUID = ruleUID;
        if (UPDATE_MODULE_TYPE_ID.equals(module.getTypeUID())) {
            this.types = Set.of(ItemStateEvent.TYPE, ItemAddedEvent.TYPE, ItemRemovedEvent.TYPE);
        } else {
            this.types = Set.of(ItemStateChangedEvent.TYPE, GroupItemStateChangedEvent.TYPE, ItemAddedEvent.TYPE,
                    ItemRemovedEvent.TYPE);
        }
        this.bundleContext = bundleContext;
        eventSubscriberRegistration = this.bundleContext.registerService(EventSubscriber.class.getName(), this, null);

        if (itemRegistry.get(itemName) == null) {
            logger.warn("Item '{}' needed for rule '{}' is missing. Trigger '{}' will not work.", itemName, ruleUID,
                    module.getId());
        }
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return types;
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return eventFilter;
    }

    @Override
    public void receive(Event event) {
        if (event instanceof ItemAddedEvent) {
            if (itemName.equals(((ItemAddedEvent) event).getItem().name)) {
                logger.info("Item '{}' needed for rule '{}' added. Trigger '{}' will now work.", itemName, ruleUID,
                        module.getId());
                return;
            }
        } else if (event instanceof ItemRemovedEvent) {
            if (itemName.equals(((ItemRemovedEvent) event).getItem().name)) {
                logger.warn("Item '{}' needed for rule '{}' removed. Trigger '{}' will no longer work.", itemName,
                        ruleUID, module.getId());
                return;
            }
        }

        ModuleHandlerCallback callback = this.callback;
        if (callback != null) {
            logger.trace("Received Event: Source: {} Topic: {} Type: {}  Payload: {}", event.getSource(),
                    event.getTopic(), event.getType(), event.getPayload());
            Map<String, Object> values = new HashMap<>();
            if (event instanceof ItemStateEvent && UPDATE_MODULE_TYPE_ID.equals(module.getTypeUID())) {
                String state = this.state;
                State itemState = ((ItemStateEvent) event).getItemState();
                if ((state == null || state.equals(itemState.toFullString()))) {
                    values.put("state", itemState);
                }
            } else if (event instanceof ItemStateChangedEvent && CHANGE_MODULE_TYPE_ID.equals(module.getTypeUID())) {
                State itemState = ((ItemStateChangedEvent) event).getItemState();
                State oldItemState = ((ItemStateChangedEvent) event).getOldItemState();

                if (stateMatches(this.state, itemState) && stateMatches(this.previousState, oldItemState)) {
                    values.put("oldState", oldItemState);
                    values.put("newState", itemState);
                }
            }
            if (!values.isEmpty()) {
                values.put("event", event);
                ((TriggerHandlerCallback) callback).triggered(this.module, values);
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
        if (eventSubscriberRegistration != null) {
            eventSubscriberRegistration.unregister();
            eventSubscriberRegistration = null;
        }
    }
}
