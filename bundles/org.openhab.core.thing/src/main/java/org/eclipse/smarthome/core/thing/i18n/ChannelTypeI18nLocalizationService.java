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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.i18n.TranslationProvider;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.thing.type.StateChannelTypeBuilder;
import org.eclipse.smarthome.core.thing.type.TriggerChannelTypeBuilder;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateOption;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This OSGi service could be used to localize a {@link ChannelType} using the I18N mechanism of the Eclipse SmartHome
 * framework.
 *
 * @author Markus Rathgeb - Move code from XML thing type provider to separate service
 * @author Laurent Garnier - fix localized label and description for channel group definition
 * @author Christoph Weitkamp - factored out from {@link XmlChannelTypeProvider} and {@link XmlChannelGroupTypeProvider}
 * @author Henning Treu - factored out from {@link ThingTypeI18nLocalizationService}
 */
@Component(service = ChannelTypeI18nLocalizationService.class)
@NonNullByDefault
public class ChannelTypeI18nLocalizationService {

    @NonNullByDefault({})
    private ThingTypeI18nUtil thingTypeI18nUtil;

    @Reference
    protected void setTranslationProvider(TranslationProvider i18nProvider) {
        this.thingTypeI18nUtil = new ThingTypeI18nUtil(i18nProvider);
    }

    protected void unsetTranslationProvider(TranslationProvider i18nProvider) {
        this.thingTypeI18nUtil = null;
    }

    private @Nullable StateDescription createLocalizedStateDescription(final Bundle bundle,
            final @Nullable StateDescription state, final ChannelTypeUID channelTypeUID,
            final @Nullable Locale locale) {
        if (state == null) {
            return null;
        }
        String pattern = thingTypeI18nUtil.getChannelStatePattern(bundle, channelTypeUID, state.getPattern(), locale);

        List<StateOption> localizedOptions = new ArrayList<>();
        for (final StateOption options : state.getOptions()) {
            String optionLabel = thingTypeI18nUtil.getChannelStateOption(bundle, channelTypeUID, options.getValue(),
                    options.getLabel(), locale);
            localizedOptions.add(new StateOption(options.getValue(), optionLabel));
        }

        return new StateDescription(state.getMinimum(), state.getMaximum(), state.getStep(), pattern,
                state.isReadOnly(), localizedOptions);
    }

    public ChannelType createLocalizedChannelType(Bundle bundle, ChannelType channelType, @Nullable Locale locale) {
        ChannelTypeUID channelTypeUID = channelType.getUID();
        String defaultLabel = channelType.getLabel();
        String label = thingTypeI18nUtil.getChannelLabel(bundle, channelTypeUID, defaultLabel, locale);
        String description = thingTypeI18nUtil.getChannelDescription(bundle, channelTypeUID,
                channelType.getDescription(), locale);

        switch (channelType.getKind()) {
            case STATE:
                StateDescription state = createLocalizedStateDescription(bundle, channelType.getState(), channelTypeUID,
                        locale);

                StateChannelTypeBuilder stateBuilder = ChannelTypeBuilder
                        .state(channelTypeUID, label == null ? defaultLabel : label, channelType.getItemType())
                        .isAdvanced(channelType.isAdvanced()).withCategory(channelType.getCategory())
                        .withConfigDescriptionURI(channelType.getConfigDescriptionURI()).withTags(channelType.getTags())
                        .withStateDescription(state).withAutoUpdatePolicy(channelType.getAutoUpdatePolicy());
                if (description != null) {
                    stateBuilder.withDescription(description);
                }
                return stateBuilder.build();
            case TRIGGER:
                TriggerChannelTypeBuilder triggerBuilder = ChannelTypeBuilder
                        .trigger(channelTypeUID, label == null ? defaultLabel : label)
                        .isAdvanced(channelType.isAdvanced()).withCategory(channelType.getCategory())
                        .withConfigDescriptionURI(channelType.getConfigDescriptionURI()).withTags(channelType.getTags())
                        .withEventDescription(channelType.getEvent());
                if (description != null) {
                    triggerBuilder.withDescription(description);
                }
                return triggerBuilder.build();
            default:
                return new ChannelType(channelTypeUID, channelType.isAdvanced(), channelType.getItemType(),
                        channelType.getKind(), label == null ? defaultLabel : label, description,
                        channelType.getCategory(), channelType.getTags(), channelType.getState(),
                        channelType.getEvent(), channelType.getConfigDescriptionURI(),
                        channelType.getAutoUpdatePolicy());
        }
    }

}
