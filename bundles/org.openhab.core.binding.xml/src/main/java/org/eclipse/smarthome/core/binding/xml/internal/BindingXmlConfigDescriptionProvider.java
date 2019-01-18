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
import org.eclipse.smarthome.config.core.ConfigDescriptionProvider;
import org.eclipse.smarthome.config.core.i18n.ConfigI18nLocalizationService;
import org.eclipse.smarthome.config.xml.AbstractXmlConfigDescriptionProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Provides {@link ConfigDescription}s for bindings which are read from XML files.
 *
 * @author Simon Kaufmann - initial contribution and API
 *
 */
@Component(service = ConfigDescriptionProvider.class, immediate = true, property = { "esh.scope=core.xml.binding" })
public class BindingXmlConfigDescriptionProvider extends AbstractXmlConfigDescriptionProvider {

    private ConfigI18nLocalizationService configI18nLocalizerService;

    @Reference
    public void setConfigI18nLocalizerService(ConfigI18nLocalizationService configI18nLocalizerService) {
        this.configI18nLocalizerService = configI18nLocalizerService;
    }

    public void unsetConfigI18nLocalizerService(ConfigI18nLocalizationService configI18nLocalizerService) {
        this.configI18nLocalizerService = null;
    }

    @Override
    protected ConfigI18nLocalizationService getConfigI18nLocalizerService() {
        return configI18nLocalizerService;
    }

}
