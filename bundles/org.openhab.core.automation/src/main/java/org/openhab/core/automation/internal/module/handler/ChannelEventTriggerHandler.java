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
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.events.ChannelTriggeredEvent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a ModuleHandler implementation for trigger channels with specific events
 *
 * @author Stefan Triller - Initial contribution
 */
@NonNullByDefault
public class ChannelEventTriggerHandler extends BaseTriggerModuleHandler implements EventSubscriber, EventFilter {

    public static final String MODULE_TYPE_ID = "core.ChannelEventTrigger";

    public static final String CFG_CHANNEL_EVENT = "event";
    public static final String CFG_CHANNEL = "channelUID";
    public static final String TOPIC = "openhab/channels/*/triggered";

    private final Logger logger = LoggerFactory.getLogger(ChannelEventTriggerHandler.class);

    private @Nullable final String eventOnChannel;
    private final ChannelUID channelUID;
    private final Set<String> types;
    private final BundleContext bundleContext;
    private final ServiceRegistration<?> eventSubscriberRegistration;

    public ChannelEventTriggerHandler(Trigger module, BundleContext bundleContext) {
        super(module);

        this.eventOnChannel = (String) module.getConfiguration().get(CFG_CHANNEL_EVENT);
        this.channelUID = new ChannelUID((String) module.getConfiguration().get(CFG_CHANNEL));
        this.types = Set.of("ChannelTriggeredEvent");
        this.bundleContext = bundleContext;

        eventSubscriberRegistration = this.bundleContext.registerService(EventSubscriber.class.getName(), this, null);
    }

    @Override
    public void receive(Event event) {
        ModuleHandlerCallback localCallback = callback;
        if (localCallback != null) {
            logger.trace("Received Event: Source: {} Topic: {} Type: {}  Payload: {}", event.getSource(),
                    event.getTopic(), event.getType(), event.getPayload());
            ((TriggerHandlerCallback) localCallback).triggered(module, Map.of("event", event));
        }
    }

    @Override
    public boolean apply(Event event) {
        boolean eventMatches = false;
        if (event instanceof ChannelTriggeredEvent) {
            ChannelTriggeredEvent cte = (ChannelTriggeredEvent) event;
            if (channelUID.equals(cte.getChannel())) {
                String eventOnChannel = this.eventOnChannel;
                logger.trace("->FILTER: {}:{}", cte.getEvent(), eventOnChannel);
                eventMatches = eventOnChannel == null || eventOnChannel.isBlank()
                        || eventOnChannel.equals(cte.getEvent());
            }
        }
        return eventMatches;
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return this;
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return types;
    }

    @Override
    public void dispose() {
        super.dispose();
        eventSubscriberRegistration.unregister();
    }
}
