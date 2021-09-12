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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.openhab.core.addon.Addon;
import org.openhab.core.addon.marketplace.MarketplaceAddonHandler;
import org.openhab.core.addon.marketplace.MarketplaceHandlerException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link MarketplaceAddonHandler} implementation, which handles add-ons as jar files (specifically, OSGi
 * bundles) and installs
 * them through the standard OSGi bundle installation mechanism.
 * The information, which installed bundle corresponds to which add-on is written to a file in the bundle's data
 * store. It is therefore wiped together with the bundles upon an OSGi "clean".
 * We might want to move this class into a separate bundle in future, when we add support for further add-on types.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Yannick Schaus - refactoring
 *
 */
@Component(immediate = true)
public class CommunityBundleAddonHandler implements MarketplaceAddonHandler {

    // add-on types supported by this handler
    private static final List<String> SUPPORTED_EXT_TYPES = Arrays.asList("binding");

    private static final String BUNDLE_CONTENTTYPE = "application/vnd.openhab.bundle";

    private static final String JAR_DOWNLOAD_URL_PROPERTY = "jar_download_url";

    private final Logger logger = LoggerFactory.getLogger(CommunityBundleAddonHandler.class);

    private Map<String, Long> installedBundles;

    private BundleContext bundleContext;

    @Activate
    protected void activate(BundleContext bundleContext, Map<String, Object> config) {
        this.bundleContext = bundleContext;
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
        return bundleContext.getBundle(id) != null;
    }

    @Override
    public void install(Addon addon) throws MarketplaceHandlerException {
        try {
            URL url = new URL((String) addon.getProperties().get(JAR_DOWNLOAD_URL_PROPERTY));
            InputStream inputStream = url.openStream();
            Bundle bundle = bundleContext.installBundle(addon.getId(), inputStream);
            try {
                bundle.start();
            } catch (BundleException e) {
                logger.warn("Installed bundle, but failed to start it: {}", e.getMessage());
            }
        } catch (BundleException | IOException e) {
            logger.debug("Failed to install bundle from marketplace.", e);
            throw new MarketplaceHandlerException("Bundle cannot be installed: " + e.getMessage());
        }
    }

    @Override
    public void uninstall(Addon addon) throws MarketplaceHandlerException {
        Bundle bundle = bundleContext.getBundle(addon.getId());
        if (bundle != null) {
            try {
                bundle.stop();
                bundle.uninstall();
            } catch (BundleException e) {
                throw new MarketplaceHandlerException("Failed deinstalling bundle: " + e.getMessage());
            }
        } else {
            throw new MarketplaceHandlerException("Id not known.");
        }
    }
}
