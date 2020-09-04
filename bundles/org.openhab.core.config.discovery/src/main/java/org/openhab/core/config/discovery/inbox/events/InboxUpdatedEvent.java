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
 * An {@link InboxUpdatedEvent} notifies subscribers that a discovery result has been updated in the inbox.
 * Inbox updated events must be created with the {@link InboxEventFactory}.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
@NonNullByDefault
public class InboxUpdatedEvent extends AbstractInboxEvent {

    /**
     * The inbox updated event type.
     */
    public static final String TYPE = InboxUpdatedEvent.class.getSimpleName();

    /**
     * Constructs a new inbox updated event object.
     *
     * @param topic the topic
     * @param payload the payload
     * @param discoveryResult the discovery-result data transfer object
     */
    protected InboxUpdatedEvent(String topic, String payload, DiscoveryResultDTO discoveryResult) {
        super(topic, payload, discoveryResult);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "Discovery Result with UID '" + getDiscoveryResult().thingUID + "' has been updated.";
    }
}
