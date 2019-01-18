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
package org.eclipse.smarthome.core.extension;

import java.util.Collections;

import org.eclipse.smarthome.core.events.AbstractEventFactory;
import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventFactory;
import org.osgi.service.component.annotations.Component;

/**
 * This is an {@link EventFactory} for creating extension events. The following event types are supported by this
 * factory:
 *
 * {@link ExtensionEventFactory#TYPE}
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
@Component(service = EventFactory.class, immediate = true)
public class ExtensionEventFactory extends AbstractEventFactory {

    static final String TOPIC_PREFIX = "smarthome/extensions/{id}";

    static final String EXTENSION_INSTALLED_EVENT_TOPIC_POSTFIX = "/installed";
    static final String EXTENSION_UNINSTALLED_EVENT_TOPIC_POSTFIX = "/uninstalled";
    static final String EXTENSION_FAILURE_EVENT_TOPIC_POSTFIX = "/failed";

    static final String EXTENSION_INSTALLED_EVENT_TOPIC = TOPIC_PREFIX + EXTENSION_INSTALLED_EVENT_TOPIC_POSTFIX;
    static final String EXTENSION_UNINSTALLED_EVENT_TOPIC = TOPIC_PREFIX + EXTENSION_UNINSTALLED_EVENT_TOPIC_POSTFIX;
    static final String EXTENSION_FAILURE_EVENT_TOPIC = TOPIC_PREFIX + EXTENSION_FAILURE_EVENT_TOPIC_POSTFIX;

    /**
     * Constructs a new ExtensionEventFactory.
     */
    public ExtensionEventFactory() {
        super(Collections.singleton(ExtensionEvent.TYPE));
    }

    @Override
    protected Event createEventByType(String eventType, String topic, String payload, String source) throws Exception {
        if (topic.endsWith(EXTENSION_FAILURE_EVENT_TOPIC_POSTFIX)) {
            String[] properties = deserializePayload(payload, String[].class);
            Event event = new ExtensionEvent(topic, payload, properties[0], properties[1]);
            return event;
        } else {
            String id = deserializePayload(payload, String.class);
            Event event = new ExtensionEvent(topic, payload, id);
            return event;
        }
    }

    /**
     * Creates an "extension installed" event.
     *
     * @param id the id of the installed extension
     * @return the according event
     */
    public static ExtensionEvent createExtensionInstalledEvent(String id) {
        String topic = buildTopic(EXTENSION_INSTALLED_EVENT_TOPIC, id);
        String payload = serializePayload(id);
        return new ExtensionEvent(topic, payload, id);
    }

    /**
     * Creates an "extension uninstalled" event.
     *
     * @param id the id of the uninstalled extension
     * @return the according event
     */
    public static ExtensionEvent createExtensionUninstalledEvent(String id) {
        String topic = buildTopic(EXTENSION_UNINSTALLED_EVENT_TOPIC, id);
        String payload = serializePayload(id);
        return new ExtensionEvent(topic, payload, id);
    }

    /**
     * Creates an "extension failure" event.
     *
     * @param id the id of the extension that caused a failure
     * @param msg the message text of the failure
     * @return the according event
     */
    public static ExtensionEvent createExtensionFailureEvent(String id, String msg) {
        String topic = buildTopic(EXTENSION_FAILURE_EVENT_TOPIC, id);
        String[] properties = new String[] { id, msg };
        String payload = serializePayload(properties);
        return new ExtensionEvent(topic, payload, id, msg);
    }

    static String buildTopic(String topic, String id) {
        return topic.replace("{id}", id);
    }

}
