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
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.OpenHAB;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.marketplace.MarketplaceAddonHandler;
import org.openhab.core.addon.marketplace.MarketplaceHandlerException;
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
@NonNullByDefault
public class CommunityKarafAddonHandler implements MarketplaceAddonHandler {
    private static final String KAR_CONTENT_TYPE = "application/vnd.openhab.feature;type=karfile";
    private static final String BUNDLE_CACHE_PATH = OpenHAB.getUserDataFolder() + File.separator + "marketplace"
            + File.separator + "kar";
    private static final List<String> SUPPORTED_EXT_TYPES = List.of("binding");
    private static final String KAR_DOWNLOAD_URL_PROPERTY = "kar_download_url";

    private final Logger logger = LoggerFactory.getLogger(CommunityKarafAddonHandler.class);

    private final FeaturesService featuresService;

    @Activate
    public CommunityKarafAddonHandler(@Reference FeaturesService featuresService) {
        this.featuresService = featuresService;
        ensureCachedBundlesAreInstalled();
    }

    @Override
    public boolean supports(String type, String contentType) {
        // we support only certain extension types, and only as pure OSGi bundles
        return SUPPORTED_EXT_TYPES.contains(type) && KAR_CONTENT_TYPE.equals(contentType);
    }

    @Override
    @SuppressWarnings("null")
    public boolean isInstalled(String addonId) {
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
            sourceUrl = new URL((String) addon.getProperties().get(KAR_DOWNLOAD_URL_PROPERTY));
            addBundleToCache(addon.getId(), sourceUrl);
            installFromCache(addon.getId());
        } catch (MalformedURLException e) {
            throw new MarketplaceHandlerException("Malformed source URL: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("null")
    public void uninstall(Addon addon) throws MarketplaceHandlerException {
        try {
            File addonPath = getAddonCacheDirectory(addon.getId());
            List<URI> repositories = Arrays
                    .stream(Objects.requireNonNullElse(featuresService.listRepositories(), new Repository[] {}))
                    .map(Repository::getURI).collect(Collectors.toList());
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

    /**
     * Downloads a bundle file from a remote source and puts it in the local cache with the add-on ID.
     *
     * @param addonId the add-on ID
     * @param sourceUrl the (online) source where the .kar file can be found
     * @throws MarketplaceHandlerException on error
     */
    private void addBundleToCache(String addonId, URL sourceUrl) throws MarketplaceHandlerException {
        try {
            String fileName = new File(sourceUrl.toURI().getPath()).getName();
            File addonFile = new File(getAddonCacheDirectory(addonId), fileName);
            addonFile.getParentFile().mkdirs();

            InputStream source = sourceUrl.openStream();
            Path outputPath = Path.of(addonFile.toURI());
            Files.copy(source, outputPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | URISyntaxException e) {
            throw new MarketplaceHandlerException("Cannot copy bundle to local cache: " + e.getMessage());
        }
    }

    private void installFromCache(String addonId) throws MarketplaceHandlerException {
        File addonPath = getAddonCacheDirectory(addonId);
        if (addonPath.exists() && addonPath.isDirectory()) {
            File[] bundleFiles = addonPath.listFiles();
            if (bundleFiles == null || bundleFiles.length != 1) {
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

    private void ensureCachedBundlesAreInstalled() {
        File addonPath = new File(BUNDLE_CACHE_PATH);
        if (addonPath.exists() && addonPath.isDirectory()) {
            for (File bundleFile : addonPath.listFiles()) {
                if (bundleFile.isDirectory()) {
                    String addonId = "marketplace:" + bundleFile.getName();
                    if (!isInstalled(addonId)) {
                        logger.info("Reinstalling missing marketplace bundle: {}", addonId);
                        try {
                            installFromCache(addonId);
                        } catch (MarketplaceHandlerException e) {
                            logger.warn("Failed reinstalling add-on from cache", e);
                        }
                    }
                }
            }
        }
    }

    private File getAddonCacheDirectory(String addonId) {
        return new File(BUNDLE_CACHE_PATH + File.separator + addonId.replace("marketplace:", ""));
    }
}
