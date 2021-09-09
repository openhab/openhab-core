/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.marketplace.MarketplaceAddonHandler;
import org.openhab.core.addon.marketplace.MarketplaceHandlerException;
import org.openhab.core.addon.marketplace.internal.automation.MarketplaceRuleTemplateProvider;
import org.openhab.core.automation.template.RuleTemplateProvider;
import org.openhab.core.storage.Storage;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link MarketplaceExtensionHandler} implementation, which handles rule templates as JSON files and installs
 * them by adding them to a {@link Storage}. The templates are then served from this storage through a dedicated
 * {@link RuleTemplateProvider}.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Yannick Schaus - refactoring
 *
 */
@Component
public class CommunityRuleTemplateAddonHandler implements MarketplaceAddonHandler {
    private static final String JSON_DOWNLOAD_URL_PROPERTY = "json_download_url";
    private static final String YAML_DOWNLOAD_URL_PROPERTY = "yaml_download_url";
    private static final String JSON_CONTENT_PROPERTY = "json_content";
    private static final String YAML_CONTENT_PROPERTY = "yaml_content";
    private static final String RULETEMPLATES_CONTENT_TYPE = "application/vnd.openhab.ruletemplate";

    private final Logger logger = LoggerFactory.getLogger(CommunityRuleTemplateAddonHandler.class);

    private MarketplaceRuleTemplateProvider marketplaceRuleTemplateProvider;

    @Reference
    protected void setMarketplaceRuleTemplateProvider(MarketplaceRuleTemplateProvider marketplaceRuleTemplateProvider) {
        this.marketplaceRuleTemplateProvider = marketplaceRuleTemplateProvider;
    }

    protected void unsetMarketplaceRuleTemplateProvider(
            MarketplaceRuleTemplateProvider marketplaceRuleTemplateProvider) {
        this.marketplaceRuleTemplateProvider = null;
    }

    @Override
    public boolean supports(String type, String contentType) {
        return type.equals("automation") && contentType.equals(RULETEMPLATES_CONTENT_TYPE);
    }

    @Override
    public boolean isInstalled(String id) {
        return marketplaceRuleTemplateProvider.getAll().stream().anyMatch(t -> t.getTags().contains(id));
    }

    @Override
    public void install(Addon addon) throws MarketplaceHandlerException {
        try {
            String template;
            if (addon.getProperties().containsKey(JSON_DOWNLOAD_URL_PROPERTY)) {
                template = getTemplateFromURL((String) addon.getProperties().get(JSON_DOWNLOAD_URL_PROPERTY));
                marketplaceRuleTemplateProvider.addTemplateAsJSON(addon.getId(), template);
            } else if (addon.getProperties().containsKey(YAML_DOWNLOAD_URL_PROPERTY)) {
                template = getTemplateFromURL((String) addon.getProperties().get(YAML_DOWNLOAD_URL_PROPERTY));
                marketplaceRuleTemplateProvider.addTemplateAsYAML(addon.getId(), template);
            } else if (addon.getProperties().containsKey(JSON_CONTENT_PROPERTY)) {
                template = (@NonNull String) addon.getProperties().get(JSON_CONTENT_PROPERTY);
                marketplaceRuleTemplateProvider.addTemplateAsJSON(addon.getId(), template);
            } else if (addon.getProperties().containsKey(YAML_CONTENT_PROPERTY)) {
                template = (@NonNull String) addon.getProperties().get(YAML_CONTENT_PROPERTY);
                marketplaceRuleTemplateProvider.addTemplateAsYAML(addon.getId(), template);
            }
        } catch (IOException e) {
            logger.error("Rule template from marketplace cannot be downloaded: {}", e.getMessage());
            throw new MarketplaceHandlerException("Template cannot be downloaded.");
        } catch (Exception e) {
            logger.error("Rule template from marketplace is invalid: {}", e.getMessage());
            throw new MarketplaceHandlerException("Template is not valid.");
        }
    }

    @Override
    public void uninstall(Addon addon) throws MarketplaceHandlerException {
        marketplaceRuleTemplateProvider.getAll().stream().filter(t -> t.getTags().contains(addon.getId()))
                .forEach(w -> {
                    marketplaceRuleTemplateProvider.remove(w.getUID());
                });
    }

    private String getTemplateFromURL(String urlString) throws IOException {
        URL u = new URL(urlString);
        try (InputStream in = u.openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
