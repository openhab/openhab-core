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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.i18n.TranslationProvider;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.internal.i18n.ChannelI18nUtil;
import org.eclipse.smarthome.core.thing.type.BridgeType;
import org.eclipse.smarthome.core.thing.type.ChannelDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelGroupDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeRegistry;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelTypeRegistry;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.thing.type.ThingTypeBuilder;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * This OSGi service could be used to localize a {@link ThingType} using the I18N mechanism of the Eclipse SmartHome
 * framework.
 *
 * @author Markus Rathgeb - Move code from XML thing type provider to separate service
 * @author Laurent Garnier - fix localized label and description for channel group definition
 * @author Christoph Weitkamp - factored out from {@link XmlChannelTypeProvider} and {@link XmlChannelGroupTypeProvider}
 */
@Component(service = ThingTypeI18nLocalizationService.class)
@NonNullByDefault
public class ThingTypeI18nLocalizationService {

    @NonNullByDefault({})
    private ThingTypeI18nUtil thingTypeI18nUtil;

    @NonNullByDefault({})
    private ChannelI18nUtil channelI18nUtil;

    @NonNullByDefault({})
    private ChannelGroupTypeRegistry channelGroupTypeRegistry;

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

    @Reference
    protected void setChannelGroupTypeRegistry(ChannelGroupTypeRegistry channelGroupTypeRegistry) {
        this.channelGroupTypeRegistry = channelGroupTypeRegistry;
    }

    protected void unsetChannelGroupTypeRegistry(ChannelGroupTypeRegistry channelGroupTypeRegistry) {
        this.channelGroupTypeRegistry = null;
    }

    @Activate
    protected void activate() {
        channelI18nUtil = new ChannelI18nUtil(channelTypeI18nLocalizationService, channelTypeRegistry);
    }

    @Deactivate
    protected void deactivate() {
        channelI18nUtil = null;
    }

    private List<ChannelGroupDefinition> createLocalizedChannelGroupDefinitions(final Bundle bundle,
            final List<ChannelGroupDefinition> channelGroupDefinitions,
            final Function<ChannelGroupDefinition, @Nullable String> channelGroupLabelResolver,
            final Function<ChannelGroupDefinition, @Nullable String> channelGroupDescriptionResolver,
            final @Nullable Locale locale) {
        List<ChannelGroupDefinition> localizedChannelGroupDefinitions = new ArrayList<>(channelGroupDefinitions.size());
        for (final ChannelGroupDefinition channelGroupDefinition : channelGroupDefinitions) {
            String channelGroupLabel = channelGroupLabelResolver.apply(channelGroupDefinition);
            String channelGroupDescription = channelGroupDescriptionResolver.apply(channelGroupDefinition);
            if (channelGroupLabel == null || channelGroupDescription == null) {
                ChannelGroupTypeUID channelGroupTypeUID = channelGroupDefinition.getTypeUID();
                ChannelGroupType channelGroupType = channelGroupTypeRegistry.getChannelGroupType(channelGroupTypeUID,
                        locale);
                if (channelGroupType != null) {
                    if (channelGroupLabel == null) {
                        channelGroupLabel = thingTypeI18nUtil.getChannelGroupLabel(bundle, channelGroupTypeUID,
                                channelGroupType.getLabel(), locale);
                    }
                    if (channelGroupDescription == null) {
                        channelGroupDescription = thingTypeI18nUtil.getChannelGroupDescription(bundle,
                                channelGroupTypeUID, channelGroupType.getDescription(), locale);
                    }
                }
            }
            localizedChannelGroupDefinitions.add(new ChannelGroupDefinition(channelGroupDefinition.getId(),
                    channelGroupDefinition.getTypeUID(), channelGroupLabel, channelGroupDescription));
        }
        return localizedChannelGroupDefinitions;
    }

    public ThingType createLocalizedThingType(Bundle bundle, ThingType thingType, @Nullable Locale locale) {
        ThingTypeUID thingTypeUID = thingType.getUID();
        String label = thingTypeI18nUtil.getLabel(bundle, thingTypeUID, thingType.getLabel(), locale);
        String description = thingTypeI18nUtil.getDescription(bundle, thingTypeUID, thingType.getDescription(), locale);

        List<ChannelDefinition> localizedChannelDefinitions = channelI18nUtil.createLocalizedChannelDefinitions(bundle,
                thingType.getChannelDefinitions(),
                channelDefinition -> thingTypeI18nUtil.getChannelLabel(bundle, thingTypeUID, channelDefinition,
                        channelDefinition.getLabel(), locale),
                channelDefinition -> thingTypeI18nUtil.getChannelDescription(bundle, thingTypeUID, channelDefinition,
                        channelDefinition.getDescription(), locale),
                locale);

        List<ChannelGroupDefinition> localizedChannelGroupDefinitions = createLocalizedChannelGroupDefinitions(bundle,
                thingType.getChannelGroupDefinitions(),
                channelGroupDefinition -> thingTypeI18nUtil.getChannelGroupLabel(bundle, thingTypeUID,
                        channelGroupDefinition, channelGroupDefinition.getLabel(), locale),
                channelGroupDefinition -> thingTypeI18nUtil.getChannelGroupDescription(bundle, thingTypeUID,
                        channelGroupDefinition, channelGroupDefinition.getDescription(), locale),
                locale);

        ThingTypeBuilder builder = ThingTypeBuilder.instance(thingType);
        if (label != null) {
            builder.withLabel(label);
        }
        if (description != null) {
            builder.withDescription(description);
        }
        builder.withChannelDefinitions(localizedChannelDefinitions)
                .withChannelGroupDefinitions(localizedChannelGroupDefinitions);

        if (thingType instanceof BridgeType) {
            return builder.buildBridge();
        }

        return builder.build();
    }

}
