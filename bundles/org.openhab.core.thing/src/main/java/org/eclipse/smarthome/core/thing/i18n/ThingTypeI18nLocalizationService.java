/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.thing.i18n;

import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.i18n.TranslationProvider;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.internal.i18n.ChannelGroupI18nUtil;
import org.eclipse.smarthome.core.thing.internal.i18n.ChannelI18nUtil;
import org.eclipse.smarthome.core.thing.type.BridgeType;
import org.eclipse.smarthome.core.thing.type.ChannelDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelGroupDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeRegistry;
import org.eclipse.smarthome.core.thing.type.ChannelTypeRegistry;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.thing.type.ThingTypeBuilder;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * This OSGi service could be used to localize a {@link ThingType} using the I18N mechanism of the openHAB
 * framework.
 *
 * @author Markus Rathgeb - Initial contribution
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
    private ChannelGroupI18nUtil channelGroupI18nUtil;

    @NonNullByDefault({})
    private ChannelTypeI18nLocalizationService channelTypeI18nLocalizationService;

    @NonNullByDefault({})
    private ChannelGroupTypeI18nLocalizationService channelGroupTypeI18nLocalizationService;

    @NonNullByDefault({})
    private ChannelTypeRegistry channelTypeRegistry;

    @NonNullByDefault({})
    private ChannelGroupTypeRegistry channelGroupTypeRegistry;

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
    protected void setChannelGroupTypeI18nLocalizationService(
            ChannelGroupTypeI18nLocalizationService channelGroupTypeI18nLocalizationService) {
        this.channelGroupTypeI18nLocalizationService = channelGroupTypeI18nLocalizationService;
    }

    protected void unsetChannelGroupTypeI18nLocalizationService(
            ChannelGroupTypeI18nLocalizationService channelGroupTypeI18nLocalizationService) {
        this.channelGroupTypeI18nLocalizationService = null;
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
        channelGroupI18nUtil = new ChannelGroupI18nUtil(channelGroupTypeI18nLocalizationService,
                channelGroupTypeRegistry);
    }

    @Deactivate
    protected void deactivate() {
        channelI18nUtil = null;
        channelGroupI18nUtil = null;
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

        List<ChannelGroupDefinition> localizedChannelGroupDefinitions = channelGroupI18nUtil
                .createLocalizedChannelGroupDefinitions(bundle, thingType.getChannelGroupDefinitions(),
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
