/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.binding.xml.internal;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.xml.AbstractXmlConfigDescriptionProvider;
import org.eclipse.smarthome.config.xml.osgi.XmlDocumentProvider;
import org.eclipse.smarthome.core.binding.BindingInfo;
import org.eclipse.smarthome.core.binding.BindingInfoProvider;
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
 * @author Michael Grammling - Initial Contribution
 *
 * @see BindingInfoXmlProviderFactory
 */
public class BindingInfoXmlProvider implements XmlDocumentProvider<BindingInfoXmlResult> {

    private Logger logger = LoggerFactory.getLogger(BindingInfoXmlProvider.class);

    private Bundle bundle;

    private XmlBindingInfoProvider bindingInfoProvider;
    private AbstractXmlConfigDescriptionProvider configDescriptionProvider;

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
    public synchronized void addingObject(BindingInfoXmlResult bindingInfoXmlResult) {
        if (bindingInfoXmlResult != null) {
            ConfigDescription configDescription = bindingInfoXmlResult.getConfigDescription();

            if (configDescription != null) {
                try {
                    this.configDescriptionProvider.add(this.bundle, configDescription);
                } catch (Exception ex) {
                    this.logger.error("Could not register ConfigDescription!", ex);
                }
            }

            BindingInfo bindingInfo = bindingInfoXmlResult.getBindingInfo();
            this.bindingInfoProvider.add(bundle, bindingInfo);
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
