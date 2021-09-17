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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.marketplace.MarketplaceAddonHandler;
import org.openhab.core.addon.marketplace.MarketplaceBundleInstaller;
import org.openhab.core.addon.marketplace.MarketplaceHandlerException;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link CommunityKarafAddonHandler} implementation, which handles add-ons as kar files and installs them through
 * the standard OSGi feature repository installation mechanism.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Yannick Schaus - refactoring
 * @author Jan N. Klug - refactor to support kar files
 *
 */
@Component(immediate = true)
public class CommunityKarafAddonHandler extends MarketplaceBundleInstaller implements MarketplaceAddonHandler {
    private static final String KAR_CONTENT_TYPE = "application/vnd.openhab.kar";

    // add-on types supported by this handler
    private static final List<String> SUPPORTED_EXT_TYPES = List.of("automation", "binding", "persistence",
            "transformation");

    private static final String JAR_DOWNLOAD_URL_PROPERTY = "jar_download_url";

    private final Logger logger = LoggerFactory.getLogger(CommunityKarafAddonHandler.class);

    private final BundleContext bundleContext;
    private final FeaturesService featuresService;

    @Activate
    public CommunityKarafAddonHandler(BundleContext bundleContext, @Reference FeaturesService featuresService,
            Map<String, Object> config) {
        this.bundleContext = bundleContext;
        this.featuresService = featuresService;
        ensureCachedBundlesAreInstalled(bundleContext);
    }

    @Override
    public boolean supports(String type, String contentType) {
        // we support only certain extension types, and only as pure OSGi bundles
        return SUPPORTED_EXT_TYPES.contains(type) && KAR_CONTENT_TYPE.equals(contentType);
    }

    @Override
    public boolean isInstalled(String id) {
        return isBundleInstalled(bundleContext, id);
    }

    @Override
    protected boolean isBundleInstalled(BundleContext bundleContext, String addonId) {
        try {
            File addonDirectory = getAddonCacheDirectory(addonId);
            List<URI> repositories = Arrays.stream(featuresService.listRepositories()).map(Repository::getURI)
                    .collect(Collectors.toList());
            if (addonDirectory.exists() && addonDirectory.isDirectory()) {
                return Files.list(addonDirectory.toPath()).filter(path -> path.endsWith(".kar")).findFirst()
                        .map(Path::toUri).map(repositories::contains).orElse(false);
            }
        } catch (Exception e) {
            logger.warn("Failed to determine installation status for {}: ", addonId, e);
        }

        return false;
    }

    @Override
    public void install(Addon addon) throws MarketplaceHandlerException {
        URL sourceUrl;
        try {
            sourceUrl = new URL((String) addon.getProperties().get(JAR_DOWNLOAD_URL_PROPERTY));
            addBundleToCache(addon.getId(), sourceUrl);
            installFromCache(bundleContext, addon.getId());
        } catch (MalformedURLException e) {
            throw new MarketplaceHandlerException("Malformed source URL: " + e.getMessage());
        }
    }

    @Override
    protected void installFromCache(BundleContext bundleContext, String addonId) throws MarketplaceHandlerException {
        File addonPath = getAddonCacheDirectory(addonId);
        if (addonPath.exists() && addonPath.isDirectory()) {
            File[] bundleFiles = addonPath.listFiles();
            if (bundleFiles != null && bundleFiles.length != 1) {
                throw new MarketplaceHandlerException(
                        "The local cache folder doesn't contain a single file: " + addonPath);
            }
            try {
                featuresService.addRepository(bundleFiles[0].toURI(), true);
            } catch (Exception e) {
                throw new MarketplaceHandlerException(
                        "Cannot install bundle from marketplace cache: " + e.getMessage());
            }
        }
    }

    @Override
    public void uninstall(Addon addon) throws MarketplaceHandlerException {
        uninstallBundle(bundleContext, addon.getId());
    }

    @Override
    protected void uninstallBundle(BundleContext bundleContext, String addonId) throws MarketplaceHandlerException {
        try {
            File addonPath = getAddonCacheDirectory(addonId);
            List<URI> repositories = Arrays.stream(featuresService.listRepositories()).map(Repository::getURI)
                    .collect(Collectors.toList());
            if (addonPath.exists() && addonPath.isDirectory()) {
                for (File file : Objects.requireNonNullElse(addonPath.listFiles(), new File[] {})) {
                    if (repositories.contains(file.toURI())) {
                        featuresService.removeRepository(file.toURI(), true);
                    }
                    file.delete();
                }
            }
            addonPath.delete();
        } catch (Exception e) {
            throw new MarketplaceHandlerException("Failed uninstalling bundle: " + e.getMessage());
        }
    }
}
