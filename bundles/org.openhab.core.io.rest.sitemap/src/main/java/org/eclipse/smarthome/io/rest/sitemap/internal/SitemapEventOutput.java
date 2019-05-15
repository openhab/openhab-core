/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.io.rest.sitemap.internal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventSink;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.io.rest.sitemap.SitemapSubscriptionService;
import org.eclipse.smarthome.io.rest.sitemap.internal.events.ServerAliveEvent;
import org.eclipse.smarthome.io.rest.sitemap.internal.events.SitemapEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link EventOutput} implementation that takes a subscription id parameter and only writes out events that match the
 * page of this subscription.
 * Should only be used when the {@link OutboundSseEvent}s sent through this {@link SseEventSink} contain a data object
 * of type {@link SitemapEvent}.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
@NonNullByDefault
public class SitemapEventOutput implements SseEventSink {

    private final Logger logger = LoggerFactory.getLogger(SitemapEventOutput.class);

    private final String subscriptionId;
    private final SitemapSubscriptionService subscriptions;
    private SseEventSink proxy;

    public SitemapEventOutput(SseEventSink sseEventSink, SitemapSubscriptionService subscriptions,
            String subscriptionId) {
        this.proxy = sseEventSink;
        this.subscriptions = subscriptions;
        this.subscriptionId = subscriptionId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    @Override
    public boolean isClosed() {
        return proxy.isClosed();
    }

    @Override
    public CompletionStage<?> send(@NonNullByDefault({}) OutboundSseEvent event) {
        if (event.getName().equals("subscriptionId") && event.getData().equals(subscriptionId)) {
            return proxy.send(event);
        } else {
            SitemapEvent sitemapEvent = (SitemapEvent) event.getData();
            String sitemapName = sitemapEvent.sitemapName;
            String pageId = sitemapEvent.pageId;
            if (sitemapName != null && sitemapName.equals(subscriptions.getSitemapName(subscriptionId))
                    && pageId != null && pageId.equals(subscriptions.getPageId(subscriptionId))) {
                if (logger.isDebugEnabled()) {
                    if (sitemapEvent instanceof SitemapWidgetEvent) {
                        logger.debug("Sent sitemap event for widget {} to subscription {}.",
                                ((SitemapWidgetEvent) sitemapEvent).widgetId, subscriptionId);
                    } else if (sitemapEvent instanceof ServerAliveEvent) {
                        logger.debug("Sent alive event to subscription {}.", subscriptionId);
                    }
                }
                return proxy.send(event);
            }
        }
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public void close() {
        proxy.close();
    }
}
