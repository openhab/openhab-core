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

import org.openhab.core.config.discovery.dto.DiscoveryResultDTO;
import org.openhab.core.config.discovery.inbox.Inbox;
import org.openhab.core.events.AbstractEvent;

/**
 * Abstract implementation of an inbox event which will be posted by the {@link Inbox} for added, removed
 * and updated discovery results.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
public abstract class AbstractInboxEvent extends AbstractEvent {

    private final DiscoveryResultDTO discoveryResult;

    /**
     * Must be called in subclass constructor to create an inbox event.
     *
     * @param topic the topic
     * @param payload the payload
     * @param discoveryResult the discovery-result data transfer object
     */
    public AbstractInboxEvent(String topic, String payload, DiscoveryResultDTO discoveryResult) {
        super(topic, payload, null);
        this.discoveryResult = discoveryResult;
    }

    /**
     * Gets the discovery result as data transfer object.
     * 
     * @return the discoveryResult
     */
    public DiscoveryResultDTO getDiscoveryResult() {
        return discoveryResult;
    }

}
