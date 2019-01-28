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
package org.eclipse.smarthome.config.xml.internal;

import java.util.List;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionProvider;
import org.eclipse.smarthome.config.xml.AbstractXmlConfigDescriptionProvider;
import org.eclipse.smarthome.config.xml.osgi.XmlDocumentProvider;
import org.osgi.framework.Bundle;

/**
 * The {@link ConfigDescriptionXmlProvider} is responsible managing any created
 * objects by a {@link ConfigDescriptionReader} for a certain bundle.
 * <p>
 * This implementation registers each {@link ConfigDescription} object at the
 * {@link AbstractXmlConfigDescriptionProvider} which
 * is itself registered as {@link ConfigDescriptionProvider} service at the <i>OSGi</i> service registry.
 *
 * @author Michael Grammling - Initial Contribution
 *
 * @see ConfigDescriptionXmlProviderFactory
 */
public class ConfigDescriptionXmlProvider implements XmlDocumentProvider<List<ConfigDescription>> {

    private Bundle bundle;
    private AbstractXmlConfigDescriptionProvider configDescriptionProvider;

    public ConfigDescriptionXmlProvider(Bundle bundle, AbstractXmlConfigDescriptionProvider configDescriptionProvider)
            throws IllegalArgumentException {
        if (bundle == null) {
            throw new IllegalArgumentException("The Bundle must not be null!");
        }

        if (configDescriptionProvider == null) {
            throw new IllegalArgumentException("The XmlConfigDescriptionProvider must not be null!");
        }

        this.bundle = bundle;
        this.configDescriptionProvider = configDescriptionProvider;
    }

    @Override
    public synchronized void addingObject(List<ConfigDescription> configDescriptions) {
        this.configDescriptionProvider.addAll(this.bundle, configDescriptions);
    }

    @Override
    public void addingFinished() {
        // nothing to do
    }

    @Override
    public synchronized void release() {
        this.configDescriptionProvider.removeAll(bundle);
    }

}
