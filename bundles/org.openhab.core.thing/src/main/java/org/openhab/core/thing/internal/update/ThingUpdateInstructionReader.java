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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.binding.ThingHandlerFactory;
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
    private static final Pattern UPDATE_INSTRUCTION = Pattern
            .compile("(?<version>\\d+);(?<action>ADD_CHANNEL|UPDATE_CHANNEL|REMOVE_CHANNEL);(?<parameters>.*)");

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
        Enumeration<URL> entries = bundle.findEntries("update", "*.update", true);

        if (entries != null) {
            while (entries.hasMoreElements()) {
                URL url = entries.nextElement();
                String fileName = url.getPath();
                String thingTypeId = fileName.substring(fileName.lastIndexOf("/") + 1, fileName.lastIndexOf("."));

                logger.trace("Reading update instructions from '{}'", url.getPath());
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                    updateInstructions.put(new UpdateInstructionKey(factory, thingTypeId), reader.lines()
                            .map(this::parse).filter(Objects::nonNull).map(Objects::requireNonNull).toList());
                } catch (IOException e) {
                    logger.warn("Failed to read update instructions for '{}' from bundle '{}", thingTypeId,
                            bundle.getSymbolicName());
                }
            }
        }

        return updateInstructions;
    }

    private @Nullable ThingUpdateInstruction parse(String string) {
        if (string.isEmpty() || string.startsWith("#")) {
            // either a comment or an empty line
            return null;
        }

        Matcher matcher = UPDATE_INSTRUCTION.matcher(string);
        if (!matcher.matches()) {
            logger.warn("Line '{}' did not match format for instruction. Ignoring.", string);
            return null;
        }

        // create update instruction: version;command;parameter(s)
        int targetThingTypeVersion = Integer.parseInt(matcher.group("version"));
        List<String> parameters = Arrays.asList(matcher.group("parameters").split(","));

        String action = matcher.group("action");
        switch (action) {
            case "ADD_CHANNEL":
            case "UPDATE_CHANNEL":
                if (parameters.size() >= 2 && parameters.size() <= 4) {
                    return new UpdateChannelInstructionImpl(targetThingTypeVersion, parameters,
                            "ADD_CHANNEL".equals(action));
                } else {
                    logger.warn("Line '{}'  has wrong number of parameters (required: >=2, <=4). Ignoring.", string);
                }
                break;
            case "REMOVE_CHANNEL":
                if (parameters.size() == 1) {
                    return new RemoveChannelInstructionImpl(targetThingTypeVersion, parameters);
                } else {
                    logger.warn("Line '{}'  has wrong number of parameters (required: ==1). Ignoring.", string);
                }
                break;
        }

        return null;
    }

    public record UpdateInstructionKey(ThingHandlerFactory factory, String thingTypeId) {
    }
}
