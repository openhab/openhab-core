/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.tools.i18n.plugin;

import static org.openhab.core.tools.i18n.plugin.Translations.TranslationsEntry.entry;
import static org.openhab.core.tools.i18n.plugin.Translations.TranslationsGroup.group;
import static org.openhab.core.tools.i18n.plugin.Translations.TranslationsSection.section;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.binding.BindingInfo;
import org.openhab.core.binding.xml.internal.BindingInfoXmlResult;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameterGroup;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelGroupDefinition;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.xml.internal.ChannelGroupTypeXmlResult;
import org.openhab.core.thing.xml.internal.ChannelTypeXmlResult;
import org.openhab.core.thing.xml.internal.ThingTypeXmlResult;
import org.openhab.core.tools.i18n.plugin.Translations.TranslationsEntry;
import org.openhab.core.tools.i18n.plugin.Translations.TranslationsGroup;
import org.openhab.core.tools.i18n.plugin.Translations.TranslationsSection;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.StateDescription;

/**
 * Converts XML based {@link BundleInfo} to {@link Translations}.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public class XmlToTranslationsConverter {

    public Translations convert(BundleInfo bundleInfo) {
        BindingInfoXmlResult bindingInfoXml = bundleInfo.getBindingInfoXml();
        return bindingInfoXml == null ? configTranslations(bundleInfo) : bindingTranslations(bundleInfo);
    }

    private Translations bindingTranslations(BundleInfo bundleInfo) {
        return Translations.translations( //
                bindingSection(bundleInfo), //
                bindingConfigSection(bundleInfo), //
                thingTypesSection(bundleInfo), //
                thingTypesConfigSection(bundleInfo), //
                channelGroupTypesSection(bundleInfo), //
                channelTypesSection(bundleInfo), //
                channelTypesConfigSection(bundleInfo));
    }

    private Translations configTranslations(BundleInfo bundleInfo) {
        Builder<TranslationsGroup> groupsBuilder = Stream.builder();

        bundleInfo.getConfigDescriptions().stream().map(configDescription -> {
            Object[] uid = configDescription.getUID().toString().split(":");
            String configKeyPrefix = String.format("%s.config" + ".%s".repeat(uid.length - 1), uid);
            Builder<TranslationsGroup> streamBuilder = Stream.builder();
            configDescriptionGroupParameters(configKeyPrefix, configDescription.getParameterGroups())
                    .forEach(streamBuilder::add);
            configDescriptionParameters(configKeyPrefix, configDescription.getParameters()).forEach(streamBuilder::add);
            return streamBuilder.build();
        }).reduce(Stream::concat).orElseGet(Stream::empty).forEach(groupsBuilder::add);

        return Translations.translations(section(groupsBuilder.build()));
    }

    private TranslationsSection bindingSection(BundleInfo bundleInfo) {
        String header = "binding";
        BindingInfoXmlResult bindingInfoXml = bundleInfo.getBindingInfoXml();
        if (bindingInfoXml == null) {
            return section(header);
        }

        BindingInfo bindingInfo = bindingInfoXml.getBindingInfo();
        String bindingId = bundleInfo.getBindingId();

        String keyPrefix = String.format("binding.%s", bindingId);

        return section(header, group( //
                entry(String.format("%s.name", keyPrefix), bindingInfo.getName()),
                entry(String.format("%s.description", keyPrefix), bindingInfo.getDescription()) //
        ));
    }

    private TranslationsSection bindingConfigSection(BundleInfo bundleInfo) {
        String header = "binding config";
        BindingInfoXmlResult bindingInfoXml = bundleInfo.getBindingInfoXml();
        if (bindingInfoXml == null) {
            return section(header);
        }
        ConfigDescription bindingConfig = bindingInfoXml.getConfigDescription();
        if (bindingConfig == null) {
            return section(header);
        }

        String keyPrefix = String.format("binding.config.%s", bundleInfo.getBindingId());
        return section(header,
                Stream.concat(configDescriptionGroupParameters(keyPrefix, bindingConfig.getParameterGroups()),
                        configDescriptionParameters(keyPrefix, bindingConfig.getParameters())));
    }

    private TranslationsSection thingTypesSection(BundleInfo bundleInfo) {
        String header = "thing types";
        List<ThingTypeXmlResult> thingTypesXml = bundleInfo.getThingTypesXml();
        if (thingTypesXml.isEmpty()) {
            return section(header);
        }

        String bindingId = bundleInfo.getBindingId();
        String keyPrefix = String.format("thing-type.%s", bindingId);

        return section(header, thingTypesXml.stream().map(thingTypeXml -> {
            ThingType thingType = thingTypeXml.toThingType();
            String thingId = thingType.getUID().getId();

            Builder<TranslationsEntry> entriesBuilder = Stream.builder();
            entriesBuilder.add(entry(String.format("%s.%s.label", keyPrefix, thingId), thingType.getLabel()));
            entriesBuilder
                    .add(entry(String.format("%s.%s.description", keyPrefix, thingId), thingType.getDescription()));

            thingType.getChannelDefinitions().stream() //
                    .sorted(Comparator.comparing(ChannelDefinition::getId)) //
                    .forEach(channelDefinition -> {
                        String label = channelDefinition.getLabel();
                        if (label != null) {
                            entriesBuilder.add(entry(String.format("%s.%s.channel.%s.label", keyPrefix, thingId,
                                    channelDefinition.getId()), label));
                        }

                        String description = channelDefinition.getDescription();
                        if (description != null) {
                            entriesBuilder.add(entry(String.format("%s.%s.channel.%s.description", keyPrefix, thingId,
                                    channelDefinition.getId()), description));
                        }
                    });

            thingType.getChannelGroupDefinitions().stream() //
                    .sorted(Comparator.comparing(ChannelGroupDefinition::getId)) //
                    .forEach(channelGroupDefinition -> {
                        String label = channelGroupDefinition.getLabel();
                        if (label != null) {
                            entriesBuilder.add(entry(String.format("%s.%s.group.%s.label", keyPrefix, thingId,
                                    channelGroupDefinition.getId()), label));
                        }

                        String description = channelGroupDefinition.getDescription();
                        if (description != null) {
                            entriesBuilder.add(entry(String.format("%s.%s.group.%s.description", keyPrefix, thingId,
                                    channelGroupDefinition.getId()), description));
                        }
                    });

            return group(entriesBuilder.build());
        }));
    }

    private TranslationsSection thingTypesConfigSection(BundleInfo bundleInfo) {
        String header = "thing types config";
        List<ThingTypeXmlResult> thingTypesXml = bundleInfo.getThingTypesXml();
        if (thingTypesXml.isEmpty()) {
            return section(header);
        }

        Stream<ConfigDescription> configDescriptionStream = Stream.concat( //
                thingTypesXml.stream() //
                        .map(ThingTypeXmlResult::getConfigDescription) //
                        .filter(Objects::nonNull),
                thingTypesXml.stream() //
                        .map(ThingTypeXmlResult::toThingType) //
                        .map(ThingType::getConfigDescriptionURI) //
                        .distinct() //
                        .map(bundleInfo::getConfigDescription) //
                        .filter(Optional::isPresent) //
                        .map(Optional::get));

        Builder<TranslationsGroup> groupsBuilder = Stream.builder();

        configDescriptionStream.map(configDescription -> {
            String configKeyPrefix = String.format("%s.config.%s.%s",
                    (Object[]) configDescription.getUID().toString().split(":"));
            Builder<TranslationsGroup> streamBuilder = Stream.builder();
            configDescriptionGroupParameters(configKeyPrefix, configDescription.getParameterGroups())
                    .forEach(streamBuilder::add);
            configDescriptionParameters(configKeyPrefix, configDescription.getParameters()).forEach(streamBuilder::add);
            return streamBuilder.build();
        }).reduce(Stream::concat).orElseGet(Stream::empty).forEach(groupsBuilder::add);

        return section(header, groupsBuilder.build());
    }

    private TranslationsSection channelGroupTypesSection(BundleInfo bundleInfo) {
        String header = "channel group types";
        List<ChannelGroupTypeXmlResult> channelGroupTypesXml = bundleInfo.getChannelGroupTypesXml();
        if (channelGroupTypesXml.isEmpty()) {
            return section(header);
        }

        String keyPrefix = String.format("channel-group-type.%s", bundleInfo.getBindingId());

        return section(header, channelGroupTypesXml.stream().map(ChannelGroupTypeXmlResult::toChannelGroupType)
                .map(channelGroupType -> {
                    String groupTypeKeyPrefix = String.format("%s.%s", keyPrefix,
                            channelGroupType.getUID().toString().split(":")[1]);

                    Builder<TranslationsEntry> entriesBuilder = Stream.builder();

                    entriesBuilder
                            .add(entry(String.format("%s.label", groupTypeKeyPrefix), channelGroupType.getLabel()));

                    entriesBuilder.add(entry(String.format("%s.description", groupTypeKeyPrefix),
                            channelGroupType.getDescription()));

                    channelGroupType.getChannelDefinitions().stream() //
                            .sorted(Comparator.comparing(ChannelDefinition::getId)) //
                            .forEach(channelDefinition -> {
                                String label = channelDefinition.getLabel();
                                if (label != null) {
                                    entriesBuilder.add(entry(String.format("%s.channel.%s.label", groupTypeKeyPrefix,
                                            channelDefinition.getId()), label));
                                }

                                String description = channelDefinition.getDescription();
                                if (description != null) {
                                    entriesBuilder.add(entry(String.format("%s.channel.%s.description",
                                            groupTypeKeyPrefix, channelDefinition.getId()), description));
                                }
                            });

                    return group(entriesBuilder.build());
                }));
    }

    private TranslationsSection channelTypesSection(BundleInfo bundleInfo) {
        String header = "channel types";
        List<ChannelTypeXmlResult> channelTypesXml = bundleInfo.getChannelTypesXml();
        if (channelTypesXml.isEmpty()) {
            return section(header);
        }

        String keyPrefix = String.format("channel-type.%s", bundleInfo.getBindingId());

        return section(header, channelTypesXml.stream().map(channelTypeXml -> {
            ChannelType channelType = channelTypeXml.toChannelType();
            String channelId = channelType.getUID().getId();

            Builder<TranslationsEntry> entriesBuilder = Stream.builder();
            entriesBuilder.add(entry(String.format("%s.%s.label", keyPrefix, channelId), channelType.getLabel()));
            entriesBuilder
                    .add(entry(String.format("%s.%s.description", keyPrefix, channelId), channelType.getDescription()));

            StateDescription stateDescription = channelType.getState();
            if (stateDescription != null) {
                stateDescription.getOptions().stream()
                        .map(option -> entry(
                                String.format("%s.%s.state.option.%s", keyPrefix, channelId, option.getValue()),
                                option.getLabel()))
                        .forEach(entriesBuilder::add);

                if (stateDescription.getPattern() != null) {
                    String pattern = stateDescription.getPattern();
                    if (pattern != null && pattern.contains("%1$t")) {
                        entriesBuilder.add(entry(String.format("%s.%s.state.pattern", keyPrefix, channelId),
                                stateDescription.getPattern()));
                    }
                }
            }

            CommandDescription commandDescription = channelType.getCommandDescription();
            if (commandDescription != null) {
                commandDescription.getCommandOptions().stream()
                        .map(option -> entry(
                                String.format("%s.%s.command.option.%s", keyPrefix, channelId, option.getCommand()),
                                option.getLabel()))
                        .forEach(entriesBuilder::add);
            }

            return group(entriesBuilder.build());
        }));
    }

    private TranslationsSection channelTypesConfigSection(BundleInfo bundleInfo) {
        String header = "channel types config";
        List<ChannelTypeXmlResult> channelTypesXml = bundleInfo.getChannelTypesXml();
        if (channelTypesXml.isEmpty()) {
            return section(header);
        }

        Stream<ConfigDescription> configDescriptionStream = Stream.concat(
                channelTypesXml.stream().map(ChannelTypeXmlResult::getConfigDescription) //
                        .filter(Objects::nonNull),
                channelTypesXml.stream().map(ChannelTypeXmlResult::toChannelType)
                        .map(ChannelType::getConfigDescriptionURI) //
                        .distinct() //
                        .map(bundleInfo::getConfigDescription) //
                        .filter(Optional::isPresent) //
                        .map(Optional::get));

        Builder<TranslationsGroup> groupsBuilder = Stream.builder();

        configDescriptionStream.map(configDescription -> {
            String configKeyPrefix = String.format("%s.config.%s.%s",
                    (Object[]) configDescription.getUID().toString().split(":"));

            Builder<TranslationsGroup> streamBuilder = Stream.builder();
            configDescriptionGroupParameters(configKeyPrefix, configDescription.getParameterGroups())
                    .forEach(streamBuilder::add);
            configDescriptionParameters(configKeyPrefix, configDescription.getParameters()).forEach(streamBuilder::add);

            return streamBuilder.build();
        }).reduce(Stream::concat).orElseGet(Stream::empty).forEach(groupsBuilder::add);

        return section(header, groupsBuilder.build());
    }

    private Stream<TranslationsGroup> configDescriptionGroupParameters(String keyPrefix,
            List<ConfigDescriptionParameterGroup> parameterGroups) {
        String groupKeyPrefix = String.format("%s.group", keyPrefix);
        return parameterGroups.stream()
                .map(parameterGroup -> group(
                        entry(String.format("%s.%s.label", groupKeyPrefix, parameterGroup.getName()),
                                parameterGroup.getLabel()),
                        entry(String.format("%s.%s.description", groupKeyPrefix, parameterGroup.getName()),
                                parameterGroup.getDescription())));
    }

    private Stream<TranslationsGroup> configDescriptionParameters(String keyPrefix,
            List<ConfigDescriptionParameter> parameters) {
        return parameters.stream().map(parameter -> {
            String parameterKeyPrefix = String.format("%s.%s", keyPrefix, parameter.getName());

            Builder<TranslationsEntry> entriesBuilder = Stream.builder();
            entriesBuilder.add(entry(String.format("%s.label", parameterKeyPrefix), parameter.getLabel()));
            entriesBuilder.add(entry(String.format("%s.description", parameterKeyPrefix), parameter.getDescription()));

            parameter.getOptions().stream()
                    .map(option -> entry(String.format("%s.option.%s", parameterKeyPrefix, option.getValue()),
                            option.getLabel()))
                    .forEach(entriesBuilder::add);

            return group(entriesBuilder.build());
        });
    }
}
