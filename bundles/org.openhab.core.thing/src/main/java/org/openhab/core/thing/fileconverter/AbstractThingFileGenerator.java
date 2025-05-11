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
package org.openhab.core.thing.fileconverter;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.osgi.service.component.annotations.Activate;

/**
 * {@link AbstractThingFileGenerator} is the base class for any {@link Thing} file generator.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractThingFileGenerator implements ThingFileGenerator {

    protected final ThingTypeRegistry thingTypeRegistry;
    protected final ChannelTypeRegistry channelTypeRegistry;
    protected final ConfigDescriptionRegistry configDescRegistry;

    @Activate
    public AbstractThingFileGenerator(ThingTypeRegistry thingTypeRegistry, ChannelTypeRegistry channelTypeRegistry,
            ConfigDescriptionRegistry configDescRegistry) {
        this.thingTypeRegistry = thingTypeRegistry;
        this.channelTypeRegistry = channelTypeRegistry;
        this.configDescRegistry = configDescRegistry;
    }

    /**
     * {@link ConfigParameter} is a container for any configuration parameter defined by a name and a value.
     */
    protected record ConfigParameter(String name, Object value) {
    }

    /**
     * Get the child things of a bridge thing amongst a list of things, ordered by UID.
     *
     * @param thing the thing
     * @param fromThings the list of things to look for
     * @return the sorted list of child things or an empty list if the thing is not a bridge thing
     */
    protected List<Thing> getChildThings(Thing thing, List<Thing> fromThings) {
        if (thing instanceof Bridge bridge) {
            return fromThings.stream().filter(th -> bridge.getUID().equals(th.getBridgeUID()))
                    .sorted((thing1, thing2) -> {
                        return thing1.getUID().getAsString().compareTo(thing2.getUID().getAsString());
                    }).collect(Collectors.toList());
        }
        return List.of();
    }

    /**
     * Get the list of configuration parameters for a thing.
     *
     * If a configuration description is found for the thing type, the parameters are provided in the same order
     * as in this configuration description, and any parameter having the default value is ignored.
     * If not, the parameters are provided sorted by natural order of their names.
     *
     * @param thing the thing
     * @param hideDefaultParameters true to hide the configuration parameters having the default value
     * @return the sorted list of configuration parameters for the thing
     */
    protected List<ConfigParameter> getConfigurationParameters(Thing thing, boolean hideDefaultParameters) {
        return getConfigurationParameters(getConfigDescriptionParameters(thing), thing.getConfiguration(),
                hideDefaultParameters);
    }

    /**
     * Get the list of configuration parameters for a channel.
     *
     * If a configuration description is found for the channel type, the parameters are provided in the same order
     * as in this configuration description, and any parameter having the default value is ignored.
     * If not, the parameters are provided sorted by natural order of their names.
     *
     * @param channel the channel
     * @param hideDefaultParameters true to hide the configuration parameters having the default value
     * @return the sorted list of configuration parameters for the channel
     */
    protected List<ConfigParameter> getConfigurationParameters(Channel channel, boolean hideDefaultParameters) {
        return getConfigurationParameters(getConfigDescriptionParameters(channel), channel.getConfiguration(),
                hideDefaultParameters);
    }

    private List<ConfigParameter> getConfigurationParameters(
            List<ConfigDescriptionParameter> configDescriptionParameter, Configuration configParameters,
            boolean hideDefaultParameters) {
        List<ConfigParameter> parameters = new ArrayList<>();
        Set<String> handledNames = new HashSet<>();
        for (ConfigDescriptionParameter param : configDescriptionParameter) {
            String paramName = param.getName();
            if (handledNames.contains(paramName)) {
                continue;
            }
            Object value = configParameters.get(paramName);
            Object defaultValue = ConfigUtil.getDefaultValueAsCorrectType(param);
            if (value != null && (!hideDefaultParameters || !value.equals(defaultValue))) {
                parameters.add(new ConfigParameter(paramName, value));
            }
            handledNames.add(paramName);
        }
        for (String paramName : configParameters.keySet().stream().sorted().collect(Collectors.toList())) {
            if (handledNames.contains(paramName)) {
                continue;
            }
            Object value = configParameters.get(paramName);
            if (value != null) {
                parameters.add(new ConfigParameter(paramName, value));
            }
            handledNames.add(paramName);
        }
        return parameters;
    }

    private List<ConfigDescriptionParameter> getConfigDescriptionParameters(Thing thing) {
        List<ConfigDescriptionParameter> configParams = null;
        ThingType thingType = thingTypeRegistry.getThingType(thing.getThingTypeUID());
        if (thingType != null) {
            configParams = getConfigDescriptionParameters(thingType.getConfigDescriptionURI());
        }
        return configParams != null ? configParams : List.of();
    }

    private List<ConfigDescriptionParameter> getConfigDescriptionParameters(Channel channel) {
        List<ConfigDescriptionParameter> configParams = null;
        ChannelTypeUID channelTypeUID = channel.getChannelTypeUID();
        if (channelTypeUID != null) {
            ChannelType channelType = channelTypeRegistry.getChannelType(channelTypeUID);
            if (channelType != null) {
                configParams = getConfigDescriptionParameters(channelType.getConfigDescriptionURI());
            }
        }
        return configParams != null ? configParams : List.of();
    }

    private @Nullable List<ConfigDescriptionParameter> getConfigDescriptionParameters(@Nullable URI descURI) {
        if (descURI != null) {
            ConfigDescription configDesc = configDescRegistry.getConfigDescription(descURI);
            if (configDesc != null) {
                return configDesc.getParameters();
            }
        }
        return null;
    }

    /**
     * Get non default channels.
     * It includes extensible channels and channels with a non default configuration.
     *
     * @param thing the thing
     * @return the list of channels
     */
    protected List<Channel> getNonDefaultChannels(Thing thing) {
        ThingType thingType = thingTypeRegistry.getThingType(thing.getThingTypeUID());
        List<String> ids = thingType != null ? thingType.getExtensibleChannelTypeIds() : List.of();
        return thing
                .getChannels().stream().filter(ch -> ch.getChannelTypeUID() == null
                        || ids.contains(ch.getChannelTypeUID().getId()) || channelWithNonDefaultConfig(ch))
                .collect(Collectors.toList());
    }

    private boolean channelWithNonDefaultConfig(Channel channel) {
        for (ConfigDescriptionParameter param : getConfigDescriptionParameters(channel)) {
            Object value = channel.getConfiguration().get(param.getName());
            if (value != null) {
                value = ConfigUtil.normalizeType(value, param);
                if (!value.equals(ConfigUtil.getDefaultValueAsCorrectType(param))) {
                    return true;
                }
            }
        }
        return false;
    }
}
