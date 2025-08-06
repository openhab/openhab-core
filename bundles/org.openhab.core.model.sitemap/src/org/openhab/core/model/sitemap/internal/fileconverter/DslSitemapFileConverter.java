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
package org.openhab.core.model.sitemap.internal.fileconverter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.sitemap.fileconverter.AbstractSitemapFileGenerator;
import org.openhab.core.model.sitemap.fileconverter.SitemapFileGenerator;
import org.openhab.core.model.sitemap.sitemap.Sitemap;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DslSitemapFileConverter} is the DSL file converter for {@link Sitemap} object
 * with the capabilities of parsing and generating file.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = SitemapFileGenerator.class)
public class DslSitemapFileConverter extends AbstractSitemapFileGenerator {

    private final Logger logger = LoggerFactory.getLogger(DslSitemapFileConverter.class);

    private final ModelRepository modelRepository;

    @Activate
    public DslSitemapFileConverter(final @Reference ModelRepository modelRepository) {
        this.modelRepository = modelRepository;
    }

    @Override
    public String getFileFormatGenerator() {
        return "DSL";
    }

    @Override
    public synchronized void generateFileFormat(OutputStream out, List<Sitemap> sitemaps) {
        if (sitemaps.isEmpty()) {
            return;
        }
        for (Sitemap sitemap : sitemaps) {
            modelRepository.generateSyntaxFromModel(out, "sitemap", sitemap);
            try {
                out.write(System.lineSeparator().getBytes());
            } catch (IOException e) {
                logger.warn("Exception when saving the sitemap {}", sitemap.getName());
            }
        }
    }
}
