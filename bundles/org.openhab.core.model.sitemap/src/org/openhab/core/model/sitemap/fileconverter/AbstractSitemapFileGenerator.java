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
package org.openhab.core.model.sitemap.fileconverter;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.model.sitemap.sitemap.Sitemap;
import org.osgi.service.component.annotations.Activate;

/**
 * {@link AbstractSitemapFileGenerator} is the base class for any {@link Sitemap} file generator.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractSitemapFileGenerator implements SitemapFileGenerator {

    @Activate
    public AbstractSitemapFileGenerator() {
    }
}
