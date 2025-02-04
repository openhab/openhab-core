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
package org.openhab.core.thing.syntax;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.Metadata;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.thing.link.ItemChannelLink;

/**
 * {@link AbstractItemSyntaxGenerator} is the base class for any {@link Item} syntax generator.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractItemSyntaxGenerator implements ItemSyntaxGenerator {

    private final ConfigDescriptionRegistry configDescRegistry;

    public AbstractItemSyntaxGenerator(ConfigDescriptionRegistry configDescRegistry) {
        this.configDescRegistry = configDescRegistry;
    }

    /**
     * {@link ConfigParameter} is a container for any configuration parameter defined by a name and a value.
     */
    protected record ConfigParameter(String name, Object value) {
    }

    /**
     * Get the list of available channel links for an item, sorted by natural order of their UID.
     *
     * @param channelLinks a collection of channel links
     * @param itemName the item name
     * @return the sorted list of channel links for this item
     */
    protected List<ItemChannelLink> getChannelLinks(Collection<ItemChannelLink> channelLinks, String itemName) {
        return channelLinks.stream().filter(link -> link.getItemName().equals(itemName)).sorted((link1, link2) -> {
            return link1.getLinkedUID().getAsString().compareTo(link2.getLinkedUID().getAsString());
        }).collect(Collectors.toList());
    }

    /**
     * Get the list of configuration parameters for a channel link.
     *
     * If a profile is set and a configuration description is found for this profile, the parameters are provided
     * in the same order as in this configuration description, and any parameter having the default value is ignored.
     * If no profile is set, the parameters are provided sorted by natural order of their names.
     *
     * @param channelLink the channel link
     * @param hideDefaultParameters true to hide the configuration parameters having the default value
     * @return a sorted list of configuration parameters for the channel link
     */
    protected List<ConfigParameter> getConfigurationParameters(ItemChannelLink channelLink,
            boolean hideDefaultParameters) {
        List<ConfigParameter> parameters = new ArrayList<>();
        Map<String, Object> configParameters = channelLink.getConfiguration().getProperties();
        Set<String> handledNames = new HashSet<>();
        Object profile = configParameters.get("profile");
        List<ConfigDescriptionParameter> configDescriptionParameter = List.of();
        if (profile instanceof String profileStr) {
            parameters.add(new ConfigParameter("profile", profileStr));
            handledNames.add("profile");
            try {
                ConfigDescription configDesc = configDescRegistry
                        .getConfigDescription(new URI("profile:" + profileStr));
                if (configDesc != null) {
                    configDescriptionParameter = configDesc.getParameters();
                }
            } catch (URISyntaxException e) {
                // Ignored; in practice this will never be thrown
            }
        }
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

    /**
     * Get the list of available metadata for an item, sorted by natural order of their namespaces.
     * The "semantics" namespace is ignored.
     *
     * @param metadata a collection of metadata
     * @param itemName the item name
     * @return the sorted list of metadata for this item
     */
    protected List<Metadata> getMetadata(Collection<Metadata> metadata, String itemName) {
        return metadata.stream().filter(
                md -> !"semantics".equals(md.getUID().getNamespace()) && md.getUID().getItemName().equals(itemName))
                .sorted((md1, md2) -> {
                    return md1.getUID().getNamespace().compareTo(md2.getUID().getNamespace());
                }).collect(Collectors.toList());
    }

    /**
     * Get the list of configuration parameters for a metadata, sorted by natural order of their names
     * with the exception of the "stateDescription" namespace where "min", "max" and "step" parameters
     * are provided at first in this order.
     *
     * @param metadata the metadata
     * @return a sorted list of configuration parameters for the metadata
     */
    protected List<ConfigParameter> getConfigurationParameters(Metadata metadata) {
        String namespace = metadata.getUID().getNamespace();
        Map<String, Object> configParams = metadata.getConfiguration();
        List<String> paramNames = configParams.keySet().stream().sorted((key1, key2) -> {
            if ("stateDescription".equals(namespace)) {
                if ("min".equals(key1)) {
                    return -1;
                } else if ("min".equals(key2)) {
                    return 1;
                } else if ("max".equals(key1)) {
                    return -1;
                } else if ("max".equals(key2)) {
                    return 1;
                } else if ("step".equals(key1)) {
                    return -1;
                } else if ("step".equals(key2)) {
                    return 1;
                }
            }
            return key1.compareTo(key2);
        }).collect(Collectors.toList());

        List<ConfigParameter> parameters = new ArrayList<>();
        for (String paramName : paramNames) {
            Object value = configParams.get(paramName);
            if (value != null) {
                parameters.add(new ConfigParameter(paramName, value));
            }
        }
        return parameters;
    }

    /**
     * Get the default state pattern for an item.
     *
     * @param item the item
     * @return the default state pattern of null if no default
     */
    protected @Nullable String getDefaultStatePattern(Item item) {
        String pattern = null;
        if (item instanceof GroupItem group) {
            Item baseItem = group.getBaseItem();
            if (baseItem != null) {
                pattern = getDefaultStatePattern(baseItem);
            }
        } else if (item.getType().startsWith(CoreItemFactory.NUMBER + ":")) {
            pattern = "%.0f %unit%";
        } else {
            switch (item.getType()) {
                case CoreItemFactory.STRING:
                    pattern = "%s";
                    break;
                case CoreItemFactory.DATETIME:
                    pattern = "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS";
                    break;
                case CoreItemFactory.NUMBER:
                    pattern = "%.0f";
                    break;
                default:
                    break;
            }
        }
        return pattern;
    }
}
