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
package org.openhab.core.tools.internal;

import static org.openhab.core.thing.DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_ATMOSPHERIC_HUMIDITY;
import static org.openhab.core.thing.DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_BATTERY_LEVEL;
import static org.openhab.core.thing.DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_COLOR_TEMPERATURE_ABS;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.items.ManagedItemProvider;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.storage.json.internal.JsonStorage;
import org.openhab.core.thing.dto.ThingDTO;
import org.openhab.core.thing.internal.link.ItemChannelLinkConfigDescriptionProvider;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.types.util.UnitUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Upgrader} contains the implementation of the upgrade methods
 *
 * @author Jan N. Klug - Initial contribution
 * @author Florian Hotze - Add script profile upgrade
 */
@NonNullByDefault
public class Upgrader {
    public static final String ITEM_COPY_UNIT_TO_METADATA = "itemCopyUnitToMetadata";
    public static final String LINK_UPGRADE_JS_PROFILE = "linkUpgradeJsProfile";
    public static final String LINK_UPGRADE_SCRIPT_PROFILE = "linkUpgradeScriptProfile";

    private final Logger logger = LoggerFactory.getLogger(Upgrader.class);
    private final String baseDir;
    private final boolean force;
    private final JsonStorage<UpgradeRecord> upgradeRecords;

    public Upgrader(String baseDir, boolean force) {
        this.baseDir = baseDir;
        this.force = force;

        Path upgradeJsonDatabasePath = Path.of(baseDir, "jsondb", "org.openhab.core.tools.UpgradeTool");

        upgradeRecords = new JsonStorage<>(upgradeJsonDatabasePath.toFile(), null, 5, 0, 0, List.of());
    }

    private boolean checkUpgradeRecord(String key) {
        UpgradeRecord upgradeRecord = upgradeRecords.get(key);
        if (upgradeRecord != null && !force) {
            logger.info("Already executed '{}' on {}. Use '--force'  to execute it again.", key,
                    upgradeRecord.executionDate);
            return false;
        }
        return true;
    }

    public void itemCopyUnitToMetadata() {
        boolean noLink;

        if (!checkUpgradeRecord(ITEM_COPY_UNIT_TO_METADATA)) {
            return;
        }
        Path itemJsonDatabasePath = Path.of(baseDir, "jsondb", "org.openhab.core.items.Item.json");
        Path metadataJsonDatabasePath = Path.of(baseDir, "jsondb", "org.openhab.core.items.Metadata.json");
        Path linkJsonDatabasePath = Path.of(baseDir, "jsondb", "org.openhab.core.thing.link.ItemChannelLink.json");
        Path thingJsonDatabasePath = Path.of(baseDir, "jsondb", "org.openhab.core.thing.Thing.json");
        logger.info("Copying item unit from state description to metadata in database '{}'", itemJsonDatabasePath);

        if (!Files.isReadable(itemJsonDatabasePath)) {
            logger.error("Cannot access item database '{}', check path and access rights.", itemJsonDatabasePath);
            return;
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
            return;
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
                    if (!noLink) {
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
        upgradeRecords.put(ITEM_COPY_UNIT_TO_METADATA, new UpgradeRecord(ZonedDateTime.now()));
        upgradeRecords.flush();
    }

    public void linkUpgradeJsProfile() {
        if (!checkUpgradeRecord(LINK_UPGRADE_JS_PROFILE)) {
            return;
        }

        Path linkJsonDatabasePath = Path.of(baseDir, "jsondb", "org.openhab.core.thing.link.ItemChannelLink.json");
        logger.info("Upgrading JS profile configuration in database '{}'", linkJsonDatabasePath);

        if (!Files.isWritable(linkJsonDatabasePath)) {
            logger.error("Cannot access link database '{}', check path and access rights.", linkJsonDatabasePath);
            return;
        }
        JsonStorage<ItemChannelLink> linkStorage = new JsonStorage<>(linkJsonDatabasePath.toFile(), null, 5, 0, 0,
                List.of());

        List.copyOf(linkStorage.getKeys()).forEach(linkUid -> {
            ItemChannelLink link = Objects.requireNonNull(linkStorage.get(linkUid));
            Configuration configuration = link.getConfiguration();
            String profileName = (String) configuration.get(ItemChannelLinkConfigDescriptionProvider.PARAM_PROFILE);
            if ("transform:JS".equals(profileName)) {
                String function = (String) configuration.get("function");
                if (function != null) {
                    configuration.put("toItemScript", function);
                    configuration.put("toHandlerScript", "|input");
                    configuration.remove("function");
                    configuration.remove("sourceFormat");

                    linkStorage.put(linkUid, link);
                    logger.info("{}: rewrote JS profile link to new format", linkUid);
                } else {
                    logger.info("{}: link already has correct configuration", linkUid);
                }
            }
        });

        linkStorage.flush();
        upgradeRecords.put(LINK_UPGRADE_JS_PROFILE, new UpgradeRecord(ZonedDateTime.now()));
        upgradeRecords.flush();
    }

    /**
     * Upgrades the ItemChannelLink database for the separation of {@code toHandlerScript} into
     * {@code commandFromItemScript} and {@code stateFromItemScript}.
     * See <a href="https://github.com/openhab/openhab-core/pull/4058">openhab/openhab-core#4058</a>.
     */
    public void linkUpgradeScriptProfile() {
        if (!checkUpgradeRecord(LINK_UPGRADE_SCRIPT_PROFILE)) {
            return;
        }

        Path linkJsonDatabasePath = Path.of(baseDir, "jsondb", "org.openhab.core.thing.link.ItemChannelLink.json");
        logger.info("Upgrading script profile configuration in database '{}'", linkJsonDatabasePath);

        if (!Files.isWritable(linkJsonDatabasePath)) {
            logger.error("Cannot access link database '{}', check path and access rights.", linkJsonDatabasePath);
            return;
        }
        JsonStorage<ItemChannelLink> linkStorage = new JsonStorage<>(linkJsonDatabasePath.toFile(), null, 5, 0, 0,
                List.of());

        List.copyOf(linkStorage.getKeys()).forEach(linkUid -> {
            ItemChannelLink link = Objects.requireNonNull(linkStorage.get(linkUid));
            Configuration configuration = link.getConfiguration();
            String profileName = (String) configuration.get(ItemChannelLinkConfigDescriptionProvider.PARAM_PROFILE);
            if (profileName != null && profileName.startsWith("transform:")) {
                String toHandlerScript = (String) configuration.get("toHandlerScript");
                if (toHandlerScript != null) {
                    configuration.put("commandFromItemScript", toHandlerScript);
                    configuration.remove("toHandlerScript");

                    linkStorage.put(linkUid, link);
                    logger.info("{}: rewrote script profile link to new format", linkUid);
                } else {
                    logger.info("{}: link already has correct configuration", linkUid);
                }
            }
        });

        linkStorage.flush();
        upgradeRecords.put(LINK_UPGRADE_SCRIPT_PROFILE, new UpgradeRecord(ZonedDateTime.now()));
        upgradeRecords.flush();
    }

    private static class UpgradeRecord {
        public final String executionDate;

        public UpgradeRecord(ZonedDateTime executionDate) {
            this.executionDate = executionDate.toString();
        }
    }
}
