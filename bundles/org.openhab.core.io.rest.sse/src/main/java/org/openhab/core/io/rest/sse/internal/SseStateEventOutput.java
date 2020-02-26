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
package org.openhab.core.io.rest.sse.internal;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;

/**
 * {@link EventOutput} implementation that takes a list of item names writes out state change events that match this
 * list. Should only be used when the {@link OutboundEvent}s sent through this {@link EventOutput} contain a map of item
 * names and states.
 *
 * It also uniquely identifies the connection with a random identifier.
 *
 * @author Yannick Schaus - Initial contribution
 *
 */
public class SseStateEventOutput extends EventOutput {

    private String connectionId;
    private Collection<String> trackedItems = Collections.emptySet();

    public SseStateEventOutput() {
        super();
        this.connectionId = UUID.randomUUID().toString();
    }

    /**
     * Gets the connection identifier of this {@link SseStateEventOutput}
     *
     * @return the connection id
     */
    public String getConnectionId() {
        return connectionId;
    }

    /**
     * Updates the list of tracked items for this connection
     *
     * @param itemNames the item names to track
     */
    public void setTrackedItems(Collection<String> itemNames) {
        this.trackedItems = itemNames;
    }

    /**
     * Gets the list of tracked items for this connection
     *
     * @return a list of tracked item names
     */
    protected Collection<String> getTrackedItems() {
        return trackedItems;
    }

    @Override
    public void write(OutboundEvent chunk) throws IOException {
        if (chunk.getData() instanceof Map<?, ?>) {
            Map<?, ?> event = (Map<?, ?>) chunk.getData();
            String itemName = event.keySet().iterator().next().toString();

            if (trackedItems != null && trackedItems.contains(itemName)) {
                super.write(chunk);
            }
        }
    }

    /**
     * Writes an event without filtering
     *
     * @param chunk
     * @throws IOException
     */
    public void writeDirect(OutboundEvent chunk) throws IOException {
        super.write(chunk);
    }
}
