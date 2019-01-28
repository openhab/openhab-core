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

import org.eclipse.smarthome.core.events.AbstractEvent;

/**
 * This is an {@link Event} that is sent on extension operations, such as installing and uninstalling.
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
public class ExtensionEvent extends AbstractEvent {

    /**
     * The extension event type.
     */
    public static final String TYPE = ExtensionEvent.class.getSimpleName();

    private String msg;
    private String id;

    /**
     * Constructs a new extension event object.
     *
     * @param topic the topic
     * @param payload the payload
     * @param id the id of the extension
     * @param msg the message text
     */
    public ExtensionEvent(String topic, String payload, String id, String msg) {
        super(topic, payload, null);
        this.id = id;
        this.msg = msg;
    }

    /**
     * Constructs a new extension event object.
     *
     * @param topic the topic
     * @param payload the payload
     * @param id the id of the extension
     */
    public ExtensionEvent(String topic, String payload, String id) {
        this(topic, payload, id, null);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        if (getTopic()
                .equals(ExtensionEventFactory.buildTopic(ExtensionEventFactory.EXTENSION_INSTALLED_EVENT_TOPIC, id))) {
            return "Extension '" + id + "' has been installed.";
        } else if (getTopic().equals(
                ExtensionEventFactory.buildTopic(ExtensionEventFactory.EXTENSION_UNINSTALLED_EVENT_TOPIC, id))) {
            return "Extension '" + id + "' has been uninstalled.";
        } else {
            return id + ": " + (msg != null ? msg : "Operation failed.");
        }
    }
}
