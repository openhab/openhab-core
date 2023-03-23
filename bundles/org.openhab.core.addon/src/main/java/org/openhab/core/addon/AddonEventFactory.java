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
package org.openhab.core.addon;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.AbstractEventFactory;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFactory;
import org.osgi.service.component.annotations.Component;

/**
 * This is an {@link EventFactory} for creating add-on events. The following event types are supported by this
 * factory:
 *
 * {@link AddonEventFactory#TYPE}
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component(service = EventFactory.class, immediate = true)
@NonNullByDefault
public class AddonEventFactory extends AbstractEventFactory {

    static final String TOPIC_PREFIX = "openhab/addons/{id}";

    static final String ADDON_INSTALLED_EVENT_TOPIC_POSTFIX = "/installed";
    static final String ADDON_UNINSTALLED_EVENT_TOPIC_POSTFIX = "/uninstalled";
    static final String ADDON_FAILURE_EVENT_TOPIC_POSTFIX = "/failed";

    static final String ADDON_INSTALLED_EVENT_TOPIC = TOPIC_PREFIX + ADDON_INSTALLED_EVENT_TOPIC_POSTFIX;
    static final String ADDON_UNINSTALLED_EVENT_TOPIC = TOPIC_PREFIX + ADDON_UNINSTALLED_EVENT_TOPIC_POSTFIX;
    static final String ADDON_FAILURE_EVENT_TOPIC = TOPIC_PREFIX + ADDON_FAILURE_EVENT_TOPIC_POSTFIX;

    /**
     * Constructs a new AddonEventFactory.
     */
    public AddonEventFactory() {
        super(Set.of(AddonEvent.TYPE));
    }

    @Override
    protected Event createEventByType(String eventType, String topic, String payload, @Nullable String source)
            throws Exception {
        if (topic.endsWith(ADDON_FAILURE_EVENT_TOPIC_POSTFIX)) {
            String[] properties = deserializePayload(payload, String[].class);
            Event event = new AddonEvent(topic, payload, properties[0], properties[1]);
            return event;
        } else {
            String id = deserializePayload(payload, String.class);
            Event event = new AddonEvent(topic, payload, id);
            return event;
        }
    }

    /**
     * Creates an "add-on installed" event.
     *
     * @param id the id of the installed add-on
     * @return the according event
     */
    public static AddonEvent createAddonInstalledEvent(String id) {
        String topic = buildTopic(ADDON_INSTALLED_EVENT_TOPIC, id);
        String payload = serializePayload(id);
        return new AddonEvent(topic, payload, id);
    }

    /**
     * Creates an "add-on uninstalled" event.
     *
     * @param id the id of the uninstalled add-on
     * @return the according event
     */
    public static AddonEvent createAddonUninstalledEvent(String id) {
        String topic = buildTopic(ADDON_UNINSTALLED_EVENT_TOPIC, id);
        String payload = serializePayload(id);
        return new AddonEvent(topic, payload, id);
    }

    /**
     * Creates an "add-on failure" event.
     *
     * @param id the id of the add-on that caused a failure
     * @param msg the message text of the failure
     * @return the according event
     */
    public static AddonEvent createAddonFailureEvent(String id, @Nullable String msg) {
        String topic = buildTopic(ADDON_FAILURE_EVENT_TOPIC, id);
        String[] properties = new String[] { id, msg };
        String payload = serializePayload(properties);
        return new AddonEvent(topic, payload, id, msg);
    }

    static String buildTopic(String topic, String id) {
        return topic.replace("{id}", id);
    }
}
