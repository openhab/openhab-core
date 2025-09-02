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
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.sitemap.ButtonDefinition;
import org.openhab.core.sitemap.Condition;
import org.openhab.core.sitemap.Mapping;
import org.openhab.core.sitemap.Parent;
import org.openhab.core.sitemap.Rule;
import org.openhab.core.sitemap.Sitemap;
import org.openhab.core.sitemap.Widget;

/**
 * The {@link SitemapFactory} is used to create {@link Sitemap}s and {@link Widget}s from their type string.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface SitemapFactory {

    /**
     * Creates a new {@link Sitemap} instance with name <code>sitemapName</code>
     *
     * @param sitemapName
     * @return a new Sitemap.
     */
    Sitemap createSitemap(String sitemapName);

    /**
     * Creates a new {@link Widget} instance of type <code>widgetTypeName</code>
     *
     * @param widgetTypeName
     * @return a new Widget of type <code>widgetTypeName</code> or <code>null</code> if no matching class is known.
     */
    @Nullable
    Widget createWidget(String widgetTypeName);

    /**
     * Creates a new {@link Widget} instance of type <code>widgetTypeName</code> and with {@link Parent}
     * <code>parent</code>
     *
     * @param widgetTypeName
     * @param parent
     * @return a new Widget of type <code>widgetTypeName</code> or <code>null</code> if no matching class is known.
     */
    @Nullable
    Widget createWidget(String widgetTypeName, Parent parent);

    /**
     * Creates a {@link ButtonDefinition} instance
     *
     * @return a new ButtonDefinition.
     */
    ButtonDefinition createButtonDefinition();

    /**
     * Creates a {@link Mapping} instance
     *
     * @return a new Mapping.
     */
    Mapping createMapping();

    /**
     * Creates a {@link Rule} instance
     *
     * @return a new Rule.
     */
    Rule createRule();

    /**
     * Creates a {@link Rule} {@link Condition} instance
     *
     * @return a new Rule Condition.
     */
    Condition createCondition();

    /**
     * Returns the list of all supported WidgetTypes of this Factory.
     *
     * @return the supported WidgetTypes
     */
    String[] getSupportedWidgetTypes();
}
