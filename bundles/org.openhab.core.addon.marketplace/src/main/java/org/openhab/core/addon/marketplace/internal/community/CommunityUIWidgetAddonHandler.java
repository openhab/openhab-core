/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.marketplace.MarketplaceAddonHandler;
import org.openhab.core.addon.marketplace.MarketplaceHandlerException;
import org.openhab.core.ui.components.RootUIComponent;
import org.openhab.core.ui.components.UIComponentRegistry;
import org.openhab.core.ui.components.UIComponentRegistryFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
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
    private static final String YAML_DOWNLOAD_URL_PROPERTY = "yaml_download_url";
    private static final String YAML_CONTENT_PROPERTY = "yaml_content";
    private static final String UIWIDGETS_CONTENT_TYPE = "application/vnd.openhab.uicomponent;type=widget";

    private final Logger logger = LoggerFactory.getLogger(CommunityUIWidgetAddonHandler.class);
    ObjectMapper yamlMapper;

    private UIComponentRegistry widgetRegistry;

    @Activate
    public CommunityUIWidgetAddonHandler(final @Reference UIComponentRegistryFactory uiComponentRegistryFactory) {
        this.widgetRegistry = uiComponentRegistryFactory.getRegistry("ui:widget");
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        yamlMapper.findAndRegisterModules();
        this.yamlMapper.setDateFormat(new SimpleDateFormat("MMM d, yyyy, hh:mm:ss aa", Locale.ENGLISH));
        yamlMapper.setAnnotationIntrospector(new AnnotationIntrospectorPair(new SerializedNameAnnotationIntrospector(),
                yamlMapper.getSerializationConfig().getAnnotationIntrospector()));
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
                throw new IllegalArgumentException("Couldn't find the widget in the add-on entry");
            }
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
        URL u = new URL(urlString);
        try (InputStream in = u.openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void addWidgetAsYAML(String id, String yaml) {
        try {
            RootUIComponent widget = yamlMapper.readValue(yaml, RootUIComponent.class);
            // add a tag with the add-on ID to be able to identify the widget in the registry
            widget.addTag(id);
            widgetRegistry.add(widget);
        } catch (IOException e) {
            logger.error("Unable to parse YAML: {}", e.getMessage());
            throw new IllegalArgumentException("Unable to parse YAML");
        }
    }
}
