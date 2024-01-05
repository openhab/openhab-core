/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.openhab.core.io.rest.core.item.EnrichedItemDTO;

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
 */
public class WidgetDTO {

    public String widgetId;
    public String type;
    public String name;
    public boolean visibility;

    public String label;
    public String labelSource;
    public String icon;
    /**
     * staticIcon is a boolean indicating if the widget state must be ignored when requesting the icon.
     * It is set to true when the widget has either the staticIcon property set or the icon property set
     * with conditional rules.
     */
    public Boolean staticIcon;
    public String labelcolor;
    public String valuecolor;
    public String iconcolor;

    public String pattern;
    public String unit;

    // widget-specific attributes
    public final List<MappingDTO> mappings = new ArrayList<>();
    public Boolean switchSupport;
    public Integer sendFrequency;
    public Integer refresh;
    public Integer height;
    public BigDecimal minValue;
    public BigDecimal maxValue;
    public BigDecimal step;
    public String inputHint;
    public String url;
    public String encoding;
    public String service;
    public String period;
    public String yAxisDecimalPattern;
    public Boolean legend;
    public Boolean forceAsItem;
    public String state;

    public EnrichedItemDTO item;
    public PageDTO linkedPage;

    // only for frames, other linkable widgets link to a page
    public final List<WidgetDTO> widgets = new ArrayList<>();

    public WidgetDTO() {
    }
}
