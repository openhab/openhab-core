/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.binding.xml.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.binding.BindingInfo;
import org.openhab.core.binding.BindingInfoProvider;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.xml.AbstractXmlConfigDescriptionProvider;
import org.openhab.core.config.xml.osgi.XmlDocumentProvider;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link BindingInfoXmlProvider} is responsible managing any created
 * objects by a {@link BindingInfoReader} for a certain bundle.
 * <p>
 * This implementation registers each {@link BindingInfo} object at the {@link XmlBindingInfoProvider} which is itself
 * registered as {@link BindingInfoProvider} service at the <i>OSGi</i> service registry.
 * <p>
 * If there is a {@link ConfigDescription} object within the {@link BindingInfoXmlResult} object, it is added to the
 * {@link AbstractXmlConfigDescriptionProvider} which is itself registered as <i>OSGi</i> service at the service
 * registry.
 *
 * @author Michael Grammling - Initial contribution
 *
 * @see BindingInfoXmlProviderFactory
 */
@NonNullByDefault
public class BindingInfoXmlProvider implements XmlDocumentProvider<BindingInfoXmlResult> {

    private Logger logger = LoggerFactory.getLogger(BindingInfoXmlProvider.class);

    private final Bundle bundle;

    private final XmlBindingInfoProvider bindingInfoProvider;
    private final AbstractXmlConfigDescriptionProvider configDescriptionProvider;

    public BindingInfoXmlProvider(Bundle bundle, XmlBindingInfoProvider bindingInfoProvider,
            AbstractXmlConfigDescriptionProvider configDescriptionProvider) throws IllegalArgumentException {
        if (bundle == null) {
            throw new IllegalArgumentException("The Bundle must not be null!");
        }

        if (bindingInfoProvider == null) {
            throw new IllegalArgumentException("The XmlBindingInfoProvider must not be null!");
        }

        if (configDescriptionProvider == null) {
            throw new IllegalArgumentException("The XmlConfigDescriptionProvider must not be null!");
        }

        this.bundle = bundle;
        this.bindingInfoProvider = bindingInfoProvider;
        this.configDescriptionProvider = configDescriptionProvider;
    }

    @Override
    public synchronized void addingObject(@Nullable BindingInfoXmlResult bindingInfoXmlResult) {
        if (bindingInfoXmlResult != null) {
            ConfigDescription configDescription = bindingInfoXmlResult.getConfigDescription();

            if (configDescription != null) {
                try {
                    configDescriptionProvider.add(bundle, configDescription);
                } catch (Exception ex) {
                    logger.error("Could not register ConfigDescription!", ex);
                }
            }

            bindingInfoProvider.add(bundle, bindingInfoXmlResult.getBindingInfo());
        }
    }

    @Override
    public void addingFinished() {
        // nothing to do
    }

    @Override
    public synchronized void release() {
        this.bindingInfoProvider.removeAll(bundle);
        this.configDescriptionProvider.removeAll(bundle);
    }
}
