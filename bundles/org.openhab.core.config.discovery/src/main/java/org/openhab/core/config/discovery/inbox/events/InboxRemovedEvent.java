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
package org.openhab.core.config.discovery.inbox.events;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.discovery.dto.DiscoveryResultDTO;

/**
 * An {@link InboxRemovedEvent} notifies subscribers that a discovery result has been removed from the inbox.
 * Inbox removed events must be created with the {@link InboxEventFactory}.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
@NonNullByDefault
public class InboxRemovedEvent extends AbstractInboxEvent {

    /**
     * The inbox removed event type.
     */
    public static final String TYPE = InboxRemovedEvent.class.getSimpleName();

    /**
     * Constructs a new inbox removed event object.
     *
     * @param topic the topic
     * @param payload the payload
     * @param discoveryResult the discovery result data transfer object
     */
    InboxRemovedEvent(String topic, String payload, DiscoveryResultDTO discoveryResult) {
        super(topic, payload, discoveryResult);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "Discovery Result with UID '" + getDiscoveryResult().thingUID + "' has been removed.";
    }
}
