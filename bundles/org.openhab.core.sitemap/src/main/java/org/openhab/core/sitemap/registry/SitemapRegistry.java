/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.sitemap.registry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.common.registry.Registry;
import org.openhab.core.sitemap.Sitemap;

/**
 * The {@link SitemapRegistry} is the central place to store sitemaps.
 * Sitemaps are registered through {@link SitemapProvider}.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface SitemapRegistry extends Registry<Sitemap, String> {

    /**
     * Add a sitemap provider to the registry.
     *
     * @param provider
     */
    public void addSitemapProvider(Provider<Sitemap> provider);

    /**
     * Remove a sitemap provider from the registry.
     *
     * @param provider
     */
    public void removeSitemapProvider(Provider<Sitemap> provider);
}
