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
package org.openhab.core.addon.marketplace;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.OpenHAB;
import org.openhab.core.util.UIDUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle the management of bundles related to marketplace add-ons that resists OSGi cache cleanups.
 *
 * These operations cache incoming bundle files locally in a structure under the user data folder, and can make sure the
 * bundles are re-installed if they are present in the local cache but not installed in the OSGi framework.
 * They can be used by marketplace handler implementations dealing with OSGi bundles.
 *
 * @author Yannick Schaus - Initial contribution and API
 *
 */
@NonNullByDefault
public abstract class MarketplaceBundleInstaller {
    private final Logger logger = LoggerFactory.getLogger(MarketplaceBundleInstaller.class);

    private static final Path BUNDLE_CACHE_PATH = Path.of(OpenHAB.getUserDataFolder(), "marketplace", "bundles");

    /**
     * Downloads a bundle file from a remote source and puts it in the local cache with the add-on ID.
     *
     * @param addonId the add-on ID
     * @param sourceUrl the (online) source where the .jar file can be found
     * @throws MarketplaceHandlerException
     */
    protected void addBundleToCache(String addonId, URL sourceUrl) throws MarketplaceHandlerException {
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

    /**
     * Installs a bundle from its ID by looking up in the local cache
     *
     * @param bundleContext the {@link BundleContext} to use to install the bundle
     * @param addonId the add-on ID
     * @throws MarketplaceHandlerException
     */
    protected void installFromCache(BundleContext bundleContext, String addonId) throws MarketplaceHandlerException {
        Path addonPath = getAddonCacheDirectory(addonId);
        if (Files.isDirectory(addonPath)) {
            try (Stream<Path> files = Files.list(addonPath)) {
                List<Path> bundleFiles = files.toList();
                if (bundleFiles.size() != 1) {
                    throw new MarketplaceHandlerException(
                            "The local cache folder doesn't contain a single file: " + addonPath, null);
                }

                try (FileInputStream fileInputStream = new FileInputStream(bundleFiles.get(0).toFile())) {
                    Bundle bundle = bundleContext.installBundle(addonId, fileInputStream);
                    try {
                        bundle.start();
                    } catch (BundleException e) {
                        logger.warn("The marketplace bundle was successfully installed but doesn't start: {}",
                                e.getMessage());
                    }
                }
            } catch (IOException | BundleException e) {
                throw new MarketplaceHandlerException("Cannot install bundle from marketplace cache: " + e.getMessage(),
                        e);
            }
        }
    }

    /**
     * Determines whether a bundle associated to the given add-on ID is installed
     *
     * @param bundleContext the {@link BundleContext} to use to look up the bundle
     * @param addonId the add-on ID
     */
    protected boolean isBundleInstalled(BundleContext bundleContext, String addonId) {
        return bundleContext.getBundle(addonId) != null;
    }

    /**
     * Uninstalls a bundle associated to the given add-on ID. Also removes it from the local cache.
     *
     * @param bundleContext the {@link BundleContext} to use to look up the bundle
     * @param addonId the add-on ID
     */
    protected void uninstallBundle(BundleContext bundleContext, String addonId) throws MarketplaceHandlerException {
        try {
            Path addonPath = getAddonCacheDirectory(addonId);
            if (Files.isDirectory(addonPath)) {
                try (Stream<Path> files = Files.list(addonPath)) {
                    for (Path path : files.toList()) {
                        Files.delete(path);
                    }
                }
            }
            Files.delete(addonPath);
        } catch (IOException e) {
            throw new MarketplaceHandlerException("Failed to delete bundle-files: " + e.getMessage(), e);
        }
        if (isBundleInstalled(bundleContext, addonId)) {
            Bundle bundle = bundleContext.getBundle(addonId);
            try {
                bundle.stop();
                bundle.uninstall();
            } catch (BundleException e) {
                throw new MarketplaceHandlerException("Failed uninstalling bundle: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Iterates over the local cache entries and re-installs bundles that are missing
     *
     * @param bundleContext the {@link BundleContext} to use to look up the bundles
     */
    protected void ensureCachedBundlesAreInstalled(BundleContext bundleContext) {
        if (Files.isDirectory(BUNDLE_CACHE_PATH)) {
            try (Stream<Path> files = Files.list(BUNDLE_CACHE_PATH)) {
                files.filter(Files::isDirectory).map(this::addonIdFromPath)
                        .filter(addonId -> !isBundleInstalled(bundleContext, addonId)).forEach(addonId -> {
                            logger.info("Reinstalling missing marketplace bundle: {}", addonId);
                            try {
                                installFromCache(bundleContext, addonId);
                            } catch (MarketplaceHandlerException e) {
                                logger.warn("Failed reinstalling add-on from cache", e);
                            }
                        });

            } catch (IOException e) {
                logger.warn("Failed to re-install bundles: {}", e.getMessage());
            }
        }
    }

    private String addonIdFromPath(Path path) {
        String pathName = UIDUtils.decode(path.getFileName().toString());
        return pathName.contains(":") ? pathName : "marketplace:" + pathName;
    }

    private Path getAddonCacheDirectory(String addonId) {
        String dir = addonId.startsWith("marketplace:") ? addonId.replace("marketplace:", "")
                : UIDUtils.encode(addonId);
        return BUNDLE_CACHE_PATH.resolve(dir);
    }
}
