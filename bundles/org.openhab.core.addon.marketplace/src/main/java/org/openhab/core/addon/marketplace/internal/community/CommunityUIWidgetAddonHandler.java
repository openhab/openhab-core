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
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * A {@link MarketplaceAddonHandler} implementation, which handles UI widgets as YAML files and installs
 * them by adding them to the {@link UIComponentRegistry} for the ui:widget namespace.
 *
 * @author Yannick Schaus - Initial contribution and API
 *
 */
@Component(immediate = true)
@NonNullByDefault
public class CommunityUIWidgetAddonHandler implements MarketplaceAddonHandler {
    private final Logger logger = LoggerFactory.getLogger(CommunityUIWidgetAddonHandler.class);

    private final ObjectMapper yamlMapper;
    private final UIComponentRegistry widgetRegistry;
    private final Map<String, RootUIComponentParser> parsers = new ConcurrentHashMap<>();

    @Activate
    public CommunityUIWidgetAddonHandler(final @Reference UIComponentRegistryFactory uiComponentRegistryFactory) {
        this.widgetRegistry = uiComponentRegistryFactory.getRegistry("ui:widget");
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        yamlMapper.findAndRegisterModules();
        this.yamlMapper.setDateFormat(new SimpleDateFormat("MMM d, yyyy, hh:mm:ss aa", Locale.ENGLISH));
        yamlMapper.setAnnotationIntrospector(new AnnotationIntrospectorPair(new SerializedNameAnnotationIntrospector(),
                yamlMapper.getSerializationConfig().getAnnotationIntrospector()));
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
        return "ui".equals(type) && UIWIDGETS_CONTENT_TYPE.equals(contentType);
    }

    @Override
    public boolean isInstalled(String id) {
        return widgetRegistry.getAll().stream().anyMatch(w -> w.hasTag(id));
    }

    @Override
    public void install(Addon addon) throws MarketplaceHandlerException {
        try {
            String yamlDownloadUrl = (String) addon.getProperties().get(YAML_DOWNLOAD_URL_PROPERTY);
            String yamlContent = (String) addon.getProperties().get(YAML_CONTENT_PROPERTY);

            if (yamlDownloadUrl != null) {
                addWidgetAsYAML(addon.getUid(), getWidgetFromURL(yamlDownloadUrl));
            } else if (yamlContent != null) {
                addWidgetAsYAML(addon.getUid(), yamlContent);
            } else {
                logger.error("UI Widget {} has neither download URL nor embedded content", addon.getUid());
                throw new MarketplaceHandlerException("UI Widget has neither download URL nor embedded content", null);
            }
        } catch (MarketplaceHandlerException e) {
            logger.error("Failed to install widget '{}' from the marketplace: {}", addon.getId(), e.getMessage());
            throw e;
        } catch (IOException e) {
            logger.error("Widget from marketplace cannot be downloaded: {}", e.getMessage());
            throw new MarketplaceHandlerException("Widget cannot be downloaded.", e);
        } catch (Exception e) {
            logger.error("Widget from marketplace is invalid: {}", e.getMessage());
            throw new MarketplaceHandlerException("Widget is not valid.", e);
        }
    }

    @Override
    public void uninstall(Addon addon) throws MarketplaceHandlerException {
        widgetRegistry.getAll().stream().filter(w -> w.hasTag(addon.getUid())).forEach(w -> {
            widgetRegistry.remove(w.getUID());
        });
    }

    private String getWidgetFromURL(String urlString) throws IOException {
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

    private void addWidgetAsYAML(String id, String yaml) throws MarketplaceHandlerException {
        if (yaml.trim().startsWith("version:")) {
            // Use the "new YAML" parser
            RootUIComponentParser parser = parsers.get("YAML");

            // The parser might not have been registered yet
            if (parser == null) {
                throw new MarketplaceHandlerException("No widget YAML parser available", null);
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
                logger.warn("Parsing of marketplace widget add-on {} has warnings: {}", id,
                        String.join(", ", warnings));
            }
            Collection<? extends RootUIComponent> widgets;
            try {
                widgets = parser.getParsedObjects(modelName);
            } finally {
                parser.finishParsingFormat(modelName);
            }

            for (RootUIComponent widget : widgets) {
                // add a tag with the add-on ID to be able to identify the widget in the registry
                widget.addTag(id);
                widgetRegistry.add(widget);
            }
        } else {
            // Use the "old YAML" parser
            try {
                RootUIComponent widget = yamlMapper.readValue(yaml, RootUIComponent.class);
                // add a tag with the add-on ID to be able to identify the widget in the registry
                widget.addTag(id);
                widgetRegistry.add(widget);
            } catch (IOException e) {
                logger.error("Unable to parse YAML: {}", e.getMessage());
                throw new MarketplaceHandlerException("Unable to parse YAML", e);
            }
        }
    }
}
