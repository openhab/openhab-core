/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.thing.internal.i18n.ThingTypeI18nUtil;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.TriggerChannelTypeBuilder;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.CommandDescriptionBuilder;
import org.openhab.core.types.CommandOption;
import org.openhab.core.types.EventDescription;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateOption;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This OSGi service could be used to localize a {@link ChannelType} using the I18N mechanism of the openHAB
 * framework.
 *
 * @author Markus Rathgeb - Initial contribution
 * @author Markus Rathgeb - Move code from XML thing type provider to separate service
 * @author Laurent Garnier - fix localized label and description for channel group definition
 * @author Christoph Weitkamp - factored out from {@link XmlChannelTypeProvider} and {@link XmlChannelGroupTypeProvider}
 * @author Henning Treu - factored out from {@link ThingTypeI18nLocalizationService}
 */
@Component(service = ChannelTypeI18nLocalizationService.class)
@NonNullByDefault
public class ChannelTypeI18nLocalizationService {

    private final ThingTypeI18nUtil thingTypeI18nUtil;

    @Activate
    public ChannelTypeI18nLocalizationService(final @Reference TranslationProvider i18nProvider) {
        this.thingTypeI18nUtil = new ThingTypeI18nUtil(i18nProvider);
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

    public @Nullable StateDescriptionFragment createLocalizedStateDescriptionFragment(final Bundle bundle,
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
        return builder.withOptions(localizedOptions).build();
    }

    public List<CommandOption> createLocalizedCommandOptions(final Bundle bundle, List<CommandOption> commandOptions,
            final ChannelTypeUID channelTypeUID, final @Nullable Locale locale) {
        List<CommandOption> localizedOptions = new ArrayList<>();
        for (final CommandOption commandOption : commandOptions) {
            String optionLabel = commandOption.getLabel();
            if (optionLabel != null) {
                optionLabel = thingTypeI18nUtil.getChannelCommandOption(bundle, channelTypeUID,
                        commandOption.getCommand(), optionLabel, locale);
            }
            localizedOptions.add(new CommandOption(commandOption.getCommand(), optionLabel));
        }
        return localizedOptions;
    }

    public @Nullable CommandDescription createLocalizedCommandDescription(final Bundle bundle,
            final @Nullable CommandDescription command, final ChannelTypeUID channelTypeUID,
            final @Nullable Locale locale) {
        if (command == null) {
            return null;
        }

        List<CommandOption> localizedOptions = createLocalizedCommandOptions(bundle, command.getCommandOptions(),
                channelTypeUID, locale);

        CommandDescriptionBuilder commandDescriptionBuilder = CommandDescriptionBuilder.create();
        return commandDescriptionBuilder.withCommandOptions(localizedOptions).build();
    }

    public ChannelType createLocalizedChannelType(Bundle bundle, ChannelType channelType, @Nullable Locale locale) {
        ChannelTypeUID channelTypeUID = channelType.getUID();
        String defaultLabel = channelType.getLabel();
        String label = thingTypeI18nUtil.getChannelLabel(bundle, channelTypeUID, defaultLabel, locale);
        String description = thingTypeI18nUtil.getChannelDescription(bundle, channelTypeUID,
                channelType.getDescription(), locale);

        final ChannelTypeBuilder<?> builder;
        switch (channelType.getKind()) {
            case STATE:
                StateDescriptionFragment stateDescriptionFragment = createLocalizedStateDescriptionFragment(bundle,
                        channelType.getState(), channelTypeUID, locale);
                CommandDescription command = createLocalizedCommandDescription(bundle,
                        channelType.getCommandDescription(), channelTypeUID, locale);
                String itemType = channelType.getItemType();
                if (itemType == null || itemType.isBlank()) {
                    throw new IllegalArgumentException("If the kind is 'state', the item type must be set!");
                }

                builder = ChannelTypeBuilder.state(channelTypeUID, label == null ? defaultLabel : label, itemType)
                        .withStateDescriptionFragment(stateDescriptionFragment)
                        .withAutoUpdatePolicy(channelType.getAutoUpdatePolicy()).withCommandDescription(command);
                break;
            case TRIGGER:
                EventDescription eventDescription = channelType.getEvent();

                TriggerChannelTypeBuilder triggerBuilder = ChannelTypeBuilder.trigger(channelTypeUID,
                        label == null ? defaultLabel : label);
                if (eventDescription != null) {
                    triggerBuilder.withEventDescription(eventDescription);
                }
                builder = triggerBuilder;
                break;
            default:
                throw new IllegalArgumentException("Kind must not be null or empty!");
        }
        if (description != null) {
            builder.withDescription(description);
        }
        String category = channelType.getCategory();
        if (category != null) {
            builder.withCategory(category);
        }
        URI configDescriptionURI = channelType.getConfigDescriptionURI();
        if (configDescriptionURI != null) {
            builder.withConfigDescriptionURI(configDescriptionURI);
        }
        return builder.isAdvanced(channelType.isAdvanced()).withTags(channelType.getTags()).build();
    }
}
