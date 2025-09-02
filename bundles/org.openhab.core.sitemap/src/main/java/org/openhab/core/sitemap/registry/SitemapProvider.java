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

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.sitemap.Sitemap;

/**
 * {@link SitemapProvider} should be implemented by any service that provides {@link Sitemap}s to a
 * {@link SitemapRegistry}.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface SitemapProvider extends Provider<Sitemap> {

    /**
     * Get a sitemap from the provider.
     *
     * @param sitemapName
     * @return sitemap
     */
    @Nullable
    Sitemap getSitemap(String sitemapName);

    /**
     * Get the names of all sitemaps available from the provider
     *
     * @return sitemap names
     */
    Set<String> getSitemapNames();
}
