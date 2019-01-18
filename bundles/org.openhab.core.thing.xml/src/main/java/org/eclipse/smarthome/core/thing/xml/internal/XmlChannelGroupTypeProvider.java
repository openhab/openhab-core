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
import org.eclipse.smarthome.core.thing.i18n.ChannelGroupTypeI18nLocalizationService;
import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * {@link XmlChannelGroupTypeProvider} provides channel group types from XML files.
 *
 * @author Simon Kaufmann - factored out from {@link XmlChannelTypeProvider}
 * @author Christoph Weitkamp - factored out common aspects into ThingTypeI18nLocalizationService
 */
@Component(property = { "esh.scope=core.xml.channelGroups" })
public class XmlChannelGroupTypeProvider extends AbstractXmlBasedProvider<UID, ChannelGroupType>
        implements ChannelGroupTypeProvider {

    private ChannelGroupTypeI18nLocalizationService channelGroupTypeI18nLocalizationService;

    @Override
    public ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID, Locale locale) {
        return get(channelGroupTypeUID, locale);
    }

    @Override
    public Collection<ChannelGroupType> getChannelGroupTypes(Locale locale) {
        return getAll(locale);
    }

    @Reference
    public void setChannelGroupTypeI18nLocalizationService(
            final ChannelGroupTypeI18nLocalizationService channelGroupTypeI18nLocalizationService) {
        this.channelGroupTypeI18nLocalizationService = channelGroupTypeI18nLocalizationService;
    }

    public void unsetChannelGroupTypeI18nLocalizationService(
            final ChannelGroupTypeI18nLocalizationService channelGroupTypeI18nLocalizationService) {
        this.channelGroupTypeI18nLocalizationService = null;
    }

    @Override
    protected ChannelGroupType localize(Bundle bundle, ChannelGroupType channelGroupType, Locale locale) {
        if (channelGroupTypeI18nLocalizationService == null) {
            return null;
        }
        return channelGroupTypeI18nLocalizationService.createLocalizedChannelGroupType(bundle, channelGroupType,
                locale);
    }

}
