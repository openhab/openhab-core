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
package org.openhab.core.model.yaml.internal.items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemProvider;
import org.openhab.core.items.dto.GroupFunctionDTO;
import org.openhab.core.items.dto.ItemDTOMapper;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.model.yaml.YamlModelListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link YamlItemProvider} is an OSGi service, that allows to define items in YAML configuration files.
 * Files can be added, updated or removed at runtime.
 * These items are automatically exposed to the {@link org.openhab.core.items.ItemRegistry}.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { ItemProvider.class, YamlItemProvider.class, YamlModelListener.class })
public class YamlItemProvider extends AbstractProvider<Item> implements ItemProvider, YamlModelListener<YamlItemDTO> {

    private final Logger logger = LoggerFactory.getLogger(YamlItemProvider.class);

    private CoreItemFactory itemFactory;
    private final YamlChannelLinkProvider itemChannelLinkProvider;
    private final YamlMetadataProvider metaDataProvider;

    private final Map<String, Collection<Item>> itemsMap = new ConcurrentHashMap<>();

    @Activate
    public YamlItemProvider(final @Reference CoreItemFactory itemFactory,
            final @Reference YamlChannelLinkProvider itemChannelLinkProvider,
            final @Reference YamlMetadataProvider metaDataProvider, Map<String, Object> properties) {
        this.itemFactory = itemFactory;
        this.itemChannelLinkProvider = itemChannelLinkProvider;
        this.metaDataProvider = metaDataProvider;
    }

    @Deactivate
    public void deactivate() {
        itemsMap.clear();
    }

    @Override
    public Collection<Item> getAll() {
        return itemsMap.values().stream().flatMap(list -> list.stream()).toList();
    }

    public Collection<Item> getAllFromModel(String modelName) {
        return itemsMap.getOrDefault(modelName, List.of());
    }

    @Override
    public Class<YamlItemDTO> getElementClass() {
        return YamlItemDTO.class;
    }

    @Override
    public boolean isVersionSupported(int version) {
        return version >= 2;
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

    @Override
    public void addedModel(String modelName, Collection<YamlItemDTO> elements) {
        Map<Item, YamlItemDTO> added = new LinkedHashMap<>();
        elements.forEach(elt -> {
            Item item = mapItem(elt);
            if (item != null) {
                added.put(item, elt);
            }
        });

        Collection<Item> modelItems = Objects
                .requireNonNull(itemsMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        modelItems.addAll(added.keySet());

        added.forEach((item, itemDTO) -> {
            String name = item.getName();
            logger.debug("model {} added item {}", modelName, name);
            notifyListenersAboutAddedElement(item);
            processChannelLinks(modelName, name, itemDTO);
            processMetadata(modelName, name, itemDTO);
        });
    }

    @Override
    public void updatedModel(String modelName, Collection<YamlItemDTO> elements) {
        Map<Item, YamlItemDTO> updated = new LinkedHashMap<>();
        elements.forEach(elt -> {
            Item item = mapItem(elt);
            if (item != null) {
                updated.put(item, elt);
            }
        });

        Collection<Item> modelItems = Objects
                .requireNonNull(itemsMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        updated.forEach((item, itemDTO) -> {
            String name = item.getName();
            modelItems.stream().filter(i -> i.getName().equals(name)).findFirst().ifPresentOrElse(oldItem -> {
                modelItems.remove(oldItem);
                modelItems.add(item);
                logger.debug("model {} updated item {}", modelName, name);
                notifyListenersAboutUpdatedElement(oldItem, item);
            }, () -> {
                modelItems.add(item);
                logger.debug("model {} added item {}", modelName, name);
                notifyListenersAboutAddedElement(item);
            });
            processChannelLinks(modelName, name, itemDTO);
            processMetadata(modelName, name, itemDTO);
        });
    }

    @Override
    public void removedModel(String modelName, Collection<YamlItemDTO> elements) {
        List<Item> removed = elements.stream().map(elt -> mapItem(elt)).filter(Objects::nonNull).toList();

        Collection<Item> modelItems = itemsMap.getOrDefault(modelName, List.of());
        removed.forEach(item -> {
            String name = item.getName();
            modelItems.stream().filter(i -> i.getName().equals(name)).findFirst().ifPresentOrElse(oldItem -> {
                modelItems.remove(oldItem);
                logger.debug("model {} removed item {}", modelName, name);
                notifyListenersAboutRemovedElement(oldItem);
            }, () -> logger.debug("model {} item {} not found", modelName, name));
            processChannelLinks(modelName, name, null);
            processMetadata(modelName, name, null);
        });

        if (modelItems.isEmpty()) {
            itemsMap.remove(modelName);
        }
    }

    private @Nullable Item mapItem(YamlItemDTO itemDTO) {
        String name = itemDTO.name;
        Item item;
        try {
            if (GroupItem.TYPE.equalsIgnoreCase(itemDTO.type)) {
                YamlGroupDTO groupDTO = itemDTO.group;
                if (groupDTO != null) {
                    Item baseItem = createItemOfType(groupDTO.getBaseType(), name);
                    if (baseItem != null) {
                        GroupFunctionDTO groupFunctionDto = new GroupFunctionDTO();
                        groupFunctionDto.name = groupDTO.getFunction();
                        groupFunctionDto.params = groupDTO.parameters != null
                                ? groupDTO.parameters.toArray(new String[0])
                                : new String[0];
                        item = new GroupItem(name, baseItem, ItemDTOMapper.mapFunction(baseItem, groupFunctionDto));
                    } else {
                        item = new GroupItem(name);
                    }
                } else {
                    item = new GroupItem(name);
                }
            } else {
                item = createItemOfType(itemDTO.getType(), name);
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Error creating item '{}', item will be ignored: {}", name, e.getMessage());
            item = null;
        }

        if (item instanceof GenericItem genericItem) {
            genericItem.setLabel(itemDTO.label);
            genericItem.setCategory(itemDTO.icon);

            if (itemDTO.tags != null) {
                for (String tag : itemDTO.tags) {
                    genericItem.addTag(tag);
                }
            }
            if (itemDTO.groups != null) {
                for (String groupName : itemDTO.groups) {
                    genericItem.addGroupName(groupName);
                }
            }
        }

        return item;
    }

    private @Nullable Item createItemOfType(@Nullable String itemType, String itemName) {
        if (itemType == null) {
            return null;
        }

        Item item = itemFactory.createItem(itemType, itemName);
        if (item != null) {
            logger.debug("Created item '{}' of type '{}'", itemName, itemType);
            return item;
        }

        logger.warn("CoreItemFactory cannot create item '{}' of type '{}'", itemName, itemType);
        return null;
    }

    private void processChannelLinks(String modelName, String itemName, @Nullable YamlItemDTO itemDTO) {
        Map<String, Configuration> channelLinks = new HashMap<>(2);
        if (itemDTO != null) {
            if (itemDTO.channel != null) {
                channelLinks.put(itemDTO.channel, new Configuration());
            }
            if (itemDTO.channels != null) {
                itemDTO.channels.forEach((channelUID, config) -> {
                    channelLinks.put(channelUID, new Configuration(config));
                });
            }
        }
        try {
            itemChannelLinkProvider.updateItemChannelLinks(modelName, itemName, channelLinks);
        } catch (Exception e) {
            logger.warn("Channel links configuration of item '{}' could not be parsed correctly.", itemName, e);
        }
    }

    private void processMetadata(String modelName, String itemName, @Nullable YamlItemDTO itemDTO) {
        Map<String, YamlMetadataDTO> metadata = new HashMap<>();
        if (itemDTO != null) {
            boolean hasAutoUpdateMetadata = false;
            boolean hasUnitMetadata = false;
            boolean hasStateDescriptionMetadata = false;
            if (itemDTO.metadata != null) {
                for (Map.Entry<String, YamlMetadataDTO> entry : itemDTO.metadata.entrySet()) {
                    if ("autoupdate".equals(entry.getKey())) {
                        hasAutoUpdateMetadata = true;
                    } else if ("unit".equals(entry.getKey())) {
                        hasUnitMetadata = true;
                    } else if ("stateDescription".equals(entry.getKey())) {
                        hasStateDescriptionMetadata = true;
                    }
                    Map<String, Object> config = entry.getValue().config;
                    if (itemDTO.format != null && "stateDescription".equals(entry.getKey())
                            && (entry.getValue().config == null || entry.getValue().config.get("pattern") == null)) {
                        config = new HashMap<>();
                        if (entry.getValue().config != null) {
                            for (Map.Entry<String, Object> confEntry : entry.getValue().config.entrySet()) {
                                config.put(confEntry.getKey(), confEntry.getValue());
                            }
                            config.put("pattern", itemDTO.format);
                        }
                    }
                    YamlMetadataDTO mdDTO = new YamlMetadataDTO();
                    mdDTO.value = entry.getValue().value;
                    mdDTO.config = config;
                    metadata.put(entry.getKey(), mdDTO);
                }
            }
            if (!hasAutoUpdateMetadata && itemDTO.autoupdate != null) {
                YamlMetadataDTO mdDTO = new YamlMetadataDTO();
                mdDTO.value = String.valueOf(itemDTO.autoupdate);
                metadata.put("autoupdate", mdDTO);
            }
            if (!hasUnitMetadata && itemDTO.unit != null) {
                YamlMetadataDTO mdDTO = new YamlMetadataDTO();
                mdDTO.value = itemDTO.unit;
                metadata.put("unit", mdDTO);
            }
            if (!hasStateDescriptionMetadata && itemDTO.format != null) {
                YamlMetadataDTO mdDTO = new YamlMetadataDTO();
                mdDTO.config = Map.of("pattern", itemDTO.format);
                metadata.put("stateDescription", mdDTO);
            }
        }
        metaDataProvider.updateMetadata(modelName, itemName, metadata);
    }
}
