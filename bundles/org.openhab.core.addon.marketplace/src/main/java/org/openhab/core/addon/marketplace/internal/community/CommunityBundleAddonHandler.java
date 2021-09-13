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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.openhab.core.addon.Addon;
import org.openhab.core.addon.marketplace.MarketplaceAddonHandler;
import org.openhab.core.addon.marketplace.MarketplaceBundleInstaller;
import org.openhab.core.addon.marketplace.MarketplaceHandlerException;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
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
public class CommunityBundleAddonHandler extends MarketplaceBundleInstaller implements MarketplaceAddonHandler {

    // add-on types supported by this handler
    private static final List<String> SUPPORTED_EXT_TYPES = Arrays.asList("binding");

    private static final String BUNDLE_CONTENTTYPE = "application/vnd.openhab.bundle";

    private static final String JAR_DOWNLOAD_URL_PROPERTY = "jar_download_url";

    private final Logger logger = LoggerFactory.getLogger(CommunityBundleAddonHandler.class);

    private BundleContext bundleContext;

    @Activate
    protected void activate(BundleContext bundleContext, Map<String, Object> config) {
        this.bundleContext = bundleContext;
        ensureCachedBundlesAreInstalled(bundleContext);
    }

    @Deactivate
    protected void deactivate() {
        this.bundleContext = null;
    }

    @Override
    public boolean supports(String type, String contentType) {
        // we support only certain extension types, and only as pure OSGi bundles
        return SUPPORTED_EXT_TYPES.contains(type) && contentType.equals(BUNDLE_CONTENTTYPE);
    }

    @Override
    public boolean isInstalled(String id) {
        return isBundleInstalled(bundleContext, id);
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
    public void uninstall(Addon addon) throws MarketplaceHandlerException {
        uninstallBundle(bundleContext, addon.getId());
    }
}
