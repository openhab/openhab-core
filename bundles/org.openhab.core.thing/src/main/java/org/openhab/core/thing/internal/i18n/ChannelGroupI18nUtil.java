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
package org.openhab.core.thing.internal.i18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.i18n.ChannelGroupTypeI18nLocalizationService;
import org.openhab.core.thing.type.ChannelGroupDefinition;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeRegistry;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.osgi.framework.Bundle;

/**
 * A utility service which localizes {@link ChannelGroupDefinition}.
 * Falls back to a localized {@link ChannelGroupType} for label and description when not given otherwise.
 *
 * @see {@link ThingTypeI18nLocalizationService}
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class ChannelGroupI18nUtil {

    private final ChannelGroupTypeI18nLocalizationService channelGroupTypeI18nLocalizationService;

    private final ChannelGroupTypeRegistry channelGroupTypeRegistry;

    /**
     * Create a new util instance and pass the appropriate dependencies.
     *
     * @param channelGroupTypeI18nLocalizationService an instance of {@link ChannelGroupTypeI18nLocalizationService}.
     * @param channelGroupTypeRegistry the {@link ChannelGroupTypeRegistry}.
     */
    public ChannelGroupI18nUtil(ChannelGroupTypeI18nLocalizationService channelGroupTypeI18nLocalizationService,
            ChannelGroupTypeRegistry channelGroupTypeRegistry) {
        this.channelGroupTypeI18nLocalizationService = channelGroupTypeI18nLocalizationService;
        this.channelGroupTypeRegistry = channelGroupTypeRegistry;
    }

    public List<ChannelGroupDefinition> createLocalizedChannelGroupDefinitions(final Bundle bundle,
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
                    ChannelGroupType localizedChannelGroupType = channelGroupTypeI18nLocalizationService
                            .createLocalizedChannelGroupType(bundle, channelGroupType, locale);
                    if (channelGroupLabel == null) {
                        channelGroupLabel = localizedChannelGroupType.getLabel();
                    }
                    if (channelGroupDescription == null) {
                        channelGroupDescription = localizedChannelGroupType.getDescription();
                    }
                }
            }
            localizedChannelGroupDefinitions.add(new ChannelGroupDefinition(channelGroupDefinition.getId(),
                    channelGroupDefinition.getTypeUID(), channelGroupLabel, channelGroupDescription));
        }
        return localizedChannelGroupDefinitions;
    }
}
