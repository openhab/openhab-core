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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.handler.BaseTriggerModuleHandler;
import org.openhab.core.automation.handler.TriggerHandlerCallback;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.thing.events.ChannelTriggeredEvent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an ModuleHandler implementation for trigger channels with specific events
 *
 * @author Stefan Triller - Initial contribution
 */
public class ChannelEventTriggerHandler extends BaseTriggerModuleHandler implements EventSubscriber, EventFilter {

    public static final String MODULE_TYPE_ID = "core.ChannelEventTrigger";

    public static final String CFG_CHANNEL_EVENT = "event";
    public static final String CFG_CHANNEL = "channelUID";
    public static final String TOPIC = "openhab/channels/*/triggered";

    private final Logger logger = LoggerFactory.getLogger(ChannelEventTriggerHandler.class);

    private final String eventOnChannel;
    private final String channelUID;
    private final Set<String> types = new HashSet<>();
    private final BundleContext bundleContext;

    @SuppressWarnings("rawtypes")
    private ServiceRegistration eventSubscriberRegistration;

    public ChannelEventTriggerHandler(Trigger module, BundleContext bundleContext) {
        super(module);

        this.eventOnChannel = (String) module.getConfiguration().get(CFG_CHANNEL_EVENT);
        this.channelUID = (String) module.getConfiguration().get(CFG_CHANNEL);
        this.bundleContext = bundleContext;
        this.types.add("ChannelTriggeredEvent");

        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("event.topics", TOPIC);
        eventSubscriberRegistration = this.bundleContext.registerService(EventSubscriber.class.getName(), this,
                properties);
    }

    @Override
    public void receive(Event event) {
        if (callback != null) {
            logger.trace("Received Event: Source: {} Topic: {} Type: {}  Payload: {}", event.getSource(),
                    event.getTopic(), event.getType(), event.getPayload());

            Map<String, Object> values = new HashMap<>();
            values.put("event", event);

            ((TriggerHandlerCallback) callback).triggered(this.module, values);
        }
    }

    @Override
    public boolean apply(Event event) {
        logger.trace("->FILTER: {}:{}", event.getTopic(), TOPIC);

        boolean eventMatches = false;
        if (event instanceof ChannelTriggeredEvent) {
            ChannelTriggeredEvent cte = (ChannelTriggeredEvent) event;
            if (cte.getTopic().contains(this.channelUID)) {
                logger.trace("->FILTER: {}:{}", cte.getEvent(), eventOnChannel);
                eventMatches = true;
                if (eventOnChannel != null && !eventOnChannel.isEmpty() && !eventOnChannel.equals(cte.getEvent())) {
                    eventMatches = false;
                }
            }
        }
        return eventMatches;
    }

    @Override
    public EventFilter getEventFilter() {
        return this;
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return types;
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
