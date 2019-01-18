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
package org.eclipse.smarthome.core.thing.i18n;

import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.i18n.TranslationProvider;
import org.eclipse.smarthome.core.thing.internal.i18n.ChannelI18nUtil;
import org.eclipse.smarthome.core.thing.type.ChannelDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelTypeRegistry;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * This OSGi service could be used to localize a {@link ChannelGroupType} type using the I18N mechanism of the Eclipse
 * SmartHome framework.
 *
 * @author Markus Rathgeb - Move code from XML thing type provider to separate service
 * @author Laurent Garnier - fix localized label and description for channel group definition
 * @author Christoph Weitkamp - factored out from {@link XmlChannelTypeProvider} and {@link XmlChannelGroupTypeProvider}
 * @author Henning Treu - factored out from {@link ThingTypeI18nLocalizationService}
 */
@Component(service = ChannelGroupTypeI18nLocalizationService.class)
@NonNullByDefault
public class ChannelGroupTypeI18nLocalizationService {

    @NonNullByDefault({})
    private ChannelI18nUtil channelI18nUtil;

    @NonNullByDefault({})
    private ThingTypeI18nUtil thingTypeI18nUtil;

    @NonNullByDefault({})
    private ChannelTypeI18nLocalizationService channelTypeI18nLocalizationService;

    @NonNullByDefault({})
    private ChannelTypeRegistry channelTypeRegistry;

    @Reference
    protected void setChannelTypeI18nLocalizationService(
            ChannelTypeI18nLocalizationService channelTypeI18nLocalizationService) {
        this.channelTypeI18nLocalizationService = channelTypeI18nLocalizationService;
    }

    protected void unsetChannelTypeI18nLocalizationService(
            ChannelTypeI18nLocalizationService channelTypeI18nLocalizationService) {
        this.channelTypeI18nLocalizationService = null;
    }

    @Reference
    protected void setChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = channelTypeRegistry;
    }

    protected void unsetChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = null;
    }

    @Reference
    protected void setTranslationProvider(TranslationProvider i18nProvider) {
        this.thingTypeI18nUtil = new ThingTypeI18nUtil(i18nProvider);
    }

    protected void unsetTranslationProvider(TranslationProvider i18nProvider) {
        this.thingTypeI18nUtil = null;
    }

    @Activate
    protected void activate() {
        channelI18nUtil = new ChannelI18nUtil(channelTypeI18nLocalizationService, channelTypeRegistry);
    }

    @Deactivate
    protected void deactivate() {
        channelI18nUtil = null;
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
                .isAdvanced(channelGroupType.isAdvanced()).withCategory(channelGroupType.getCategory())
                .withChannelDefinitions(localizedChannelDefinitions);
        if (description != null) {
            builder.withDescription(description);
        }
        return builder.build();
    }

}
