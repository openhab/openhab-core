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
import org.openhab.core.thing.i18n.ChannelGroupTypeI18nLocalizationService;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeProvider;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * {@link XmlChannelGroupTypeProvider} provides channel group types from XML files.
 *
 * @author Simon Kaufmann - Initial contribution
 * @author Christoph Weitkamp - factored out common aspects into ThingTypeI18nLocalizationService
 */
@Component(property = { "esh.scope=core.xml.channelGroups" })
public class XmlChannelGroupTypeProvider extends AbstractXmlBasedProvider<UID, ChannelGroupType>
        implements ChannelGroupTypeProvider {

    private final ChannelGroupTypeI18nLocalizationService channelGroupTypeI18nLocalizationService;

    @Activate
    public XmlChannelGroupTypeProvider(
            final @Reference ChannelGroupTypeI18nLocalizationService channelGroupTypeI18nLocalizationService) {
        this.channelGroupTypeI18nLocalizationService = channelGroupTypeI18nLocalizationService;
    }

    @Override
    public @Nullable ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID,
            @Nullable Locale locale) {
        return get(channelGroupTypeUID, locale);
    }

    @Override
    public Collection<ChannelGroupType> getChannelGroupTypes(@Nullable Locale locale) {
        return getAll(locale);
    }

    @Override
    protected @Nullable ChannelGroupType localize(Bundle bundle, ChannelGroupType channelGroupType,
            @Nullable Locale locale) {
        return channelGroupTypeI18nLocalizationService.createLocalizedChannelGroupType(bundle, channelGroupType,
                locale);
    }
}
