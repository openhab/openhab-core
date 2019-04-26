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
import org.eclipse.smarthome.core.types.CommandDescription;
import org.eclipse.smarthome.core.types.CommandDescriptionBuilder;
import org.eclipse.smarthome.core.types.CommandOption;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateDescriptionFragmentBuilder;
import org.eclipse.smarthome.core.types.StateOption;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This OSGi service could be used to localize a {@link ChannelType} using the I18N mechanism of the openHAB
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

    private @NonNullByDefault({}) ThingTypeI18nUtil thingTypeI18nUtil;

    @Reference
    protected void setTranslationProvider(TranslationProvider i18nProvider) {
        this.thingTypeI18nUtil = new ThingTypeI18nUtil(i18nProvider);
    }

    protected void unsetTranslationProvider(TranslationProvider i18nProvider) {
        this.thingTypeI18nUtil = null;
    }

    public @Nullable String createLocalizedStatePattern(final Bundle bundle, String pattern,
            final ChannelTypeUID channelTypeUID, final @Nullable Locale locale) {
        return thingTypeI18nUtil.getChannelStatePattern(bundle, channelTypeUID, pattern, locale);
    }

    public List<StateOption> createLocalizedStateOptions(final Bundle bundle, List<StateOption> stateOptions,
            final ChannelTypeUID channelTypeUID, final @Nullable Locale locale) {
        List<StateOption> localizedOptions = new ArrayList<>();
        for (final StateOption stateOption : stateOptions) {
            String optionLabel = stateOption.getLabel();
            if (optionLabel != null) {
                optionLabel = thingTypeI18nUtil.getChannelStateOption(bundle, channelTypeUID, stateOption.getValue(),
                        optionLabel, locale);
            }
            localizedOptions.add(new StateOption(stateOption.getValue(), optionLabel));
        }
        return localizedOptions;
    }

    public @Nullable StateDescription createLocalizedStateDescription(final Bundle bundle,
            final @Nullable StateDescription state, final ChannelTypeUID channelTypeUID,
            final @Nullable Locale locale) {
        if (state == null) {
            return null;
        }

        String localizedPattern = state.getPattern();
        if (localizedPattern != null) {
            localizedPattern = createLocalizedStatePattern(bundle, localizedPattern, channelTypeUID, locale);
        }
        List<StateOption> localizedOptions = createLocalizedStateOptions(bundle, state.getOptions(), channelTypeUID,
                locale);

        StateDescriptionFragmentBuilder builder = StateDescriptionFragmentBuilder.create(state);
        if (localizedPattern != null) {
            builder.withPattern(localizedPattern);
        }
        return builder.withOptions(localizedOptions).build().toStateDescription();
    }

    public @Nullable CommandDescription createLocalizedCommandDescription(final Bundle bundle,
            final @Nullable CommandDescription command, final ChannelTypeUID channelTypeUID,
            final @Nullable Locale locale) {
        if (command == null) {
            return null;
        }

        CommandDescriptionBuilder commandDescriptionBuilder = CommandDescriptionBuilder.create();
        for (final CommandOption options : command.getCommandOptions()) {
            String optionLabel = options.getLabel();
            if (optionLabel != null) {
                optionLabel = thingTypeI18nUtil.getChannelCommandOption(bundle, channelTypeUID, options.getCommand(),
                        optionLabel, locale);
            }
            commandDescriptionBuilder.withCommandOption(new CommandOption(options.getCommand(), optionLabel));
        }

        return commandDescriptionBuilder.build();
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
                CommandDescription command = createLocalizedCommandDescription(bundle,
                        channelType.getCommandDescription(), channelTypeUID, locale);

                StateChannelTypeBuilder stateBuilder = ChannelTypeBuilder
                        .state(channelTypeUID, label == null ? defaultLabel : label, channelType.getItemType())
                        .isAdvanced(channelType.isAdvanced()).withCategory(channelType.getCategory())
                        .withConfigDescriptionURI(channelType.getConfigDescriptionURI()).withTags(channelType.getTags())
                        .withStateDescription(state).withAutoUpdatePolicy(channelType.getAutoUpdatePolicy())
                        .withCommandDescription(command);
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
