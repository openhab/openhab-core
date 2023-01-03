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
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.ModuleHandlerCallback;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.handler.BaseTriggerModuleHandler;
import org.openhab.core.automation.handler.TriggerHandlerCallback;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.events.TopicGlobEventFilter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a ModuleHandler implementation for Triggers which trigger the rule
 * if an event occurs. The eventType, eventSource and topic can be set with the
 * configuration. It is a generic approach which makes it easier to specify
 * more concrete event based triggers with the composite module approach of the
 * automation component. Each GenericTriggerHandler instance registers as
 * EventSubscriber, so the dispose method must be called for unregistering the
 * service.
 *
 * @author Benedikt Niehues - Initial contribution
 * @author Kai Kreuzer - refactored and simplified customized module handling
 * @author Cody Cutrer - refactored to match configuration and semantics of GenericConditionTriggerHandler
 */
@NonNullByDefault
public class GenericEventTriggerHandler extends BaseTriggerModuleHandler implements EventSubscriber, EventFilter {

    public static final String MODULE_TYPE_ID = "core.GenericEventTrigger";

    public static final String CFG_TOPIC = "topic";
    public static final String CFG_SOURCE = "source";
    public static final String CFG_TYPES = "types";
    public static final String CFG_PAYLOAD = "payload";

    private final Logger logger = LoggerFactory.getLogger(GenericEventTriggerHandler.class);

    private final String source;
    private final @Nullable TopicGlobEventFilter topicFilter;
    private final Set<String> types;
    private final @Nullable Pattern payloadPattern;

    private @Nullable ServiceRegistration<?> eventSubscriberRegistration;

    public GenericEventTriggerHandler(Trigger module, BundleContext bundleContext) {
        super(module);
        this.source = (String) module.getConfiguration().get(CFG_SOURCE);
        String topic = (String) module.getConfiguration().get(CFG_TOPIC);
        if (!topic.isBlank()) {
            topicFilter = new TopicGlobEventFilter(topic);
        } else {
            topicFilter = null;
        }
        if (module.getConfiguration().get(CFG_TYPES) != null) {
            this.types = Set.of(((String) module.getConfiguration().get(CFG_TYPES)).split(","));
        } else {
            this.types = Set.of();
        }
        String payload = (String) module.getConfiguration().get(CFG_PAYLOAD);
        if (!payload.isBlank()) {
            payloadPattern = Pattern.compile(payload);
        } else {
            payloadPattern = null;
        }

        eventSubscriberRegistration = bundleContext.registerService(EventSubscriber.class.getName(), this, null);
        logger.trace("Registered EventSubscriber: Topic: {} Type: {} Source: {} Payload: {}", topic, types, source,
                payload);
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
        ModuleHandlerCallback callback = this.callback;
        if (callback != null) {
            logger.trace("Received Event: Source: {} Topic: {} Type: {}  Payload: {}", event.getSource(),
                    event.getTopic(), event.getType(), event.getPayload());
            if (!event.getTopic().contains(source)) {
                return;
            }
            Map<String, Object> values = new HashMap<>();
            values.put("event", event);

            ((TriggerHandlerCallback) callback).triggered(this.module, values);
        }
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
        logger.trace("->FILTER: {}: {}", event.getTopic(), source);

        TopicGlobEventFilter localTopicFilter = topicFilter;
        if (localTopicFilter != null && !topicFilter.apply(event)) {
            return false;
        }
        if (!source.isEmpty() && !source.equals(event.getSource())) {
            return false;
        }
        Pattern localPayloadPattern = payloadPattern;
        if (localPayloadPattern != null && !localPayloadPattern.matcher(event.getPayload()).find()) {
            return false;
        }

        return true;
    }
}
