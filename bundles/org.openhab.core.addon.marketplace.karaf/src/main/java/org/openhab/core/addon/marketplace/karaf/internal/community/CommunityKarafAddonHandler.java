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
package org.openhab.core.addon.marketplace.karaf.internal.community;

import static org.openhab.core.addon.marketplace.internal.community.CommunityMarketplaceAddonService.KAR_CONTENT_TYPE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.karaf.kar.KarService;
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
    private static final Path BUNDLE_CACHE_PATH = Path.of(OpenHAB.getUserDataFolder(), "marketplace", "kar");
    private static final List<String> SUPPORTED_EXT_TYPES = List.of("automation", "binding", "io", "persistence",
            "transformation", "ui", "voice");
    private static final String KAR_DOWNLOAD_URL_PROPERTY = "kar_download_url";
    private static final String KAR_EXTENSION = ".kar";

    private final Logger logger = LoggerFactory.getLogger(CommunityKarafAddonHandler.class);

    private final KarService karService;

    @Activate
    public CommunityKarafAddonHandler(@Reference KarService karService) {
        this.karService = karService;
        ensureCachedBundlesAreInstalled();
    }

    @Override
    public boolean supports(String type, String contentType) {
        return SUPPORTED_EXT_TYPES.contains(type) && KAR_CONTENT_TYPE.equals(contentType);
    }

    private Stream<Path> karFilesStream(Path addonDirectory) throws IOException {
        return Files.isDirectory(addonDirectory) ? Files.list(addonDirectory).map(Path::getFileName)
                .filter(path -> path.toString().endsWith(KAR_EXTENSION)) : Stream.empty();
    }

    private String pathToKarRepoName(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.substring(0, fileName.length() - KAR_EXTENSION.length());
    }

    @Override
    @SuppressWarnings("null")
    public boolean isInstalled(String addonId) {
        try {
            Path addonDirectory = getAddonCacheDirectory(addonId);
            List<String> repositories = karService.list();
            return karFilesStream(addonDirectory).findFirst().map(this::pathToKarRepoName).map(repositories::contains)
                    .orElse(false);
        } catch (Exception e) {
            logger.warn("Failed to determine installation status for {}: ", addonId, e);
        }

        return false;
    }

    @Override
    public void install(Addon addon) throws MarketplaceHandlerException {
        try {
            URL sourceUrl = new URL((String) addon.getProperties().get(KAR_DOWNLOAD_URL_PROPERTY));
            addBundleToCache(addon.getId(), sourceUrl);
            installFromCache(addon.getId());
        } catch (MalformedURLException e) {
            throw new MarketplaceHandlerException("Malformed source URL: " + e.getMessage(), e);
        }
    }

    @Override
    public void uninstall(Addon addon) throws MarketplaceHandlerException {
        try {
            Path addonPath = getAddonCacheDirectory(addon.getId());
            List<String> repositories = karService.list();
            for (Path path : karFilesStream(addonPath).collect(Collectors.toList())) {
                String karRepoName = pathToKarRepoName(path);
                if (repositories.contains(karRepoName)) {
                    karService.uninstall(karRepoName);
                }
                Files.delete(addonPath.resolve(path));
            }
            Files.delete(addonPath);
        } catch (Exception e) {
            throw new MarketplaceHandlerException("Failed uninstalling bundle: " + e.getMessage(), e);
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
            Path addonFile = getAddonCacheDirectory(addonId).resolve(fileName);
            Files.createDirectories(addonFile.getParent());
            InputStream source = sourceUrl.openStream();
            Files.copy(source, addonFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | URISyntaxException e) {
            throw new MarketplaceHandlerException("Cannot copy bundle to local cache: " + e.getMessage(), e);
        }
    }

    private void installFromCache(String addonId) throws MarketplaceHandlerException {
        Path addonPath = getAddonCacheDirectory(addonId);
        if (Files.isDirectory(addonPath)) {
            try {
                List<Path> bundleFiles = Files.list(addonPath).collect(Collectors.toList());
                if (bundleFiles.size() != 1) {
                    throw new MarketplaceHandlerException(
                            "The local cache folder doesn't contain a single file: " + addonPath, null);
                }
                try {
                    karService.install(bundleFiles.get(0).toUri(), false);
                } catch (Exception e) {
                    throw new MarketplaceHandlerException(
                            "Cannot install bundle from marketplace cache: " + e.getMessage(), e);
                }
            } catch (IOException e) {
                throw new MarketplaceHandlerException("Could not list files in cache directory " + addonPath, e);
            }
        }
    }

    private void ensureCachedBundlesAreInstalled() {
        try {
            if (Files.isDirectory(BUNDLE_CACHE_PATH)) {
                Files.list(BUNDLE_CACHE_PATH).filter(Files::isDirectory).map(p -> "marketplace:" + p.getFileName())
                        .filter(addonId -> !isInstalled(addonId)).forEach(addonId -> {
                            logger.info("Reinstalling missing marketplace bundle: {}", addonId);
                            try {
                                installFromCache(addonId);
                            } catch (MarketplaceHandlerException e) {
                                logger.warn("Failed reinstalling add-on from cache", e);
                            }
                        });
            }
        } catch (IOException e) {
            logger.warn("Failed to re-install bundles: {}", e.getMessage());
        }
    }

    private Path getAddonCacheDirectory(String addonId) {
        return BUNDLE_CACHE_PATH.resolve(addonId.replace("marketplace:", ""));
    }
}
