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

import org.openhab.core.sitemap.dto.SitemapDefinitionDTO;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * This is a data transfer object that is used to serialize sitemaps to represent or edit in the UI.
 *
 * @author Mark Herwege - Initial contribution
 */
@Schema(name = "EnrichedSitemapDefinition")
public class EnrichedSitemapDefinitionDTO extends SitemapDefinitionDTO {

    public boolean editable;

    public EnrichedSitemapDefinitionDTO(SitemapDefinitionDTO dto) {
        this.name = dto.name;
        this.label = dto.label;
        this.icon = dto.icon;
        this.widgets = dto.widgets;
    }
}
