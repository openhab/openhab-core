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
package org.openhab.core.sitemap.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * This is a data transfer object that is used to serialize widgets.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Chris Jackson - Initial contribution
 * @author Laurent Garnier - New field iconcolor
 * @author Mark Herwege - New fields pattern, unit
 * @author Laurent Garnier - New field columns
 * @author Danny Baumann - New field labelSource
 * @author Laurent Garnier - Remove field columns
 * @author Laurent Garnier - New fields row, column, command, releaseCommand and stateless for Button element
 * @author Mark Herwege - Created as abstract base class for WidgetDefinitionDTO classes
 */
public abstract class AbstractWidgetDTO {

    public String type;
    public String label;
    public String icon;
    /**
     * staticIcon is a boolean indicating if the widget state must be ignored when requesting the icon.
     * It is set to true when the widget has either the staticIcon property set or the icon property set
     * with conditional rules.
     */
    public Boolean staticIcon;

    // widget-specific attributes
    public List<MappingDTO> mappings;
    public Boolean switchSupport;
    public Boolean releaseOnly;
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
    public String interpolation;
    public Boolean legend;
    public Boolean forceAsItem;
    public Integer row;
    public Integer column;
    public String command;
    public String releaseCommand;
    public Boolean stateless;
    public String state;

    public AbstractWidgetDTO() {
    }
}
