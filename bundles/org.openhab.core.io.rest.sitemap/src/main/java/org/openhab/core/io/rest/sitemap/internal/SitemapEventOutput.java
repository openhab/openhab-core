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
package org.openhab.core.io.rest.sitemap.internal;

import java.io.IOException;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.openhab.core.io.rest.sitemap.SitemapSubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link EventOutput} implementation that takes a subscription id parameter and only writes out events that match the
 * page of this subscription.
 * Should only be used when the {@link OutboundEvent}s sent through this {@link EventOutput} contain a data object of
 * type {@link SitemapEvent}
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class SitemapEventOutput extends EventOutput {

    private final Logger logger = LoggerFactory.getLogger(SitemapEventOutput.class);

    private final String subscriptionId;
    private final SitemapSubscriptionService subscriptions;

    public SitemapEventOutput(SitemapSubscriptionService subscriptions, String subscriptionId) {
        super();
        this.subscriptions = subscriptions;
        this.subscriptionId = subscriptionId;
    }

    @Override
    public void write(OutboundEvent chunk) throws IOException {
        if (chunk.getName().equals("subscriptionId") && chunk.getData().equals(subscriptionId)) {
            super.write(chunk);
        } else {
            SitemapEvent event = (SitemapEvent) chunk.getData();
            String sitemapName = event.sitemapName;
            String pageId = event.pageId;
            if (sitemapName != null && sitemapName.equals(subscriptions.getSitemapName(subscriptionId))
                    && pageId != null && pageId.equals(subscriptions.getPageId(subscriptionId))) {
                super.write(chunk);
                if (logger.isDebugEnabled()) {
                    if (event instanceof SitemapWidgetEvent) {
                        logger.debug("Sent sitemap event for widget {} to subscription {}.",
                                ((SitemapWidgetEvent) event).widgetId, subscriptionId);
                    } else if (event instanceof ServerAliveEvent) {
                        logger.debug("Sent alive event to subscription {}.", subscriptionId);
                    }
                }
            }
        }
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }
}
