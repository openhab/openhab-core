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
package org.openhab.core.addon.internal.xml;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.xml.AbstractXmlConfigDescriptionProvider;
import org.openhab.core.config.core.xml.osgi.XmlDocumentProvider;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AddonInfoXmlProvider} is responsible managing any created
 * objects by a {@link AddonInfoReader} for a certain bundle.
 * <p>
 * This implementation registers each {@link AddonInfo} object at the {@link XmlAddonInfoProvider} which is itself
 * registered as {@link AddonInfoProvider} service at the <i>OSGi</i> service registry.
 * <p>
 * If there is a {@link ConfigDescription} object within the {@link AddonInfoXmlResult} object, it is added to the
 * {@link AbstractXmlConfigDescriptionProvider} which is itself registered as <i>OSGi</i> service at the service
 * registry.
 *
 * @author Michael Grammling - Initial contribution
 * @author Jan N. Klug - Refactored to cover all add-ons
 */
@NonNullByDefault
public class AddonInfoXmlProvider implements XmlDocumentProvider<AddonInfoXmlResult> {

    private Logger logger = LoggerFactory.getLogger(AddonInfoXmlProvider.class);

    private final Bundle bundle;

    private final XmlAddonInfoProvider addonInfoProvider;
    private final AbstractXmlConfigDescriptionProvider configDescriptionProvider;

    public AddonInfoXmlProvider(Bundle bundle, XmlAddonInfoProvider addonInfoProvider,
            AbstractXmlConfigDescriptionProvider configDescriptionProvider) throws IllegalArgumentException {
        if (bundle == null) {
            throw new IllegalArgumentException("The Bundle must not be null!");
        }

        if (addonInfoProvider == null) {
            throw new IllegalArgumentException("The XmlAddonInfoProvider must not be null!");
        }

        if (configDescriptionProvider == null) {
            throw new IllegalArgumentException("The XmlConfigDescriptionProvider must not be null!");
        }

        this.bundle = bundle;
        this.addonInfoProvider = addonInfoProvider;
        this.configDescriptionProvider = configDescriptionProvider;
    }

    @Override
    public synchronized void addingObject(AddonInfoXmlResult addonInfoXmlResult) {
        ConfigDescription configDescription = addonInfoXmlResult.configDescription();

        if (configDescription != null) {
            try {
                configDescriptionProvider.add(bundle, configDescription);
            } catch (Exception ex) {
                logger.error("Could not register ConfigDescription!", ex);
            }
        }

        AddonInfo addonInfo = AddonInfo.builder(addonInfoXmlResult.addonInfo())
                .withSourceBundle(bundle.getSymbolicName()).build();
        addonInfoProvider.add(bundle, addonInfo);
    }

    @Override
    public void addingFinished() {
        // nothing to do
    }

    @Override
    public synchronized void release() {
        this.addonInfoProvider.removeAll(bundle);
        this.configDescriptionProvider.removeAll(bundle);
    }
}
