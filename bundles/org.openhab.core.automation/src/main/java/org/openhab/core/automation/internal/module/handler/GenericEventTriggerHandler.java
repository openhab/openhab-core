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

import java.util.Arrays;
import java.util.Collections;
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
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an ModuleHandler implementation for Triggers which trigger the rule
 * if an event occurs. The eventType, eventSource and topic can be set with the
 * configuration. It is an generic approach which makes it easier to specify
 * more concrete event based triggers with the composite module approach of the
 * automation component. Each GenericTriggerHandler instance registers as
 * EventSubscriber, so the dispose method must be called for unregistering the
 * service.
 *
 * @author Benedikt Niehues - Initial contribution
 * @author Kai Kreuzer - refactored and simplified customized module handling
 */
public class GenericEventTriggerHandler extends BaseTriggerModuleHandler implements EventSubscriber, EventFilter {

    public static final String MODULE_TYPE_ID = "core.GenericEventTrigger";

    private static final String CFG_EVENT_TOPIC = "eventTopic";
    private static final String CFG_EVENT_SOURCE = "eventSource";
    private static final String CFG_EVENT_TYPES = "eventTypes";

    private final Logger logger = LoggerFactory.getLogger(GenericEventTriggerHandler.class);

    private final String source;
    private String topic;
    private final Set<String> types;
    private final BundleContext bundleContext;

    private ServiceRegistration<?> eventSubscriberRegistration;

    public GenericEventTriggerHandler(Trigger module, BundleContext bundleContext) {
        super(module);
        this.source = (String) module.getConfiguration().get(CFG_EVENT_SOURCE);
        this.topic = (String) module.getConfiguration().get(CFG_EVENT_TOPIC);
        if (module.getConfiguration().get(CFG_EVENT_TYPES) != null) {
            this.types = Collections.unmodifiableSet(
                    new HashSet<>(Arrays.asList(((String) module.getConfiguration().get(CFG_EVENT_TYPES)).split(","))));
        } else {
            this.types = Collections.emptySet();
        }
        this.bundleContext = bundleContext;
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("event.topics", topic);
        eventSubscriberRegistration = this.bundleContext.registerService(EventSubscriber.class.getName(), this,
                properties);
        logger.trace("Registered EventSubscriber: Topic: {} Type: {} Source: {}", topic, types, source);
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
            if (!event.getTopic().contains(source)) {
                return;
            }
            Map<String, Object> values = new HashMap<>();
            values.put("event", event);

            ((TriggerHandlerCallback) callback).triggered(this.module, values);
        }
    }

    /**
     * @return the topic
     */
    public String getTopic() {
        return topic;
    }

    /**
     * @param topic
     *            the topic to set
     */
    public void setTopic(String topic) {
        this.topic = topic;
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
        logger.trace("->FILTER: {}:{}", event.getTopic(), source);
        return event.getTopic().contains(source);
    }
}
