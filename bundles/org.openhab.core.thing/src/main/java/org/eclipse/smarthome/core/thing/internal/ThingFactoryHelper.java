/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.thing.internal;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;
import org.eclipse.smarthome.config.core.ConfigDescriptionRegistry;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingFactory;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelGroupDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeRegistry;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeRegistry;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for creation of Things.
 *
 * It is supposed to contain methods that are commonly shared between {@link ThingManagerImpl} and {@link ThingFactory}.
 *
 * @author Simon Kaufmann - Initial contribution and API
 * @author Kai Kreuzer - Changed creation of channels to not require a thing type
 *
 */
public class ThingFactoryHelper {

    private static Logger logger = LoggerFactory.getLogger(ThingFactoryHelper.class);

    /**
     * Create {@link Channel} instances for the given Thing.
     *
     * @param thingType the type of the Thing (must not be null)
     * @param thingUID the Thing's UID (must not be null)
     * @param configDescriptionRegistry {@link ConfigDescriptionRegistry} that will be used to initialize the
     *            {@link Channel}s with their corresponding default values, if given.
     * @return a list of {@link Channel}s
     */
    public static List<Channel> createChannels(ThingType thingType, ThingUID thingUID,
            ConfigDescriptionRegistry configDescriptionRegistry) {
        List<Channel> channels = new ArrayList<>();
        List<ChannelDefinition> channelDefinitions = thingType.getChannelDefinitions();
        for (ChannelDefinition channelDefinition : channelDefinitions) {
            Channel channel = createChannel(channelDefinition, thingUID, null, configDescriptionRegistry);
            if (channel != null) {
                channels.add(channel);
            }
        }
        List<ChannelGroupDefinition> channelGroupDefinitions = thingType.getChannelGroupDefinitions();
        withChannelGroupTypeRegistry(channelGroupTypeRegistry -> {
            for (ChannelGroupDefinition channelGroupDefinition : channelGroupDefinitions) {
                ChannelGroupType channelGroupType = null;
                if (channelGroupTypeRegistry != null) {
                    channelGroupType = channelGroupTypeRegistry
                            .getChannelGroupType(channelGroupDefinition.getTypeUID());
                }
                if (channelGroupType != null) {
                    List<ChannelDefinition> channelGroupChannelDefinitions = channelGroupType.getChannelDefinitions();
                    for (ChannelDefinition channelDefinition : channelGroupChannelDefinitions) {
                        Channel channel = createChannel(channelDefinition, thingUID, channelGroupDefinition.getId(),
                                configDescriptionRegistry);
                        if (channel != null) {
                            channels.add(channel);
                        }
                    }
                } else {
                    logger.warn(
                            "Could not create channels for channel group '{}' for thing type '{}', because channel group type '{}' could not be found.",
                            channelGroupDefinition.getId(), thingUID, channelGroupDefinition.getTypeUID());
                }
            }
            return null;
        });
        return channels;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static <T> T withChannelGroupTypeRegistry(Function<ChannelGroupTypeRegistry, T> consumer) {
        BundleContext bundleContext = FrameworkUtil.getBundle(ThingFactoryHelper.class).getBundleContext();
        ServiceReference ref = bundleContext.getServiceReference(ChannelGroupTypeRegistry.class.getName());
        try {
            ChannelGroupTypeRegistry channelGroupTypeRegistry = null;
            if (ref != null) {
                channelGroupTypeRegistry = (ChannelGroupTypeRegistry) bundleContext.getService(ref);
            }
            return consumer.apply(channelGroupTypeRegistry);
        } finally {
            if (ref != null) {
                bundleContext.ungetService(ref);
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static <T> T withChannelTypeRegistry(Function<ChannelTypeRegistry, T> consumer) {
        BundleContext bundleContext = FrameworkUtil.getBundle(ThingFactoryHelper.class).getBundleContext();
        ServiceReference ref = bundleContext.getServiceReference(ChannelTypeRegistry.class.getName());
        try {
            ChannelTypeRegistry channelTypeRegistry = null;
            if (ref != null) {
                channelTypeRegistry = (ChannelTypeRegistry) bundleContext.getService(ref);
            }
            return consumer.apply(channelTypeRegistry);
        } finally {
            if (ref != null) {
                bundleContext.ungetService(ref);
            }
        }
    }

    private static Channel createChannel(ChannelDefinition channelDefinition, ThingUID thingUID, String groupId,
            ConfigDescriptionRegistry configDescriptionRegistry) {
        ChannelType type = withChannelTypeRegistry(channelTypeRegistry -> {
            return (channelTypeRegistry != null)
                    ? channelTypeRegistry.getChannelType(channelDefinition.getChannelTypeUID())
                    : null;
        });
        if (type == null) {
            logger.warn(
                    "Could not create channel '{}' for thing type '{}', because channel type '{}' could not be found.",
                    channelDefinition.getId(), thingUID, channelDefinition.getChannelTypeUID());
            return null;
        }

        ChannelUID channelUID = new ChannelUID(thingUID, groupId, channelDefinition.getId());
        ChannelBuilder channelBuilder = createChannelBuilder(channelUID, type, configDescriptionRegistry);

        // If we want to override the label, add it...
        final String label = channelDefinition.getLabel();
        if (label != null) {
            channelBuilder = channelBuilder.withLabel(label);
        }

        // If we want to override the description, add it...
        final String description = channelDefinition.getDescription();
        if (description != null) {
            channelBuilder = channelBuilder.withDescription(description);
        }

        channelBuilder = channelBuilder.withProperties(channelDefinition.getProperties());
        return channelBuilder.build();
    }

    static ChannelBuilder createChannelBuilder(ChannelUID channelUID, ChannelType channelType,
            ConfigDescriptionRegistry configDescriptionRegistry) {
        ChannelBuilder channelBuilder = ChannelBuilder.create(channelUID, channelType.getItemType()) //
                .withType(channelType.getUID()) //
                .withDefaultTags(channelType.getTags()) //
                .withKind(channelType.getKind()) //
                .withLabel(channelType.getLabel()) //
                .withAutoUpdatePolicy(channelType.getAutoUpdatePolicy());

        String description = channelType.getDescription();
        if (description != null) {
            channelBuilder = channelBuilder.withDescription(description);
        }

        // Initialize channel configuration with default-values
        URI channelConfigDescriptionURI = channelType.getConfigDescriptionURI();
        if (configDescriptionRegistry != null && channelConfigDescriptionURI != null) {
            ConfigDescription cd = configDescriptionRegistry.getConfigDescription(channelConfigDescriptionURI);
            if (cd != null) {
                Configuration config = new Configuration();
                for (ConfigDescriptionParameter param : cd.getParameters()) {
                    String defaultValue = param.getDefault();
                    if (defaultValue != null) {
                        Object value = getDefaultValueAsCorrectType(param.getType(), defaultValue);
                        if (value != null) {
                            config.put(param.getName(), value);
                        }
                    }
                }
                channelBuilder = channelBuilder.withConfiguration(config);
            }
        }

        return channelBuilder;
    }

    /**
     * Map the provided (default) value of the given {@link Type} to the corresponding Java type.
     *
     * In case the provided value is supposed to be a number and cannot be converted into the target type correctly,
     * this method will return <code>null</code> while logging a warning.
     *
     * @param parameterType the {@link Type} of the value
     * @param defaultValue the value that should be converted
     * @return the given value as the corresponding Java type or <code>null</code> if the value could not be converted
     */
    public static Object getDefaultValueAsCorrectType(Type parameterType, String defaultValue) {
        try {
            switch (parameterType) {
                case TEXT:
                    return defaultValue;
                case BOOLEAN:
                    return Boolean.parseBoolean(defaultValue);
                case INTEGER:
                    return new BigDecimal(defaultValue);
                case DECIMAL:
                    return new BigDecimal(defaultValue);
                default:
                    return null;
            }
        } catch (NumberFormatException ex) {
            LoggerFactory.getLogger(ThingFactoryHelper.class).warn(
                    "Could not parse default value '{}' as type '{}': {}", defaultValue, parameterType, ex.getMessage(),
                    ex);
            return null;
        }
    }

    /**
     * Apply the {@link ThingType}'s default values to the given {@link Configuration}.
     *
     * @param configuration the {@link Configuration} where the default values should be added (may be null,
     *            but method won't have any effect then)
     * @param thingType the {@link ThingType} where to look for the default values (must not be null)
     * @param configDescriptionRegistry the {@link ConfigDescriptionRegistry} to use (may be null, but method won't have
     *            any effect then)
     */
    public static void applyDefaultConfiguration(Configuration configuration, ThingType thingType,
            ConfigDescriptionRegistry configDescriptionRegistry) {
        if (configDescriptionRegistry != null && configuration != null) {
            // Set default values to thing-configuration
            if (thingType.getConfigDescriptionURI() != null) {
                ConfigDescription thingConfigDescription = configDescriptionRegistry
                        .getConfigDescription(thingType.getConfigDescriptionURI());
                if (thingConfigDescription != null) {
                    for (ConfigDescriptionParameter parameter : thingConfigDescription.getParameters()) {
                        String defaultValue = parameter.getDefault();
                        if (defaultValue != null && configuration.get(parameter.getName()) == null) {
                            Object value = ThingFactoryHelper.getDefaultValueAsCorrectType(parameter.getType(),
                                    defaultValue);
                            if (value != null) {
                                configuration.put(parameter.getName(), value);
                            }
                        }
                    }
                }
            }
        }
    }

}
