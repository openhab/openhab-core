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
 * Reads all the bundle information provided by XML files in the <code>OH-INF</code> directory to {@link BundleInfo}.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public class BundleInfoReader {

    private final Log log;

    public BundleInfoReader(Log log) {
        this.log = log;
    }

    public BundleInfo readBundleInfo(Path ohinfPath) throws IOException {
        BundleInfo bundleInfo = new BundleInfo();
        readBindingInfo(ohinfPath, bundleInfo);
        readConfigInfo(ohinfPath, bundleInfo);
        readThingInfo(ohinfPath, bundleInfo);
        return bundleInfo;
    }

    private Stream<Path> xmlPathStream(Path ohinfPath, String directory) throws IOException {
        Path path = ohinfPath.resolve(directory);
        return Files.exists(path)
                ? Files.find(path, Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile())
                : Stream.of();
    }

    private void readBindingInfo(Path ohinfPath, BundleInfo bundleInfo) throws IOException {
        BindingInfoReader reader = new BindingInfoReader();
        xmlPathStream(ohinfPath, "binding").forEach(path -> {
            log.info("Reading: " + path);
            try {
                BindingInfoXmlResult bindingInfoXml = reader.readFromXML(path.toUri().toURL());
                if (bindingInfoXml != null) {
                    bundleInfo.setBindingInfoXml(bindingInfoXml);
                }
            } catch (ConversionException | MalformedURLException e) {
                log.warn("Exception while reading binding info from: " + path, e);
            }
        });
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
                        bundleInfo.getThingTypesXml().add((ThingTypeXmlResult) type);
                    } else if (type instanceof ChannelGroupTypeXmlResult) {
                        bundleInfo.getChannelGroupTypesXml().add((ChannelGroupTypeXmlResult) type);
                    } else if (type instanceof ChannelTypeXmlResult) {
                        bundleInfo.getChannelTypesXml().add((ChannelTypeXmlResult) type);
                    }
                }
            } catch (ConversionException | MalformedURLException e) {
                log.warn("Exception while reading thing info from: " + path, e);
            }
        });
    }
}
