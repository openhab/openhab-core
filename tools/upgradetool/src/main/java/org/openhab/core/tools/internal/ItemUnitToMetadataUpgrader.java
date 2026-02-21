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
package org.openhab.core.tools.internal;

import static org.openhab.core.thing.DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_ATMOSPHERIC_HUMIDITY;
import static org.openhab.core.thing.DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_BATTERY_LEVEL;
import static org.openhab.core.thing.DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_COLOR_TEMPERATURE_ABS;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.ManagedItemProvider;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.storage.json.internal.JsonStorage;
import org.openhab.core.thing.dto.ThingDTO;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.tools.Upgrader;
import org.openhab.core.types.util.UnitUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ItemUnitToMetadataUpgrader} copies the unit from the item to the metadata.
 *
 * @author Jan N. Klug - Initial contribution
 * @author Jimmy Tanagra - Refactored into a separate class
 */
@NonNullByDefault
public class ItemUnitToMetadataUpgrader implements Upgrader {
    private final Logger logger = LoggerFactory.getLogger(ItemUnitToMetadataUpgrader.class);

    @Override
    public String getName() {
        return "itemCopyUnitToMetadata"; // keep the old name for backwards compatibility
    }

    @Override
    public String getDescription() {
        return "Copy item unit from state description to metadata";
    }

    @Override
    public boolean execute(@Nullable Path userdataPath, @Nullable Path confPath) {
        if (userdataPath == null) {
            logger.error("{} skipped: no userdata directory found.", getName());
            return false;
        }

        Path dataPath = userdataPath.resolve("jsondb");
        boolean noLink;

        Path itemJsonDatabasePath = dataPath.resolve("org.openhab.core.items.Item.json");
        Path metadataJsonDatabasePath = dataPath.resolve("org.openhab.core.items.Metadata.json");
        Path linkJsonDatabasePath = dataPath.resolve("org.openhab.core.thing.link.ItemChannelLink.json");
        Path thingJsonDatabasePath = dataPath.resolve("org.openhab.core.thing.Thing.json");
        logger.info("Copying item unit from state description to metadata in database '{}'", itemJsonDatabasePath);

        if (!Files.isReadable(itemJsonDatabasePath)) {
            logger.error("Cannot access item database '{}', check path and access rights.", itemJsonDatabasePath);
            return false;
        }

        if (!Files.isReadable(linkJsonDatabasePath) || !Files.isReadable(thingJsonDatabasePath)) {
            logger.warn("Cannot access thing or link database '{}', update may be incomplete.", linkJsonDatabasePath);
            noLink = true;
        } else {
            noLink = false;
        }

        // missing metadata database is also fine, we create one in that case
        if (!Files.isWritable(metadataJsonDatabasePath) && Files.exists(metadataJsonDatabasePath)) {
            logger.error("Cannot access metadata database '{}', check path and access rights.",
                    metadataJsonDatabasePath);
            return false;
        }

        JsonStorage<ManagedItemProvider.PersistedItem> itemStorage = new JsonStorage<>(itemJsonDatabasePath.toFile(),
                null, 5, 0, 0, List.of());
        JsonStorage<Metadata> metadataStorage = new JsonStorage<>(metadataJsonDatabasePath.toFile(), null, 5, 0, 0,
                List.of());
        JsonStorage<ItemChannelLink> linkStorage = noLink ? null
                : new JsonStorage<>(linkJsonDatabasePath.toFile(), null, 5, 0, 0, List.of());
        JsonStorage<ThingDTO> thingStorage = noLink ? null
                : new JsonStorage<>(thingJsonDatabasePath.toFile(), null, 5, 0, 0, List.of());

        itemStorage.getKeys().forEach(itemName -> {
            ManagedItemProvider.PersistedItem item = itemStorage.get(itemName);
            if (item != null && item.itemType.startsWith("Number:")) {
                if (metadataStorage.containsKey(NumberItem.UNIT_METADATA_NAMESPACE + ":" + itemName)) {
                    logger.debug("{}: Already contains a 'unit' metadata, skipping it", itemName);
                } else {
                    String unit = null;
                    if (linkStorage != null && thingStorage != null) {
                        List<ItemChannelLink> links = linkStorage.getValues().stream().map(Objects::requireNonNull)
                                .filter(link -> itemName.equals(link.getItemName())).toList();
                        // check if we can find the channel for these links
                        for (ItemChannelLink link : links) {
                            ThingDTO thing = thingStorage.get(link.getLinkedUID().getThingUID().toString());
                            if (thing == null) {
                                logger.info(
                                        "{}: Could not find thing for channel '{}'. Check if you need to set unit metadata.",
                                        itemName, link.getLinkedUID());
                                continue;
                            }
                            String channelTypeUID = thing.channels.stream()
                                    .filter(channel -> link.getLinkedUID().toString().equals(channel.uid))
                                    .map(channel -> channel.channelTypeUID).findFirst().orElse(null);
                            if (channelTypeUID == null) {
                                continue;
                            }
                            // replace . with :, if the database is already correct, we can ignore that
                            channelTypeUID = channelTypeUID.replace(".", ":");
                            if (channelTypeUID.startsWith("system")) {
                                if (channelTypeUID.equals(SYSTEM_CHANNEL_TYPE_UID_BATTERY_LEVEL.toString())
                                        || channelTypeUID
                                                .equals(SYSTEM_CHANNEL_TYPE_UID_ATMOSPHERIC_HUMIDITY.toString())) {
                                    unit = "%";
                                } else if (channelTypeUID
                                        .equals(SYSTEM_CHANNEL_TYPE_UID_COLOR_TEMPERATURE_ABS.toString())) {
                                    unit = "K";
                                }
                            } else {
                                logger.warn(
                                        "{}: Could not determine if channel '{}' sets a state description. Check if you need to set unit metadata.",
                                        itemName, link.getLinkedUID());
                            }
                        }
                    }

                    // metadata state description has higher priority, so we check that and override the unit if
                    // necessary
                    Metadata metadata = metadataStorage.get("stateDescription:" + itemName);
                    if (metadata == null) {
                        logger.debug("{}: No state description in metadata found.", itemName);
                    } else {
                        String pattern = (String) metadata.getConfiguration().get("pattern");
                        if (pattern != null) {
                            if (pattern.contains(UnitUtils.UNIT_PLACEHOLDER)) {
                                logger.warn(
                                        "{}: State description contains unit place-holder '%unit%', check if 'unit' metadata is needed!",
                                        itemName);
                            } else {
                                Unit<?> stateDescriptionUnit = UnitUtils.parseUnit(pattern);
                                if (stateDescriptionUnit != null) {
                                    unit = stateDescriptionUnit.toString();
                                }
                            }
                        } else {
                            logger.debug("{}: Nothing to do, no pattern found.", itemName);
                        }
                    }
                    if (unit != null) {
                        MetadataKey defaultUnitMetadataKey = new MetadataKey(NumberItem.UNIT_METADATA_NAMESPACE,
                                itemName);
                        Metadata defaultUnitMetadata = new Metadata(defaultUnitMetadataKey, unit, null);
                        metadataStorage.put(defaultUnitMetadataKey.toString(), defaultUnitMetadata);
                        logger.info("{}: Wrote 'unit={}' to metadata.", itemName, unit);
                    }
                }
            }
        });

        metadataStorage.flush();

        return true;
    }
}
