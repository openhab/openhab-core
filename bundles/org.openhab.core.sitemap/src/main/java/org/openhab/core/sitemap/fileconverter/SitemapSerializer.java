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
package org.openhab.core.sitemap.fileconverter;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.converter.ObjectSerializer;
import org.openhab.core.sitemap.Sitemap;

/**
 * {@link SitemapSerializer} is the interface to implement by any file generator for {@link Sitemap} object.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface SitemapSerializer extends ObjectSerializer<Sitemap> {

    /**
     * Specify the {@link List} of {@link Sitemap}s to serialize and associate them with an identifier.
     *
     * id the identifier of the {@link Sitemap} format generation.
     *
     * @param id the identifier of the {@link Sitemap} format generation.
     * @param sitemaps the {@link List} of {@link Sitemap}s to serialize.
     */
    void setSitemapsToBeSerialized(String id, List<Sitemap> sitemaps);
}
