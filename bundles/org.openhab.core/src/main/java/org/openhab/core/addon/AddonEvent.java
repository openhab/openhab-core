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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.AbstractEvent;

/**
 * This is an {@link Event} that is sent on add-on operations, such as installing and uninstalling.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class AddonEvent extends AbstractEvent {

    /**
     * The add-on event type.
     */
    public static final String TYPE = AddonEvent.class.getSimpleName();

    private @Nullable String msg;
    private String id;

    /**
     * Constructs a new add-on event object.
     *
     * @param topic the topic
     * @param payload the payload
     * @param id the id of the add-on
     * @param msg the message text
     */
    public AddonEvent(String topic, String payload, String id, @Nullable String msg) {
        super(topic, payload, null);
        this.id = id;
        this.msg = msg;
    }

    /**
     * Constructs a new add-on event object.
     *
     * @param topic the topic
     * @param payload the payload
     * @param id the id of the add-on
     */
    public AddonEvent(String topic, String payload, String id) {
        this(topic, payload, id, null);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        if (getTopic().equals(AddonEventFactory.buildTopic(AddonEventFactory.ADDON_INSTALLED_EVENT_TOPIC, id))) {
            return "Add-on '" + id + "' has been installed.";
        } else if (getTopic()
                .equals(AddonEventFactory.buildTopic(AddonEventFactory.ADDON_UNINSTALLED_EVENT_TOPIC, id))) {
            return "Add-on '" + id + "' has been uninstalled.";
        } else {
            return id + ": " + (msg != null ? msg : "Operation failed.");
        }
    }
}
