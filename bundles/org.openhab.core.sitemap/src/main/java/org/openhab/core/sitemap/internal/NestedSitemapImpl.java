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
package org.openhab.core.sitemap.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.sitemap.NestedSitemap;
import org.openhab.core.sitemap.Parent;

/**
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class NestedSitemapImpl extends NonLinkableWidgetImpl implements NestedSitemap {

    private @Nullable String sitemapName;

    public NestedSitemapImpl() {
    }

    public NestedSitemapImpl(Parent parent) {
        super(parent);
    }

    @Override
    public @Nullable String getSitemapName() {
        return sitemapName;
    }

    @Override
    public void setSitemapName(@Nullable String sitemapName) {
        this.sitemapName = sitemapName;
    }

    @Override
    public String getWidgetType() {
        return "Sitemap";
    }
}
