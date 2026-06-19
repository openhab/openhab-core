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
package org.openhab.core.addon.marketplace.internal.community;

import static org.openhab.core.addon.marketplace.MarketplaceConstants.*;
import static org.openhab.core.addon.marketplace.internal.community.CommunityMarketplaceAddonService.YAML_CONTENT_PROPERTY;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.marketplace.MarketplaceAddonHandler;
import org.openhab.core.addon.marketplace.MarketplaceHandlerException;
import org.openhab.core.ui.components.RootUIComponent;
import org.openhab.core.ui.components.UIComponentRegistry;
import org.openhab.core.ui.components.UIComponentRegistryFactory;
import org.openhab.core.ui.components.converter.RootUIComponentParser;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * A {@link MarketplaceAddonHandler} implementation, which handles block libraries as YAML files and installs
 * them by adding them to the {@link UIComponentRegistry} for the ui:blocks namespace.
 *
 * @author Yannick Schaus - Initial contribution and API
 *
 */
@Component(immediate = true)
@NonNullByDefault
public class CommunityBlockLibaryAddonHandler implements MarketplaceAddonHandler {
    private final Logger logger = LoggerFactory.getLogger(CommunityBlockLibaryAddonHandler.class);

    private final ObjectMapper yamlMapper;
    private UIComponentRegistry blocksRegistry;
    private final Map<String, RootUIComponentParser> parsers = new ConcurrentHashMap<>();

    @Activate
    public CommunityBlockLibaryAddonHandler(final @Reference UIComponentRegistryFactory uiComponentRegistryFactory) {
        this.blocksRegistry = uiComponentRegistryFactory.getRegistry("ui:blocks");
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        yamlMapper.findAndRegisterModules();
        this.yamlMapper.setDateFormat(new SimpleDateFormat("MMM d, yyyy, hh:mm:ss aa", Locale.ENGLISH));
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addParser(RootUIComponentParser parser) {
        parsers.put(parser.getParserFormat(), parser);
    }

    protected void removeParser(RootUIComponentParser parser) {
        parsers.remove(parser.getParserFormat());
    }

    @Override
    public boolean supports(String type, String contentType) {
        return "automation".equals(type) && BLOCKLIBRARIES_CONTENT_TYPE.equals(contentType);
    }

    @Override
    public boolean isInstalled(String id) {
        return blocksRegistry.getAll().stream().anyMatch(w -> w.hasTag(id));
    }

    @Override
    public void install(Addon addon) throws MarketplaceHandlerException {
        try {
            String yamlDownloadUrl = (String) addon.getProperties().get(YAML_DOWNLOAD_URL_PROPERTY);
            String yamlContent = (String) addon.getProperties().get(YAML_CONTENT_PROPERTY);

            if (yamlDownloadUrl != null) {
                addBlocksAsYAML(addon.getUid(), getBlocksFromURL(yamlDownloadUrl));
            } else if (yamlContent != null) {
                addBlocksAsYAML(addon.getUid(), yamlContent);
            } else {
                logger.error("Block library {} has neither download URL nor embedded content", addon.getUid());
                throw new MarketplaceHandlerException("Block library has neither download URL nor embedded content",
                        null);
            }
        } catch (IOException e) {
            logger.error("Block library from marketplace cannot be downloaded: {}", e.getMessage());
            throw new MarketplaceHandlerException("Block library cannot be downloaded.", e);
        } catch (Exception e) {
            logger.error("Block library from marketplace is invalid: {}", e.getMessage());
            throw new MarketplaceHandlerException("Block library is not valid.", e);
        }
    }

    @Override
    public void uninstall(Addon addon) throws MarketplaceHandlerException {
        blocksRegistry.getAll().stream().filter(w -> w.hasTag(addon.getUid())).forEach(w -> {
            blocksRegistry.remove(w.getUID());
        });
    }

    private String getBlocksFromURL(String urlString) throws IOException {
        URL u;
        try {
            u = (new URI(urlString)).toURL();
        } catch (IllegalArgumentException | URISyntaxException e) {
            throw new IOException(e);
        }
        try (InputStream in = u.openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void addBlocksAsYAML(String id, String yaml) throws MarketplaceHandlerException {
        if (yaml.trim().startsWith("version:")) {
            // Use the "new YAML" parser
            RootUIComponentParser parser = parsers.get("YAML");

            // The parser might not have been registered yet
            if (parser == null) {
                throw new MarketplaceHandlerException("No blocks YAML parser available", null);
            }

            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            String modelName = parser.startParsingFormat(yaml, errors, warnings);
            if (modelName == null || !errors.isEmpty()) {
                if (modelName != null) {
                    parser.finishParsingFormat(modelName);
                }
                throw new MarketplaceHandlerException("Parsing of YAML failed: " + String.join(", ", errors), null);
            }
            if (!warnings.isEmpty()) {
                logger.warn("Parsing of marketplace block library add-on {} has warnings: {}", id,
                        String.join(", ", warnings));
            }
            Collection<? extends RootUIComponent> blocksLibraries;
            try {
                blocksLibraries = parser.getParsedObjects(modelName);
            } finally {
                parser.finishParsingFormat(modelName);
            }

            for (RootUIComponent blocks : blocksLibraries) {
                // add a tag with the add-on ID to be able to identify the block library in the registry
                blocks.addTag(id);
                blocksRegistry.add(blocks);
            }
        } else {
            // Use the "old YAML" parser
            try {
                RootUIComponent blocks = yamlMapper.readValue(yaml, RootUIComponent.class);
                // add a tag with the add-on ID to be able to identify the block library in the registry
                blocks.addTag(id);
                blocksRegistry.add(blocks);
            } catch (IOException e) {
                logger.error("Unable to parse YAML: {}", e.getMessage());
                throw new IllegalArgumentException("Unable to parse YAML");
            }
        }
    }
}
