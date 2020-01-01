/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.thing.xml.internal;

import java.util.ArrayList;
import java.util.List;

import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.config.xml.AbstractXmlConfigDescriptionProvider;
import org.openhab.core.config.xml.osgi.XmlDocumentBundleTracker;
import org.openhab.core.config.xml.osgi.XmlDocumentProvider;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ThingType;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.converters.ConversionException;

/**
 * The {@link ThingTypeXmlProvider} is responsible managing any created {@link ThingType} objects by a
 * {@link ThingDescriptionReader} for a certain
 * bundle.
 * <p>
 * This implementation registers each {@link ThingType} object at the {@link ThingTypeProvider} which is itself
 * registered as service at the <i>OSGi</i> service registry. If a configuration section is found, a
 * {@link ConfigDescription} object is registered at the {@link ConfigDescriptionProvider} which is itself registered as
 * service at the <i>OSGi</i> service registry.
 * <p>
 * The {@link ThingTypeXmlProvider} uses an internal cache consisting of {@link #thingTypeRefs},
 * {@link #channelGroupTypeRefs}, {@link #channelGroupTypes} and {@link #channelTypes}. This cache is used to merge
 * first the {@link ChannelType} definitions with the {@link ChannelGroupTypeXmlResult} objects to create valid
 * {@link ChannelGroupType} objects. After that the {@link ChannelType} and the {@link ChannelGroupType} definitions are
 * used to merge with the {@link ThingTypeXmlResult} objects to create valid {@link ThingType} objects. After the merge
 * process has been finished, the cache is cleared again. The merge process is started when {@link #addingFinished()} is
 * invoked from the according {@link XmlDocumentBundleTracker}.
 *
 * @author Michael Grammling - Initial contribution
 * @author Ivan Iliev - Added support for system wide channel types
 */
public class ThingTypeXmlProvider implements XmlDocumentProvider<List<?>> {

    private final Logger logger = LoggerFactory.getLogger(ThingTypeXmlProvider.class);

    private final Bundle bundle;
    private final AbstractXmlConfigDescriptionProvider configDescriptionProvider;
    private final XmlThingTypeProvider thingTypeProvider;

    // temporary cache
    private final List<ThingTypeXmlResult> thingTypeRefs = new ArrayList<>(10);
    private final List<ChannelGroupTypeXmlResult> channelGroupTypeRefs = new ArrayList<>(10);
    private final List<ChannelTypeXmlResult> channelTypeRefs = new ArrayList<>(10);

    private final XmlChannelTypeProvider channelTypeProvider;
    private final XmlChannelGroupTypeProvider channelGroupTypeProvider;

    public ThingTypeXmlProvider(Bundle bundle, AbstractXmlConfigDescriptionProvider configDescriptionProvider,
            XmlThingTypeProvider thingTypeProvider, XmlChannelTypeProvider channelTypeProvider,
            XmlChannelGroupTypeProvider channelGroupTypeProvider) throws IllegalArgumentException {
        if (bundle == null) {
            throw new IllegalArgumentException("The Bundle must not be null!");
        }

        if (configDescriptionProvider == null) {
            throw new IllegalArgumentException("The XmlConfigDescriptionProvider must not be null!");
        }

        if (thingTypeProvider == null) {
            throw new IllegalArgumentException("The XmlThingTypeProvider must not be null!");
        }

        this.bundle = bundle;
        this.configDescriptionProvider = configDescriptionProvider;
        this.thingTypeProvider = thingTypeProvider;
        this.channelTypeProvider = channelTypeProvider;
        this.channelGroupTypeProvider = channelGroupTypeProvider;
    }

    @Override
    public synchronized void addingObject(List<?> types) {
        if (types != null) {
            for (Object type : types) {
                if (type instanceof ThingTypeXmlResult) {
                    ThingTypeXmlResult typeResult = (ThingTypeXmlResult) type;
                    addConfigDescription(typeResult.getConfigDescription());
                    thingTypeRefs.add(typeResult);
                } else if (type instanceof ChannelGroupTypeXmlResult) {
                    ChannelGroupTypeXmlResult typeResult = (ChannelGroupTypeXmlResult) type;
                    channelGroupTypeRefs.add(typeResult);
                } else if (type instanceof ChannelTypeXmlResult) {
                    ChannelTypeXmlResult typeResult = (ChannelTypeXmlResult) type;
                    channelTypeRefs.add(typeResult);
                    addConfigDescription(typeResult.getConfigDescription());
                } else {
                    throw new ConversionException("Unknown data type for '" + type + "'!");
                }
            }
        }
    }

    private void addConfigDescription(ConfigDescription configDescription) {
        if (configDescription != null) {
            try {
                configDescriptionProvider.add(bundle, configDescription);
            } catch (RuntimeException e) {
                logger.error("Could not register ConfigDescription: {}", configDescription.getUID(), e);
            }
        }
    }

    @Override
    public synchronized void addingFinished() {
        // create channel types
        for (ChannelTypeXmlResult type : channelTypeRefs) {
            ChannelType channelType = type.toChannelType();
            try {
                channelTypeProvider.add(bundle, channelType);
            } catch (RuntimeException e) {
                logger.error("Could not register ChannelType: {}", channelType.getUID(), e);
            }
        }

        // create channel group types
        for (ChannelGroupTypeXmlResult type : channelGroupTypeRefs) {
            try {
                channelGroupTypeProvider.add(bundle, type.toChannelGroupType());
            } catch (RuntimeException e) {
                logger.error("Could not register ChannelGroupType: {}", type.getUID(), e);
            }
        }

        // create thing and bridge types
        for (ThingTypeXmlResult type : thingTypeRefs) {
            try {
                thingTypeProvider.add(bundle, type.toThingType());
            } catch (RuntimeException e) {
                logger.error("Could not register ThingType: {}", type.getUID(), e);
            }
        }

        // release temporary cache
        thingTypeRefs.clear();
        channelGroupTypeRefs.clear();
        channelTypeRefs.clear();
    }

    @Override
    public synchronized void release() {
        thingTypeProvider.removeAll(bundle);
        channelGroupTypeProvider.removeAll(bundle);
        channelTypeProvider.removeAll(bundle);
        configDescriptionProvider.removeAll(bundle);
    }

}
