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
package org.openhab.core.model.thing.internal.fileconverter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.thing.thing.ModelBridge;
import org.openhab.core.model.thing.thing.ModelChannel;
import org.openhab.core.model.thing.thing.ModelProperty;
import org.openhab.core.model.thing.thing.ModelThing;
import org.openhab.core.model.thing.thing.ThingFactory;
import org.openhab.core.model.thing.thing.ThingModel;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DslThingFileConverter} is the DSL file converter for {@link Thing} object
 * with the capabilities of parsing and generating file.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = ThingFileGenerator.class)
public class DslThingFileConverter extends AbstractThingFileGenerator {

    private final Logger logger = LoggerFactory.getLogger(DslThingFileConverter.class);

    private final ModelRepository modelRepository;

    @Activate
    public DslThingFileConverter(final @Reference ModelRepository modelRepository,
            final @Reference ThingTypeRegistry thingTypeRegistry,
            final @Reference ChannelTypeRegistry channelTypeRegistry,
            final @Reference ConfigDescriptionRegistry configDescRegistry) {
        super(thingTypeRegistry, channelTypeRegistry, configDescRegistry);
        this.modelRepository = modelRepository;
    }

    @Override
    public String getFileFormatGenerator() {
        return "DSL";
    }

    @Override
    public synchronized void generateFileFormat(OutputStream out, List<Thing> things, boolean hideDefaultParameters) {
        ThingModel model = ThingFactory.eINSTANCE.createThingModel();
        Set<Thing> handledThings = new HashSet<>();
        for (Thing thing : things) {
            if (handledThings.contains(thing)) {
                continue;
            }
            model.getThings()
                    .add(buildModelThing(thing, hideDefaultParameters, things.size() > 1, true, things, handledThings));
        }
        // Double quotes are unexpectedly generated in thing UID when the segment contains a -.
        // Fix that by removing these double quotes. Requires to first build the generated syntax as a String
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        modelRepository.generateSyntaxFromModel(outputStream, "things", model);
        String syntax = new String(outputStream.toByteArray()).replaceAll(":\"([a-zA-Z0-9_][a-zA-Z0-9_-]*)\"", ":$1");
        try {
            out.write(syntax.getBytes());
        } catch (IOException e) {
            logger.warn("Exception when writing the generated syntax {}", e.getMessage());
        }
    }

    private ModelThing buildModelThing(Thing thing, boolean hideDefaultParameters, boolean preferPresentationAsTree,
            boolean topLevel, List<Thing> onlyThings, Set<Thing> handledThings) {
        ModelThing model;
        ModelBridge modelBridge;
        List<Thing> childThings = getChildThings(thing, onlyThings);
        if (preferPresentationAsTree && thing instanceof Bridge && !childThings.isEmpty()) {
            modelBridge = ThingFactory.eINSTANCE.createModelBridge();
            modelBridge.setBridge(true);
            model = modelBridge;
        } else {
            modelBridge = null;
            model = ThingFactory.eINSTANCE.createModelThing();
        }
        if (!preferPresentationAsTree || topLevel) {
            model.setId(thing.getUID().getAsString());
            ThingUID bridgeUID = thing.getBridgeUID();
            if (bridgeUID != null && modelBridge == null) {
                model.setBridgeUID(bridgeUID.getAsString());
            }
        } else {
            model.setThingTypeId(thing.getThingTypeUID().getId());
            model.setThingId(thing.getUID().getId());
        }
        if (thing.getLabel() != null) {
            model.setLabel(thing.getLabel());
        }
        if (thing.getLocation() != null) {
            model.setLocation(thing.getLocation());
        }

        for (ConfigParameter param : getConfigurationParameters(thing, hideDefaultParameters)) {
            ModelProperty property = buildModelProperty(param.name(), param.value());
            if (property != null) {
                model.getProperties().add(property);
            }
        }

        if (preferPresentationAsTree && modelBridge != null) {
            modelBridge.setThingsHeader(false);
            for (Thing child : childThings) {
                if (!handledThings.contains(child)) {
                    modelBridge.getThings()
                            .add(buildModelThing(child, hideDefaultParameters, true, false, onlyThings, handledThings));
                }
            }
        }

        List<Channel> channels = getNonDefaultChannels(thing);
        model.setChannelsHeader(!channels.isEmpty());
        for (Channel channel : channels) {
            model.getChannels().add(buildModelChannel(channel, hideDefaultParameters));
        }

        handledThings.add(thing);

        return model;
    }

    private ModelChannel buildModelChannel(Channel channel, boolean hideDefaultParameters) {
        ModelChannel modelChannel = ThingFactory.eINSTANCE.createModelChannel();
        ChannelTypeUID channelTypeUID = channel.getChannelTypeUID();
        if (channelTypeUID != null) {
            modelChannel.setChannelType(channelTypeUID.getId());
        } else {
            modelChannel.setChannelKind(channel.getKind() == ChannelKind.STATE ? "State" : "Trigger");
            modelChannel.setType(channel.getAcceptedItemType());
        }
        modelChannel.setId(channel.getUID().getId());
        if (channel.getLabel() != null) {
            modelChannel.setLabel(channel.getLabel());
        }
        for (ConfigParameter param : getConfigurationParameters(channel, hideDefaultParameters)) {
            ModelProperty property = buildModelProperty(param.name(), param.value());
            if (property != null) {
                modelChannel.getProperties().add(property);
            }
        }
        return modelChannel;
    }

    private @Nullable ModelProperty buildModelProperty(String key, Object value) {
        ModelProperty property = ThingFactory.eINSTANCE.createModelProperty();
        property.setKey(key);
        if (value instanceof List<?> list) {
            if (!list.isEmpty()) {
                property.getValue().addAll(list);
            } else {
                property = null;
            }
        } else if (value instanceof Double doubleValue) {
            // DSL thing syntax does not like a configuration parameter value provided as Double type.
            // By security, we apply a conversion to a BigDecimal in case this would happen.
            logger.debug("Configuration parameter {} with value {} is provided unexpectedly as Double type", key,
                    value);
            property.getValue().add(BigDecimal.valueOf(doubleValue));
        } else {
            property.getValue().add(value);
        }
        return property;
    }
}
