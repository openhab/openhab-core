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
package org.openhab.core.sitemap.internal;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.sitemap.Sitemap;
import org.openhab.core.sitemap.Widget;

/**
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class SitemapImpl implements Sitemap {

    private String name = "";
    private @Nullable String label;
    private @Nullable String icon;
    private List<Widget> widgets = new CopyOnWriteArrayList<>();

    public SitemapImpl() {
    }

    public SitemapImpl(String name) {
        this.name = name;
    }

    @Override
    public String getUID() {
        return name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public @Nullable String getLabel() {
        return label;
    }

    @Override
    public void setLabel(@Nullable String label) {
        this.label = label;
    }

    @Override
    public @Nullable String getIcon() {
        return icon;
    }

    @Override
    public void setIcon(@Nullable String icon) {
        this.icon = icon;
    }

    @Override
    public List<Widget> getWidgets() {
        return widgets;
    }

    @Override
    public void setWidgets(List<Widget> widgets) {
        this.widgets = new CopyOnWriteArrayList<>(widgets);
    }
}
