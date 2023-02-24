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
package org.openhab.core.thing.internal.update;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.internal.update.dto.XmlAddChannel;
import org.openhab.core.thing.internal.update.dto.XmlInstructionSet;
import org.openhab.core.thing.internal.update.dto.XmlRemoveChannel;
import org.openhab.core.thing.internal.update.dto.XmlThingType;
import org.openhab.core.thing.internal.update.dto.XmlUpdateChannel;
import org.openhab.core.thing.internal.update.dto.XmlUpdateDescriptions;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ThingUpdateInstructionReader} is used to read instructions for a given {@link ThingHandlerFactory} and
 * * create a list of {@link ThingUpdateInstruction}s
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ThingUpdateInstructionReader {
    private final Logger logger = LoggerFactory.getLogger(ThingUpdateInstructionReader.class);
    private final BundleResolver bundleResolver;

    public ThingUpdateInstructionReader(BundleResolver bundleResolver) {
        this.bundleResolver = bundleResolver;
    }

    public Map<UpdateInstructionKey, List<ThingUpdateInstruction>> readForFactory(ThingHandlerFactory factory) {
        Bundle bundle = bundleResolver.resolveBundle(factory.getClass());
        if (bundle == null) {
            logger.error(
                    "Could not get bundle for '{}', thing type updates will fail. If this occurs outside of tests, it is a bug.",
                    factory.getClass());
            return Map.of();
        }

        Map<UpdateInstructionKey, List<ThingUpdateInstruction>> updateInstructions = new HashMap<>();
        Enumeration<URL> entries = bundle.findEntries("OH-INF/update", "*.xml", true);
        if (entries != null) {
            while (entries.hasMoreElements()) {
                URL url = entries.nextElement();
                try {
                    JAXBContext context = JAXBContext.newInstance(XmlUpdateDescriptions.class);
                    Unmarshaller u = context.createUnmarshaller();
                    XmlUpdateDescriptions updateDescriptions = (XmlUpdateDescriptions) u.unmarshal(url);

                    for (XmlThingType thingType : updateDescriptions.getThingType()) {
                        ThingTypeUID thingTypeUID = new ThingTypeUID(thingType.getUid());
                        UpdateInstructionKey key = new UpdateInstructionKey(factory, thingTypeUID);
                        List<ThingUpdateInstruction> instructions = new ArrayList<>();
                        List<XmlInstructionSet> instructionSets = thingType.getInstructionSet().stream()
                                .sorted(Comparator.comparing(XmlInstructionSet::getTargetVersion)).toList();
                        for (XmlInstructionSet instructionSet : instructionSets) {
                            int targetVersion = instructionSet.getTargetVersion();
                            for (Object instruction : instructionSet.getInstructions()) {
                                if (instruction instanceof XmlAddChannel addChannelType) {
                                    instructions.add(new UpdateChannelInstructionImpl(targetVersion, addChannelType));
                                } else if (instruction instanceof XmlUpdateChannel updateChannelType) {
                                    instructions
                                            .add(new UpdateChannelInstructionImpl(targetVersion, updateChannelType));
                                } else if (instruction instanceof XmlRemoveChannel removeChannelType) {
                                    instructions
                                            .add(new RemoveChannelInstructionImpl(targetVersion, removeChannelType));
                                } else {
                                    logger.warn("Instruction type '{}' is unknown.", instruction.getClass());
                                }
                            }
                        }
                        updateInstructions.put(key, instructions);
                    }
                    logger.trace("Reading update instructions from '{}'", url.getPath());
                } catch (IllegalArgumentException | JAXBException e) {
                    logger.warn("Failed to parse update instructions from '{}':", url, e);
                }
            }
        }

        return updateInstructions;
    }

    public record UpdateInstructionKey(ThingHandlerFactory factory, ThingTypeUID thingTypeId) {
    }
}
