/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.internal.things.fileconverter;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.items.ItemUtil;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlModelRepository;
import org.openhab.core.model.yaml.internal.items.YamlChannelLinkProvider;
import org.openhab.core.model.yaml.internal.things.YamlChannelDTO;
import org.openhab.core.model.yaml.internal.things.YamlThingDTO;
import org.openhab.core.model.yaml.internal.things.YamlThingProvider;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.fileconverter.AbstractThingFileGenerator;
import org.openhab.core.thing.fileconverter.ThingFileGenerator;
import org.openhab.core.thing.fileconverter.ThingFileParser;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * {@link YamlThingFileConverter} is the YAML file converter for {@link Thing} object.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { ThingFileGenerator.class, ThingFileParser.class })
public class YamlThingFileConverter extends AbstractThingFileGenerator implements ThingFileParser {

    private final YamlModelRepository modelRepository;
    private final YamlThingProvider thingProvider;
    private final YamlChannelLinkProvider itemChannelLinkProvider;
    private final LocaleProvider localeProvider;

    @Activate
    public YamlThingFileConverter(final @Reference YamlModelRepository modelRepository,
            final @Reference YamlThingProvider thingProvider,
            final @Reference YamlChannelLinkProvider itemChannelLinkProvider,
            final @Reference ThingTypeRegistry thingTypeRegistry,
            final @Reference ChannelTypeRegistry channelTypeRegistry,
            final @Reference ConfigDescriptionRegistry configDescRegistry,
            final @Reference LocaleProvider localeProvider) {
        super(thingTypeRegistry, channelTypeRegistry, configDescRegistry);
        this.modelRepository = modelRepository;
        this.thingProvider = thingProvider;
        this.itemChannelLinkProvider = itemChannelLinkProvider;
        this.localeProvider = localeProvider;
    }

    @Override
    public String getFileFormatGenerator() {
        return "YAML";
    }

    @Override
    public void setThingsToBeGenerated(String id, List<Thing> things, boolean hideDefaultChannels,
            boolean hideDefaultParameters) {
        List<YamlElement> elements = new ArrayList<>();
        things.forEach(thing -> {
            elements.add(buildThingDTO(thing, hideDefaultChannels, hideDefaultParameters));
        });
        modelRepository.addElementsToBeGenerated(id, elements);
    }

    @Override
    public void generateFileFormat(String id, OutputStream out) {
        modelRepository.generateFileFormat(id, out);
    }

    private YamlThingDTO buildThingDTO(Thing thing, boolean hideDefaultChannels, boolean hideDefaultParameters) {
        YamlThingDTO dto = new YamlThingDTO();
        dto.uid = thing.getUID().getAsString();
        dto.isBridge = thing instanceof Bridge ? true : null;
        ThingUID bridgeUID = thing.getBridgeUID();
        dto.bridge = bridgeUID == null ? null : bridgeUID.getAsString();
        ThingType thingType = thingTypeRegistry.getThingType(thing.getThingTypeUID(), localeProvider.getLocale());
        dto.label = thingType != null && thingType.getLabel().equals(thing.getLabel()) ? null : thing.getLabel();
        dto.location = thing.getLocation();

        Map<String, Object> config = new LinkedHashMap<>();
        getConfigurationParameters(thing, hideDefaultParameters).forEach(param -> {
            if (param.value() instanceof List<?> list) {
                if (!list.isEmpty()) {
                    config.put(param.name(), param.value());
                }
            } else {
                config.put(param.name(), param.value());
            }
        });
        dto.config = config.isEmpty() ? null : config;

        Map<String, YamlChannelDTO> channels = new LinkedHashMap<>();
        List<Channel> channelList = hideDefaultChannels ? getNonDefaultChannels(thing) : thing.getChannels();
        channelList.forEach(channel -> {
            channels.put(channel.getUID().getId(), buildChannelDTO(channel, hideDefaultParameters));
        });
        dto.channels = channels.isEmpty() ? null : channels;

        return dto;
    }

    private YamlChannelDTO buildChannelDTO(Channel channel, boolean hideDefaultParameters) {
        YamlChannelDTO dto = new YamlChannelDTO();
        ChannelTypeUID channelTypeUID = channel.getChannelTypeUID();
        if (channelTypeUID != null) {
            dto.type = channelTypeUID.getId();
            ChannelType channelType = channelTypeRegistry.getChannelType(channelTypeUID, localeProvider.getLocale());
            dto.label = channelType != null && channelType.getLabel().equals(channel.getLabel()) ? null
                    : channel.getLabel();
            String descr = channelType != null ? channelType.getDescription() : null;
            dto.description = descr != null && descr.equals(channel.getDescription()) ? null : channel.getDescription();
        } else {
            dto.kind = channel.getKind() == ChannelKind.STATE ? null : "trigger";
            String itemType = channel.getAcceptedItemType();
            dto.itemType = itemType != null ? ItemUtil.getMainItemType(itemType) : null;
            dto.itemDimension = ItemUtil.getItemTypeExtension(itemType);
            dto.label = channel.getLabel();
            dto.description = channel.getDescription();
        }

        Map<String, Object> config = new LinkedHashMap<>();
        getConfigurationParameters(channel, hideDefaultParameters).forEach(param -> {
            if (param.value() instanceof List<?> list) {
                if (!list.isEmpty()) {
                    config.put(param.name(), param.value());
                }
            } else {
                config.put(param.name(), param.value());
            }
        });
        dto.config = config.isEmpty() ? null : config;

        return dto;
    }

    @Override
    public String getFileFormatParser() {
        return "YAML";
    }

    @Override
    public @Nullable String startParsingFileFormat(String syntax, List<String> errors, List<String> warnings) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(syntax.getBytes());
        return modelRepository.createIsolatedModel(inputStream, errors, warnings);
    }

    @Override
    public Collection<Thing> getParsedThings(String modelName) {
        return thingProvider.getAllFromModel(modelName);
    }

    @Override
    public Collection<ItemChannelLink> getParsedChannelLinks(String modelName) {
        return itemChannelLinkProvider.getAllFromModel(modelName);
    }

    @Override
    public void finishParsingFileFormat(String modelName) {
        modelRepository.removeIsolatedModel(modelName);
    }
}
