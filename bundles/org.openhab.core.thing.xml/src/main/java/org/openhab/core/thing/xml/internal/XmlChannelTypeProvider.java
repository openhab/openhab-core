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
package org.openhab.core.thing.xml.internal;

import java.util.Collection;
import java.util.Locale;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.xml.AbstractXmlBasedProvider;
import org.openhab.core.thing.UID;
import org.openhab.core.thing.i18n.ChannelTypeI18nLocalizationService;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeProvider;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
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

    private final ChannelTypeI18nLocalizationService channelTypeI18nLocalizationService;

    @Activate
    public XmlChannelTypeProvider(
            final @Reference ChannelTypeI18nLocalizationService channelTypeI18nLocalizationService) {
        this.channelTypeI18nLocalizationService = channelTypeI18nLocalizationService;
    }

    @Override
    public @Nullable ChannelType getChannelType(ChannelTypeUID channelTypeUID, @Nullable Locale locale) {
        return get(channelTypeUID, locale);
    }

    @Override
    public synchronized Collection<ChannelType> getChannelTypes(@Nullable Locale locale) {
        return getAll(locale);
    }

    @Override
    protected @Nullable ChannelType localize(Bundle bundle, ChannelType channelType, @Nullable Locale locale) {
        return channelTypeI18nLocalizationService.createLocalizedChannelType(bundle, channelType, locale);
    }
}
