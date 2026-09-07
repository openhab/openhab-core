/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A general sitemap event, meant to be sub-classed.
 *
 * <p>
 * The three concrete event types are streamed on the same SSE endpoints as a JSON-encoded {@code data:} line each. They
 * are declared here as a {@code oneOf} without a discriminator: {@link SitemapWidgetEvent} always carries a
 * {@code widgetId}, while {@link SitemapChangedEvent} and {@link ServerAliveEvent} carry a distinct constant
 * {@code TYPE}, which makes the three branches mutually exclusive for generated clients without changing the wire
 * format.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Schema(oneOf = { SitemapWidgetEvent.class, SitemapChangedEvent.class, ServerAliveEvent.class })
public class SitemapEvent {

    /** The sitemap name this event is for */
    public String sitemapName;

    /** The page id this event is for */
    public String pageId;

    public SitemapEvent() {
    }
}
