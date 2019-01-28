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
package org.eclipse.smarthome.core.thing.xml.internal;

import java.util.Collection;
import java.util.Locale;

import org.eclipse.smarthome.config.xml.AbstractXmlBasedProvider;
import org.eclipse.smarthome.core.thing.UID;
import org.eclipse.smarthome.core.thing.i18n.ChannelTypeI18nLocalizationService;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * {@link XmlChannelTypeProvider} provides channel types from XML files.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Kai Kreuzer - fixed concurrency issues
 * @author Simon Kaufmann - factored out common aspects into {@link AbstractXmlBasedProvider}
 * @author Henning Treu - QuantityType implementation
 * @author Christoph Weitkamp - factored out common aspects into ThingTypeI18nLocalizationService
 */
@Component(property = { "esh.scope=core.xml.channels" })
public class XmlChannelTypeProvider extends AbstractXmlBasedProvider<UID, ChannelType> implements ChannelTypeProvider {

    private ChannelTypeI18nLocalizationService channelTypeI18nLocalizationService;

    @Override
    public ChannelType getChannelType(ChannelTypeUID channelTypeUID, Locale locale) {
        return get(channelTypeUID, locale);
    }

    @Override
    public synchronized Collection<ChannelType> getChannelTypes(Locale locale) {
        return getAll(locale);
    }

    @Reference
    public void setChannelTypeI18nLocalizationService(
            final ChannelTypeI18nLocalizationService channelTypeI18nLocalizationService) {
        this.channelTypeI18nLocalizationService = channelTypeI18nLocalizationService;
    }

    public void unsetChannelTypeI18nLocalizationService(
            final ChannelTypeI18nLocalizationService channelTypeI18nLocalizationService) {
        this.channelTypeI18nLocalizationService = null;
    }

    @Override
    protected ChannelType localize(Bundle bundle, ChannelType channelType, Locale locale) {
        if (channelTypeI18nLocalizationService == null) {
            return null;
        }
        return channelTypeI18nLocalizationService.createLocalizedChannelType(bundle, channelType, locale);
    }

}
