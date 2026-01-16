/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.items.fileconverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.Metadata;
import org.openhab.core.library.CoreItemFactory;

/**
 * {@link AbstractItemFileGenerator} is the base class for any {@link Item} file generator.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractItemFileGenerator implements ItemFileGenerator {

    public AbstractItemFileGenerator() {
    }

    /**
     * {@link ConfigParameter} is a container for any configuration parameter defined by a name and a value.
     */
    public record ConfigParameter(String name, Object value) {
    }

    /**
     * Get the list of available channel links for an item, sorted by natural order of their channel UID.
     *
     * @param metadata a collection of metadata
     * @param itemName the item name
     * @return the sorted list of metadata representing the channel links for this item
     */
    protected List<Metadata> getChannelLinks(Collection<Metadata> metadata, String itemName) {
        return metadata.stream().filter(
                md -> "channel".equals(md.getUID().getNamespace()) && md.getUID().getItemName().equals(itemName))
                .sorted((md1, md2) -> {
                    return md1.getValue().compareTo(md2.getValue());
                }).collect(Collectors.toList());
    }

    /**
     * Get the list of available metadata for an item, sorted by natural order of their namespaces.
     * The "semantics" and "channel" namespaces are ignored.
     *
     * @param metadata a collection of metadata
     * @param itemName the item name
     * @return the sorted list of metadata for this item
     */
    protected List<Metadata> getMetadata(Collection<Metadata> metadata, String itemName) {
        return metadata.stream()
                .filter(md -> !"semantics".equals(md.getUID().getNamespace())
                        && !"channel".equals(md.getUID().getNamespace()) && md.getUID().getItemName().equals(itemName))
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
