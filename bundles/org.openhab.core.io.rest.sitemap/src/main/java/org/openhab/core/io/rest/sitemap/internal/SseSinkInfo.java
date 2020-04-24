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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.rest.sitemap.SitemapSubscriptionService;

/**
 * The specific information we need to hold for a SSE sink.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class SseSinkInfo {

    public final String subscriptionId;
    public final SitemapSubscriptionService subscriptions;

    public SseSinkInfo(String subscriptionId, SitemapSubscriptionService subscriptions) {
        this.subscriptionId = subscriptionId;
        this.subscriptions = subscriptions;
    }
}
