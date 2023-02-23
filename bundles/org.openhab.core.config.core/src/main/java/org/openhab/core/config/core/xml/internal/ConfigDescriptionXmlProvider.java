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
package org.openhab.core.config.core.xml.internal;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.config.core.xml.AbstractXmlConfigDescriptionProvider;
import org.openhab.core.config.core.xml.osgi.XmlDocumentProvider;
import org.osgi.framework.Bundle;

/**
 * The {@link ConfigDescriptionXmlProvider} is responsible managing any created
 * objects by a {@link ConfigDescriptionReader} for a certain bundle.
 * <p>
 * This implementation registers each {@link ConfigDescription} object at the
 * {@link AbstractXmlConfigDescriptionProvider} which
 * is itself registered as {@link ConfigDescriptionProvider} service at the <i>OSGi</i> service registry.
 *
 * @author Michael Grammling - Initial contribution
 *
 * @see ConfigDescriptionXmlProviderFactory
 */
@NonNullByDefault
public class ConfigDescriptionXmlProvider implements XmlDocumentProvider<List<ConfigDescription>> {

    private final Bundle bundle;
    private final AbstractXmlConfigDescriptionProvider configDescriptionProvider;

    public ConfigDescriptionXmlProvider(Bundle bundle, AbstractXmlConfigDescriptionProvider configDescriptionProvider)
            throws IllegalArgumentException {
        this.bundle = bundle;
        this.configDescriptionProvider = configDescriptionProvider;
    }

    @Override
    public synchronized void addingObject(List<ConfigDescription> configDescriptions) {
        this.configDescriptionProvider.addAll(bundle, configDescriptions);
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
