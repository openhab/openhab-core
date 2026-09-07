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

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Event to notify the browser that the sitemap has been changed
 *
 * @author Stefan Triller - Initial contribution
 */
public class SitemapChangedEvent extends SitemapEvent {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY, allowableValues = {
            "SITEMAP_CHANGED" })
    public final String TYPE = "SITEMAP_CHANGED";
}
