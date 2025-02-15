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
package org.openhab.core.addon.marketplace.internal.community;

import static org.openhab.core.addon.marketplace.MarketplaceConstants.*;
import static org.openhab.core.addon.marketplace.internal.community.CommunityMarketplaceAddonService.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.marketplace.MarketplaceAddonHandler;
import org.openhab.core.addon.marketplace.MarketplaceHandlerException;
import org.openhab.core.addon.marketplace.internal.automation.MarketplaceRuleTemplateProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link MarketplaceAddonHandler} implementation, which handles rule templates as JSON files and installs
 * them by adding them to a {@link org.openhab.core.storage.Storage}. The templates are then served from this storage
 * through a dedicated
 * {@link org.openhab.core.automation.template.RuleTemplateProvider}.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Yannick Schaus - refactoring
 *
 */
@Component(immediate = true)
@NonNullByDefault
public class CommunityRuleTemplateAddonHandler implements MarketplaceAddonHandler {
    private final Logger logger = LoggerFactory.getLogger(CommunityRuleTemplateAddonHandler.class);

    private final MarketplaceRuleTemplateProvider marketplaceRuleTemplateProvider;

    @Activate
    public CommunityRuleTemplateAddonHandler(
            @Reference MarketplaceRuleTemplateProvider marketplaceRuleTemplateProvider) {
        this.marketplaceRuleTemplateProvider = marketplaceRuleTemplateProvider;
    }

    @Override
    public boolean supports(String type, String contentType) {
        return "automation".equals(type) && RULETEMPLATES_CONTENT_TYPE.equals(contentType);
    }

    @Override
    public boolean isInstalled(String id) {
        return marketplaceRuleTemplateProvider.getAll().stream().anyMatch(t -> t.getTags().contains(id));
    }

    @Override
    public void install(Addon addon) throws MarketplaceHandlerException {
        try {
            String jsonDownloadUrl = (String) addon.getProperties().get(JSON_DOWNLOAD_URL_PROPERTY);
            String yamlDownloadUrl = (String) addon.getProperties().get(YAML_DOWNLOAD_URL_PROPERTY);
            String jsonContent = (String) addon.getProperties().get(JSON_CONTENT_PROPERTY);
            String yamlContent = (String) addon.getProperties().get(YAML_CONTENT_PROPERTY);

            if (jsonDownloadUrl != null) {
                marketplaceRuleTemplateProvider.addTemplateAsJSON(addon.getUid(), getTemplateFromURL(jsonDownloadUrl));
            } else if (yamlDownloadUrl != null) {
                marketplaceRuleTemplateProvider.addTemplateAsYAML(addon.getUid(), getTemplateFromURL(yamlDownloadUrl));
            } else if (jsonContent != null) {
                marketplaceRuleTemplateProvider.addTemplateAsJSON(addon.getUid(), jsonContent);
            } else if (yamlContent != null) {
                marketplaceRuleTemplateProvider.addTemplateAsYAML(addon.getUid(), yamlContent);
            }
        } catch (IOException e) {
            logger.error("Rule template from marketplace cannot be downloaded: {}", e.getMessage());
            throw new MarketplaceHandlerException("Rule template cannot be downloaded", e);
        } catch (Exception e) {
            logger.error("Failed to add rule template from the marketplace: {}", e.getMessage());
            throw new MarketplaceHandlerException("Rule template is invalid", e);
        }
    }

    @Override
    public void uninstall(Addon addon) throws MarketplaceHandlerException {
        marketplaceRuleTemplateProvider.getAll().stream().filter(t -> t.getTags().contains(addon.getUid()))
                .forEach(w -> {
                    marketplaceRuleTemplateProvider.remove(w.getUID());
                });
    }

    private String getTemplateFromURL(String urlString) throws IOException {
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
}
