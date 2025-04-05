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

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlModelRepository;
import org.openhab.core.model.yaml.internal.things.YamlChannelDTO;
import org.openhab.core.model.yaml.internal.things.YamlThingDTO;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.fileconverter.AbstractThingFileGenerator;
import org.openhab.core.thing.fileconverter.ThingFileGenerator;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeUID;
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
@Component(immediate = true, service = ThingFileGenerator.class)
public class YamlThingFileConverter extends AbstractThingFileGenerator {

    private final YamlModelRepository modelRepository;

    @Activate
    public YamlThingFileConverter(final @Reference YamlModelRepository modelRepository,
            final @Reference ThingTypeRegistry thingTypeRegistry,
            final @Reference ChannelTypeRegistry channelTypeRegistry,
            final @Reference ConfigDescriptionRegistry configDescRegistry) {
        super(thingTypeRegistry, channelTypeRegistry, configDescRegistry);
        this.modelRepository = modelRepository;
    }

    @Override
    public String getFileFormatGenerator() {
        return "YAML";
    }

    @Override
    public synchronized void generateFileFormat(OutputStream out, List<Thing> things, boolean hideDefaultParameters) {
        List<YamlElement> elements = new ArrayList<>();
        for (Thing thing : things) {
            elements.add(buildThingDTO(thing, hideDefaultParameters));
        }
        modelRepository.generateSyntaxFromElements(out, elements);
    }

    private YamlThingDTO buildThingDTO(Thing thing, boolean hideDefaultParameters) {
        YamlThingDTO dto = new YamlThingDTO();
        dto.uid = thing.getUID().getAsString();
        dto.isBridge = thing instanceof Bridge ? true : null;
        ThingUID bridgeUID = thing.getBridgeUID();
        dto.bridge = bridgeUID == null ? null : bridgeUID.getAsString();
        dto.label = thing.getLabel();
        dto.location = thing.getLocation();

        Map<String, Object> config = new LinkedHashMap<>();
        for (ConfigParameter param : getConfigurationParameters(thing, hideDefaultParameters)) {
            if (param.value() instanceof List<?> list) {
                if (!list.isEmpty()) {
                    config.put(param.name(), param.value());
                }
            } else {
                config.put(param.name(), param.value());
            }
        }
        dto.config = config.isEmpty() ? null : config;

        Map<String, YamlChannelDTO> channels = new LinkedHashMap<>();
        for (Channel channel : getNonDefaultChannels(thing)) {
            channels.put(channel.getUID().getId(), buildChannelDTO(channel, hideDefaultParameters));
        }
        dto.channels = channels.isEmpty() ? null : channels;

        return dto;
    }

    private YamlChannelDTO buildChannelDTO(Channel channel, boolean hideDefaultParameters) {
        YamlChannelDTO dto = new YamlChannelDTO();
        ChannelTypeUID channelTypeUID = channel.getChannelTypeUID();
        if (channelTypeUID != null) {
            dto.type = channelTypeUID.getId();
        } else {
            dto.kind = channel.getKind() == ChannelKind.STATE ? "state" : "trigger";
            dto.itemType = channel.getAcceptedItemType();
        }
        dto.label = channel.getLabel();

        Map<String, Object> config = new LinkedHashMap<>();
        for (ConfigParameter param : getConfigurationParameters(channel, hideDefaultParameters)) {
            if (param.value() instanceof List<?> list) {
                if (!list.isEmpty()) {
                    config.put(param.name(), param.value());
                }
            } else {
                config.put(param.name(), param.value());
            }
        }
        dto.config = config.isEmpty() ? null : config;

        return dto;
    }
}
