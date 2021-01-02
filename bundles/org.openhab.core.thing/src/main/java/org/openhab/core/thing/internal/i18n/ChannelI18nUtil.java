/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.thing.internal.i18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.i18n.ChannelTypeI18nLocalizationService;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelDefinitionBuilder;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.osgi.framework.Bundle;

/**
 * A utility service which localises {@link ChannelDefinition}.
 * Falls back to a localised {@link ChannelType} for label and description when not given otherwise.
 *
 * @see {@link ThingTypeI18nLocalizationService}
 * @see {@link ChannelGroupTypeI18nLocalizationService}
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public class ChannelI18nUtil {

    private final ChannelTypeI18nLocalizationService channelTypeI18nLocalizationService;

    private final ChannelTypeRegistry channelTypeRegistry;

    /**
     * Create a new util instance and pass the appropriate dependencies.
     *
     * @param channelTypeI18nLocalizationService an instance of {@link ChannelTypeI18nLocalizationService}.
     * @param channelTypeRegistry the {@link ChannelTypeRegistry}.
     */
    public ChannelI18nUtil(ChannelTypeI18nLocalizationService channelTypeI18nLocalizationService,
            ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeI18nLocalizationService = channelTypeI18nLocalizationService;
        this.channelTypeRegistry = channelTypeRegistry;
    }

    public List<ChannelDefinition> createLocalizedChannelDefinitions(final Bundle bundle,
            final List<ChannelDefinition> channelDefinitions,
            final Function<ChannelDefinition, @Nullable String> channelLabelResolver,
            final Function<ChannelDefinition, @Nullable String> channelDescriptionResolver,
            final @Nullable Locale locale) {
        List<ChannelDefinition> localizedChannelDefinitions = new ArrayList<>(channelDefinitions.size());
        for (final ChannelDefinition channelDefinition : channelDefinitions) {
            final ChannelDefinitionBuilder builder = new ChannelDefinitionBuilder(channelDefinition);
            String channelLabel = channelLabelResolver.apply(channelDefinition);
            String channelDescription = channelDescriptionResolver.apply(channelDefinition);
            if (channelLabel == null || channelDescription == null) {
                ChannelTypeUID channelTypeUID = channelDefinition.getChannelTypeUID();
                ChannelType channelType = channelTypeRegistry.getChannelType(channelTypeUID, locale);
                if (channelType != null) {
                    ChannelType localizedChannelType = channelTypeI18nLocalizationService
                            .createLocalizedChannelType(bundle, channelType, locale);
                    if (channelLabel == null) {
                        channelLabel = localizedChannelType.getLabel();
                    }
                    if (channelDescription == null) {
                        channelDescription = localizedChannelType.getDescription();
                    }
                }
            }
            builder.withLabel(channelLabel);
            builder.withDescription(channelDescription);
            localizedChannelDefinitions.add(builder.build());
        }
        return localizedChannelDefinitions;
    }
}
