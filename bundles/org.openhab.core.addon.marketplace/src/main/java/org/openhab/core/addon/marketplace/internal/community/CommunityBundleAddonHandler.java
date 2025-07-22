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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.marketplace.MarketplaceAddonHandler;
import org.openhab.core.addon.marketplace.MarketplaceBundleInstaller;
import org.openhab.core.addon.marketplace.MarketplaceHandlerException;
import org.openhab.core.common.ThreadPoolManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link MarketplaceAddonHandler} implementation, which handles add-ons as jar files (specifically, OSGi
 * bundles) and installs them through the standard OSGi bundle installation mechanism.
 * The bundles installed this way have their location set to the add-on ID to identify them and determine their
 * installation status.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Yannick Schaus - refactoring
 *
 */
@Component(immediate = true)
@NonNullByDefault
public class CommunityBundleAddonHandler extends MarketplaceBundleInstaller implements MarketplaceAddonHandler {
    private static final List<String> SUPPORTED_EXT_TYPES = List.of("automation", "binding", "misc", "persistence",
            "transformation", "ui", "voice");

    private final Logger logger = LoggerFactory.getLogger(CommunityBundleAddonHandler.class);
    private final ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool(ThreadPoolManager.THREAD_POOL_NAME_COMMON);
    private final BundleContext bundleContext;
    private boolean isReady = false;

    @Activate
    public CommunityBundleAddonHandler(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        scheduler.execute(() -> {
            ensureCachedBundlesAreInstalled(bundleContext);
            isReady = true;
        });
    }

    @Override
    public boolean supports(String type, String contentType) {
        // we support only certain extension types, and only as pure OSGi bundles
        return SUPPORTED_EXT_TYPES.contains(type) && contentType.equals(JAR_CONTENT_TYPE);
    }

    @Override
    public boolean isInstalled(String id) {
        return isBundleInstalled(bundleContext, id);
    }

    @Override
    public void install(Addon addon) throws MarketplaceHandlerException {
        Object urlObject = addon.getProperties().get(JAR_DOWNLOAD_URL_PROPERTY);
        if (!(urlObject instanceof String)) {
            logger.error("Bundle {} has no JAR download URL", addon.getUid());
            throw new MarketplaceHandlerException("Bundle has no JAR download URL", null);
        }

        URL sourceUrl;
        try {
            sourceUrl = new URI((String) urlObject).toURL();
        } catch (IllegalArgumentException | MalformedURLException | URISyntaxException e) {
            throw new MarketplaceHandlerException("Malformed source URL: " + e.getMessage(), e);
        }
        addBundleToCache(addon.getUid(), sourceUrl);
        installFromCache(bundleContext, addon.getUid());
    }

    @Override
    public void uninstall(Addon addon) throws MarketplaceHandlerException {
        uninstallBundle(bundleContext, addon.getUid());
    }

    @Override
    public boolean isReady() {
        return isReady;
    }
}
