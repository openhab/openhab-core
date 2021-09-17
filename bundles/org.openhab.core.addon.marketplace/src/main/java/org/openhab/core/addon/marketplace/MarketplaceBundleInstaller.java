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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.OpenHAB;
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

    private static final String BUNDLE_CACHE_PATH = OpenHAB.getUserDataFolder() + File.separator + "marketplace"
            + File.separator + "bundles";

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
            File addonFile = new File(getAddonCacheDirectory(addonId), fileName);
            addonFile.getParentFile().mkdirs();

            InputStream source = sourceUrl.openStream();
            Path outputPath = Path.of(addonFile.toURI());
            Files.copy(source, outputPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | URISyntaxException e) {
            throw new MarketplaceHandlerException("Cannot copy bundle to local cache: " + e.getMessage());
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
        File addonPath = getAddonCacheDirectory(addonId);
        if (addonPath.exists() && addonPath.isDirectory()) {
            File[] bundleFiles = addonPath.listFiles();
            if (bundleFiles.length != 1) {
                throw new MarketplaceHandlerException(
                        "The local cache folder doesn't contain a single file: " + addonPath.toString());
            }

            try (FileInputStream fileInputStream = new FileInputStream(bundleFiles[0])) {
                Bundle bundle = bundleContext.installBundle(addonId, fileInputStream);
                try {
                    bundle.start();
                } catch (BundleException e) {
                    logger.warn("The marketplace bundle was successfully installed but doesn't start: {}",
                            e.getMessage());
                }

            } catch (IOException | BundleException e) {
                throw new MarketplaceHandlerException(
                        "Cannot install bundle from marketplace cache: " + e.getMessage());
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
        File addonPath = getAddonCacheDirectory(addonId);
        if (addonPath.exists() && addonPath.isDirectory()) {
            for (File bundleFile : addonPath.listFiles()) {
                bundleFile.delete();
            }
        }
        addonPath.delete();

        if (isBundleInstalled(bundleContext, addonId)) {
            Bundle bundle = bundleContext.getBundle(addonId);
            try {
                bundle.stop();
                bundle.uninstall();
            } catch (BundleException e) {
                throw new MarketplaceHandlerException("Failed uninstalling bundle: " + e.getMessage());
            }
        }
    }

    /**
     * Iterates over the local cache entries and re-installs bundles that are missing
     *
     * @param bundleContext the {@link BundleContext} to use to look up the bundles
     */
    protected void ensureCachedBundlesAreInstalled(BundleContext bundleContext) {
        File addonPath = new File(BUNDLE_CACHE_PATH);
        if (addonPath.exists() && addonPath.isDirectory()) {
            for (File bundleFile : addonPath.listFiles()) {
                if (bundleFile.isDirectory()) {
                    String addonId = "marketplace:" + bundleFile.getName();
                    if (!isBundleInstalled(bundleContext, addonId)) {
                        logger.info("Reinstalling missing marketplace bundle: {}", addonId);
                        try {
                            installFromCache(bundleContext, addonId);
                        } catch (MarketplaceHandlerException e) {
                            logger.warn("Failed reinstalling add-on from cache", e);
                        }
                    }
                }
                bundleFile.delete();
            }
        }
    }

    protected File getAddonCacheDirectory(String addonId) {
        return new File(BUNDLE_CACHE_PATH + File.separator + addonId.replace("marketplace:", ""));
    }
}
