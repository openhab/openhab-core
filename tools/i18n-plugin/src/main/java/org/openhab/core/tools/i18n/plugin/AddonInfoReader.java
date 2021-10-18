/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
import java.util.stream.Stream;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.binding.xml.internal.BindingInfoReader;
import org.openhab.core.binding.xml.internal.BindingInfoXmlResult;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.xml.internal.ConfigDescriptionReader;
import org.openhab.core.thing.xml.internal.ChannelGroupTypeXmlResult;
import org.openhab.core.thing.xml.internal.ChannelTypeXmlResult;
import org.openhab.core.thing.xml.internal.ThingDescriptionReader;
import org.openhab.core.thing.xml.internal.ThingTypeXmlResult;

import com.thoughtworks.xstream.converters.ConversionException;

/**
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public class AddonInfoReader {

    private final Log log;

    public AddonInfoReader(Log log) {
        this.log = log;
    }

    public AddonInfo readAddonInfo(Path ohinfPath) throws IOException {
        AddonInfo addonInfo = new AddonInfo();
        readBindingInfo(ohinfPath, addonInfo);
        readConfigInfo(ohinfPath, addonInfo);
        readThingInfo(ohinfPath, addonInfo);
        return addonInfo;
    }

    private Stream<Path> xmlPathStream(Path ohinfPath, String directory) throws IOException {
        Path path = ohinfPath.resolve(directory);
        return Files.exists(path)
                ? Files.find(path, Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile())
                : Stream.of();
    }

    private void readBindingInfo(Path ohinfPath, AddonInfo addonInfo) throws IOException {
        BindingInfoReader reader = new BindingInfoReader();
        xmlPathStream(ohinfPath, "binding").forEach(path -> {
            log.info("Reading: " + path);
            try {
                BindingInfoXmlResult bindingInfoXml = reader.readFromXML(path.toUri().toURL());
                if (bindingInfoXml != null) {
                    addonInfo.setBindingInfoXml(bindingInfoXml);
                }
            } catch (ConversionException | MalformedURLException e) {
                log.warn("Exception while reading binding info from: " + path, e);
            }
        });
    }

    private void readConfigInfo(Path ohinfPath, AddonInfo addonInfo) throws IOException {
        ConfigDescriptionReader reader = new ConfigDescriptionReader();
        xmlPathStream(ohinfPath, "config").forEach(path -> {
            log.info("Reading: " + path);
            try {
                List<ConfigDescription> configDescriptions = reader.readFromXML(path.toUri().toURL());
                if (configDescriptions != null) {
                    addonInfo.getConfigDescriptions().addAll(configDescriptions);
                }
            } catch (ConversionException | MalformedURLException e) {
                log.warn("Exception while reading config info from: " + path, e);
            }
        });
    }

    private void readThingInfo(Path ohinfPath, AddonInfo addonInfo) throws IOException {
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
                        addonInfo.getThingTypesXml().add((ThingTypeXmlResult) type);
                    } else if (type instanceof ChannelGroupTypeXmlResult) {
                        addonInfo.getChannelGroupTypesXml().add((ChannelGroupTypeXmlResult) type);
                    } else if (type instanceof ChannelTypeXmlResult) {
                        addonInfo.getChannelTypesXml().add((ChannelTypeXmlResult) type);
                    }
                }
            } catch (ConversionException | MalformedURLException e) {
                log.warn("Exception while reading thing info from: " + path, e);
            }
        });
    }
}
