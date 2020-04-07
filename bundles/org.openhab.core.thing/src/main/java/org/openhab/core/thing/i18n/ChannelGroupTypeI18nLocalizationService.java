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
package org.openhab.core.thing.i18n;

import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.thing.internal.i18n.ChannelI18nUtil;
import org.openhab.core.thing.internal.i18n.ThingTypeI18nUtil;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeBuilder;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This OSGi service could be used to localize a {@link ChannelGroupType} type using the I18N mechanism of the openHAB
 * framework.
 *
 * @author Markus Rathgeb - Initial contribution
 * @author Markus Rathgeb - Move code from XML thing type provider to separate service
 * @author Laurent Garnier - fix localized label and description for channel group definition
 * @author Christoph Weitkamp - factored out from {@link XmlChannelTypeProvider} and {@link XmlChannelGroupTypeProvider}
 * @author Henning Treu - factored out from {@link ThingTypeI18nLocalizationService}
 * @author Christoph Weitkamp - Removed "advanced" attribute
 */
@Component(service = ChannelGroupTypeI18nLocalizationService.class)
@NonNullByDefault
public class ChannelGroupTypeI18nLocalizationService {

    private final ThingTypeI18nUtil thingTypeI18nUtil;
    private final ChannelI18nUtil channelI18nUtil;

    @Activate
    public ChannelGroupTypeI18nLocalizationService(final @Reference TranslationProvider i18nProvider,
            final @Reference ChannelTypeI18nLocalizationService channelTypeI18nLocalizationService,
            final @Reference ChannelTypeRegistry channelTypeRegistry) {
        this.thingTypeI18nUtil = new ThingTypeI18nUtil(i18nProvider);
        this.channelI18nUtil = new ChannelI18nUtil(channelTypeI18nLocalizationService, channelTypeRegistry);
    }

    public ChannelGroupType createLocalizedChannelGroupType(Bundle bundle, ChannelGroupType channelGroupType,
            @Nullable Locale locale) {
        ChannelGroupTypeUID channelGroupTypeUID = channelGroupType.getUID();
        String defaultLabel = channelGroupType.getLabel();
        String label = thingTypeI18nUtil.getChannelGroupLabel(bundle, channelGroupTypeUID, defaultLabel, locale);
        String description = thingTypeI18nUtil.getChannelGroupDescription(bundle, channelGroupTypeUID,
                channelGroupType.getDescription(), locale);

        List<ChannelDefinition> localizedChannelDefinitions = channelI18nUtil.createLocalizedChannelDefinitions(bundle,
                channelGroupType.getChannelDefinitions(),
                channelDefinition -> thingTypeI18nUtil.getChannelLabel(bundle, channelGroupTypeUID, channelDefinition,
                        channelDefinition.getLabel(), locale),
                channelDefinition -> thingTypeI18nUtil.getChannelDescription(bundle, channelGroupTypeUID,
                        channelDefinition, channelDefinition.getDescription(), locale),
                locale);

        ChannelGroupTypeBuilder builder = ChannelGroupTypeBuilder
                .instance(channelGroupTypeUID, label == null ? defaultLabel : label)
                .withCategory(channelGroupType.getCategory()).withChannelDefinitions(localizedChannelDefinitions);
        if (description != null) {
            builder.withDescription(description);
        }
        return builder.build();
    }

}
