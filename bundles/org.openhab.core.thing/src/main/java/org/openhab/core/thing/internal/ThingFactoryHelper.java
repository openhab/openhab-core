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
package org.openhab.core.thing.internal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingFactory;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelGroupDefinition;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeRegistry;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ThingType;
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
 * @author Simon Kaufmann - Initial contribution
 * @author Kai Kreuzer - Changed creation of channels to not require a thing type
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

        final ChannelUID channelUID = new ChannelUID(thingUID, groupId, channelDefinition.getId());
        final ChannelBuilder channelBuilder = createChannelBuilder(channelUID, type, configDescriptionRegistry);

        // If we want to override the label, add it...
        final String label = channelDefinition.getLabel();
        if (label != null) {
            channelBuilder.withLabel(label);
        }

        // If we want to override the description, add it...
        final String description = channelDefinition.getDescription();
        if (description != null) {
            channelBuilder.withDescription(description);
        }

        return channelBuilder.withProperties(channelDefinition.getProperties()).build();
    }

    static ChannelBuilder createChannelBuilder(ChannelUID channelUID, ChannelType channelType,
            ConfigDescriptionRegistry configDescriptionRegistry) {
        final ChannelBuilder channelBuilder = ChannelBuilder.create(channelUID, channelType.getItemType()) //
                .withType(channelType.getUID()) //
                .withDefaultTags(channelType.getTags()) //
                .withKind(channelType.getKind()) //
                .withLabel(channelType.getLabel()) //
                .withAutoUpdatePolicy(channelType.getAutoUpdatePolicy());

        String description = channelType.getDescription();
        if (description != null) {
            channelBuilder.withDescription(description);
        }

        // Initialize channel configuration with default-values
        if (channelType.getConfigDescriptionURI() != null) {
            final Configuration configuration = new Configuration();
            applyDefaultConfiguration(configuration, channelType, configDescriptionRegistry);
            channelBuilder.withConfiguration(configuration);
        }

        return channelBuilder;
    }

    /**
     * Apply the {@link ThingType}'s default values to the given {@link Configuration}.
     *
     * @param configuration the {@link Configuration} where the default values should be added (must not be null)
     * @param thingType the {@link ThingType} where to look for the default values (must not be null)
     * @param configDescriptionRegistry the {@link ConfigDescriptionRegistry} to use (may be null, but method won't have
     *            any effect then)
     */
    public static void applyDefaultConfiguration(Configuration configuration, ThingType thingType,
            @Nullable ConfigDescriptionRegistry configDescriptionRegistry) {
        URI configDescriptionURI = thingType.getConfigDescriptionURI();
        if (configDescriptionURI != null && configDescriptionRegistry != null) {
            // Set default values to thing configuration
            ConfigUtil.applyDefaultConfiguration(configuration,
                    configDescriptionRegistry.getConfigDescription(configDescriptionURI));
        }
    }

    /**
     * Apply the {@link ChannelType}'s default values to the given {@link Configuration}.
     *
     * @param configuration the {@link Configuration} where the default values should be added (must not be null)
     * @param channelType the {@link ChannelType} where to look for the default values (must not be null)
     * @param configDescriptionRegistry the {@link ConfigDescriptionRegistry} to use (may be null, but method won't have
     *            any effect then)
     */
    public static void applyDefaultConfiguration(Configuration configuration, ChannelType channelType,
            @Nullable ConfigDescriptionRegistry configDescriptionRegistry) {
        URI configDescriptionURI = channelType.getConfigDescriptionURI();
        if (configDescriptionURI != null && configDescriptionRegistry != null) {
            // Set default values to channel configuration
            ConfigUtil.applyDefaultConfiguration(configuration,
                    configDescriptionRegistry.getConfigDescription(configDescriptionURI));
        }
    }
}
