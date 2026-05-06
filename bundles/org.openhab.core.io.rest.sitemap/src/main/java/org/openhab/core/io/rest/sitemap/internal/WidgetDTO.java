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

import java.util.ArrayList;
import java.util.List;

import org.openhab.core.io.rest.core.item.EnrichedItemDTO;
import org.openhab.core.sitemap.dto.AbstractWidgetDTO;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * This is a data transfer object that is used to serialize widgets.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Chris Jackson - Initial contribution
 * @author Laurent Garnier - New field iconcolor
 * @author Mark herwege - New fields pattern, unit
 * @author Laurent Garnier - New field columns
 * @author Danny Baumann - New field labelSource
 * @author Laurent Garnier - Remove field columns
 * @author Laurent Garnier - New fields row, column, command, releaseCommand and stateless for Button element
 * @author Mark Herwege - Extends abstract widget DTO
 */
@Schema(name = "SitemapWidget")
public class WidgetDTO extends AbstractWidgetDTO {

    public String widgetId;
    public boolean visibility;
    public String labelSource;

    public String labelcolor;
    public String valuecolor;
    public String iconcolor;

    public String pattern;
    public String unit;

    public String state;

    public EnrichedItemDTO item;
    public PageDTO linkedPage;

    // only for frames and button grids, other linkable widgets link to a page
    public final List<WidgetDTO> widgets = new ArrayList<>();

    public WidgetDTO() {
    }
}
