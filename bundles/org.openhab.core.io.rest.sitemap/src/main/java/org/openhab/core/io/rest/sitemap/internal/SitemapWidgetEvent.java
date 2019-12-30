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

import org.openhab.core.io.rest.core.item.EnrichedItemDTO;

/**
 * A sitemap event, which provides details about a widget that has changed.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class SitemapWidgetEvent extends SitemapEvent {

    public String widgetId;

    public String label;
    public String icon;
    public String labelcolor;
    public String valuecolor;
    public boolean visibility;
    public String state;
    public EnrichedItemDTO item;

    public SitemapWidgetEvent() {
    }
}
