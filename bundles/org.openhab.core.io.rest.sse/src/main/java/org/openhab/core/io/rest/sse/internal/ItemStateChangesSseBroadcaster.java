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
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import org.glassfish.jersey.server.ChunkedOutput;
import org.openhab.core.items.ItemRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link SseBroadcaster} keeps track of the {@link SseStateEventOutput} listeners to state changes and tracks them
 * by their connectionId.
 *
 * @author Yannick Schaus - initial contribution
 */
public class ItemStateChangesSseBroadcaster extends SseBroadcaster {

    private final Logger logger = LoggerFactory.getLogger(ItemStateChangesSseBroadcaster.class);

    private Map<String, SseStateEventOutput> eventOutputs = new HashMap<>();

    private ItemRegistry itemRegistry;

    public ItemStateChangesSseBroadcaster(ItemRegistry itemRegistry) {
        super();
        this.itemRegistry = itemRegistry;
    }

    @Override
    public <OUT extends ChunkedOutput<OutboundEvent>> boolean add(OUT chunkedOutput) {
        if (chunkedOutput instanceof SseStateEventOutput) {
            SseStateEventOutput eventOutput = (SseStateEventOutput) chunkedOutput;
            OutboundEvent.Builder builder = new OutboundEvent.Builder();
            String connectionId = eventOutput.getConnectionId();
            try {
                eventOutputs.put(connectionId, eventOutput);
                eventOutput.writeDirect(builder.id("0").name("ready").data(connectionId).build());
            } catch (IOException e) {
                logger.error("Cannot write initial ready event to {}, discarding connection", connectionId);
                return false;
            }
        }

        return super.add(chunkedOutput);
    }

    @Override
    public <OUT extends ChunkedOutput<OutboundEvent>> boolean remove(OUT chunkedOutput) {
        eventOutputs.values().remove(chunkedOutput);
        return super.remove(chunkedOutput);
    }

    @Override
    public void onClose(ChunkedOutput<OutboundEvent> chunkedOutput) {
        remove(chunkedOutput);
    }

    @Override
    public void onException(ChunkedOutput<OutboundEvent> chunkedOutput, Exception exception) {
        remove(chunkedOutput);
    }

    /**
     * Updates the list of tracked items for a connection
     *
     * @param connectionId the connection id
     * @param newTrackedItems the list of items and their current state to send to the client
     */
    public void updateTrackedItems(String connectionId, Map<String, String> newTrackedItems) {
        SseStateEventOutput eventOutput = eventOutputs.get(connectionId);

        if (eventOutput == null) {
            throw new IllegalArgumentException("ConnectionId not found");
        }

        eventOutput.setTrackedItems(newTrackedItems.keySet());

        try {
            if (!eventOutput.isClosed()) {
                OutboundEvent.Builder builder = new OutboundEvent.Builder();
                OutboundEvent event = builder.mediaType(MediaType.APPLICATION_JSON_TYPE).data(newTrackedItems).build();
                eventOutput.writeDirect(event);
            }
            if (eventOutput.isClosed()) {
                onClose(eventOutput);
            }
        } catch (IOException e) {
            onException(eventOutput, e);
        }
    }
}
