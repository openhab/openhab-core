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
package org.openhab.core.tools.i18n.plugin;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.addon.internal.xml.AddonInfoReader;
import org.openhab.core.addon.internal.xml.AddonInfoXmlResult;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.xml.internal.ConfigDescriptionReader;
import org.openhab.core.thing.xml.internal.ChannelGroupTypeXmlResult;
import org.openhab.core.thing.xml.internal.ChannelTypeXmlResult;
import org.openhab.core.thing.xml.internal.ThingDescriptionReader;
import org.openhab.core.thing.xml.internal.ThingTypeXmlResult;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thoughtworks.xstream.converters.ConversionException;

/**
 * Reads all the bundle information provided by XML files in the <code>OH-INF</code> directory to {@link BundleInfo}.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public class BundleInfoReader {

    private final Log log;

    private final Predicate<Path> isJsonFile = path -> Files.isRegularFile(path) && path.toString().endsWith(".json");

    public BundleInfoReader(Log log) {
        this.log = log;
    }

    public BundleInfo readBundleInfo(Path ohinfPath) throws IOException {
        BundleInfo bundleInfo = new BundleInfo();
        readAddonInfo(ohinfPath, bundleInfo);
        readConfigInfo(ohinfPath, bundleInfo);
        readThingInfo(ohinfPath, bundleInfo);
        readModuleTypeInfo(ohinfPath, bundleInfo);
        readRuleTemplateInfo(ohinfPath, bundleInfo);
        return bundleInfo;
    }

    private Stream<Path> xmlPathStream(Path ohinfPath, String directory) throws IOException {
        Path path = ohinfPath.resolve(directory);
        return Files.exists(path)
                ? Files.find(path, Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile())
                : Stream.of();
    }

    private void readAddonInfo(Path ohinfPath, BundleInfo bundleInfo) throws IOException {
        AddonInfoReader reader = new AddonInfoReader();
        try (Stream<Path> xmlPathStream = xmlPathStream(ohinfPath, "addon")) {
            xmlPathStream.forEach(path -> {
                log.info("Reading: " + path);
                try {
                    AddonInfoXmlResult bindingInfoXml = reader.readFromXML(path.toUri().toURL());
                    if (bindingInfoXml != null) {
                        bundleInfo.setAddonId(bindingInfoXml.addonInfo().getId());
                        bundleInfo.setAddonInfoXml(bindingInfoXml);
                    }
                } catch (ConversionException | MalformedURLException e) {
                    log.warn("Exception while reading binding info from: " + path, e);
                }
            });
        }
    }

    private void readConfigInfo(Path ohinfPath, BundleInfo bundleInfo) throws IOException {
        ConfigDescriptionReader reader = new ConfigDescriptionReader();
        xmlPathStream(ohinfPath, "config").forEach(path -> {
            log.info("Reading: " + path);
            try {
                List<ConfigDescription> configDescriptions = reader.readFromXML(path.toUri().toURL());
                if (configDescriptions != null) {
                    bundleInfo.getConfigDescriptions().addAll(configDescriptions);
                }
            } catch (ConversionException | MalformedURLException e) {
                log.warn("Exception while reading config info from: " + path, e);
            }
        });
    }

    private void readThingInfo(Path ohinfPath, BundleInfo bundleInfo) throws IOException {
        ThingDescriptionReader reader = new ThingDescriptionReader();
        xmlPathStream(ohinfPath, "thing").forEach(path -> {
            log.info("Reading: " + path);
            try {
                List<?> types = reader.readFromXML(path.toUri().toURL());
                if (types == null) {
                    return;
                }
                for (Object type : types) {
                    if (type instanceof ThingTypeXmlResult) {
                        ThingTypeXmlResult result = (ThingTypeXmlResult) type;
                        bundleInfo.getThingTypesXml().add(result);
                        if (bundleInfo.getAddonId().isBlank()) {
                            bundleInfo.setAddonId(result.getUID().getBindingId());
                        }
                    } else if (type instanceof ChannelGroupTypeXmlResult) {
                        ChannelGroupTypeXmlResult result = (ChannelGroupTypeXmlResult) type;
                        bundleInfo.getChannelGroupTypesXml().add(result);
                        if (bundleInfo.getAddonId().isBlank()) {
                            bundleInfo.setAddonId(result.getUID().getBindingId());
                        }
                    } else if (type instanceof ChannelTypeXmlResult) {
                        ChannelTypeXmlResult result = (ChannelTypeXmlResult) type;
                        bundleInfo.getChannelTypesXml().add(result);
                        if (bundleInfo.getAddonId().isBlank()) {
                            bundleInfo.setAddonId(result.toChannelType().getUID().getBindingId());
                        }
                    }
                }
            } catch (ConversionException | MalformedURLException e) {
                log.warn("Exception while reading thing info from: " + path, e);
            }
        });
    }

    private void readModuleTypeInfo(Path ohinfPath, BundleInfo bundleInfo) throws IOException {
        Path modulePath = ohinfPath.resolve("automation").resolve("moduletypes");
        if (Files.exists(modulePath)) {
            try (Stream<Path> files = Files.walk(modulePath)) {
                List<JsonObject> moduleTypes = files.filter(isJsonFile).flatMap(this::readJsonElementsFromFile)
                        .map(JsonElement::getAsJsonObject).collect(Collectors.toList());
                if (!moduleTypes.isEmpty()) {
                    bundleInfo.setModuleTypesJson(moduleTypes);
                }
            }
        }
    }

    private void readRuleTemplateInfo(Path ohinfPath, BundleInfo bundleInfo) throws IOException {
        Path template = ohinfPath.resolve("automation").resolve("templates");
        if (Files.exists(template)) {
            try (Stream<Path> files = Files.walk(template)) {
                List<JsonObject> ruleTemplates = files.filter(isJsonFile).flatMap(this::readJsonElementsFromFile)
                        .map(JsonElement::getAsJsonObject).collect(Collectors.toList());
                if (!ruleTemplates.isEmpty()) {
                    bundleInfo.setRuleTemplateJson(ruleTemplates);
                }
            }
        }
    }

    private Stream<JsonElement> readJsonElementsFromFile(Path path) {
        try {
            JsonElement element = JsonParser.parseString(Files.readString(path));
            if (element.isJsonObject()) {
                return element.getAsJsonObject().entrySet().stream().map(Map.Entry::getValue)
                        .filter(JsonElement::isJsonArray)
                        .flatMap(a -> StreamSupport.stream(a.getAsJsonArray().spliterator(), false));
            }
        } catch (IOException ignored) {
        }
        return Stream.of(JsonNull.INSTANCE);
    }
}
