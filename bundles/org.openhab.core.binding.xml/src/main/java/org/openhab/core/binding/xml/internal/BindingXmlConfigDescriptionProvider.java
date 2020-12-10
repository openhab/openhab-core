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
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.config.core.i18n.ConfigI18nLocalizationService;
import org.openhab.core.config.xml.AbstractXmlConfigDescriptionProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Provides {@link ConfigDescription}s for bindings which are read from XML files.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@Component(service = ConfigDescriptionProvider.class, immediate = true, property = { "openhab.scope=core.xml.binding" })
@NonNullByDefault
public class BindingXmlConfigDescriptionProvider extends AbstractXmlConfigDescriptionProvider {

    private final ConfigI18nLocalizationService configI18nService;

    @Activate
    public BindingXmlConfigDescriptionProvider(final @Reference ConfigI18nLocalizationService configI18nService) {
        this.configI18nService = configI18nService;
    }

    @Override
    protected ConfigI18nLocalizationService getConfigI18nLocalizerService() {
        return configI18nService;
    }
}
