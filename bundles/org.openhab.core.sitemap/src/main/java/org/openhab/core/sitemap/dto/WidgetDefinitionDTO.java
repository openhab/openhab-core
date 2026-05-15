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

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * This is a data transfer object that is used to serialize widgets.
 *
 * @author Mark Herwege - Initial contribution
 * @author Mark Herwege - Add support for nested sitemaps
 */
@Schema(name = "SitemapWidgetDefinition")
public class WidgetDefinitionDTO extends AbstractWidgetDTO {

    public String item;

    // Nested sitemaps are only supported in the definition, not in the instance. The instance replaces them with a Text
    // widget containing the sitemap definition as child widgets.
    public String name;

    public List<RuleDTO> visibilityRules;
    public List<RuleDTO> iconRules;
    public List<RuleDTO> labelColorRules;
    public List<RuleDTO> valueColorRules;
    public List<RuleDTO> iconColorRules;

    public List<WidgetDefinitionDTO> widgets;
}
