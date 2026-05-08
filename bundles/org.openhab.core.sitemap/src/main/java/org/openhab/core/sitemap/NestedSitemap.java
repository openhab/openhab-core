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
package org.openhab.core.sitemap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A representation of a sitemap NestedSitemap widget.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface NestedSitemap extends NonLinkableWidget {

    /**
     * Get the name of the nested sitemap.
     *
     * @return name of nested sitemap
     */
    @Nullable
    String getSitemapName();

    /**
     * Set the name of the nested sitemap. This is used to link to another sitemap, which will be rendered when the user
     * clicks on this widget.
     *
     * @param sitemapName the name of the nested sitemap
     */
    void setSitemapName(String sitemapName);
}
