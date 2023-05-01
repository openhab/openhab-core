/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
import org.openhab.core.storage.json.internal.JsonStorage;
import org.openhab.core.thing.internal.link.ItemChannelLinkConfigDescriptionProvider;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.types.util.UnitUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Upgrader} contains the implementation of the upgrade methods
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class Upgrader {
    public static final String ITEM_COPY_UNIT_TO_METADATA = "itemCopyUnitToMetadata";
    public static final String LINK_UPGRADE_JS_PROFILE = "linkUpgradeJsProfile";

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
        if (checkUpgradeRecord(ITEM_COPY_UNIT_TO_METADATA)) {
            return;
        }
        Path itemJsonDatabasePath = Path.of(baseDir, "jsondb", "org.openhab.core.items.Item.json");
        Path metadataJsonDatabasePath = Path.of(baseDir, "jsondb", "org.openhab.core.items.Metadata.json");
        logger.info("Copying item unit from state description to metadata in database '{}'", itemJsonDatabasePath);

        if (!Files.isReadable(itemJsonDatabasePath)) {
            logger.error("Cannot access item database '{}', check path and access rights.", itemJsonDatabasePath);
            return;
        }
        if (!Files.isWritable(metadataJsonDatabasePath)) {
            logger.error("Cannot access metadata database '{}', check path and access rights.",
                    metadataJsonDatabasePath);
            return;
        }

        JsonStorage<ManagedItemProvider.PersistedItem> itemStorage = new JsonStorage<>(itemJsonDatabasePath.toFile(),
                null, 5, 0, 0, List.of());
        JsonStorage<Metadata> metadataStorage = new JsonStorage<>(metadataJsonDatabasePath.toFile(), null, 5, 0, 0,
                List.of());

        itemStorage.getKeys().forEach(itemName -> {
            ManagedItemProvider.PersistedItem item = itemStorage.get(itemName);
            if (item != null && item.itemType.startsWith("Number:")) {
                if (metadataStorage.containsKey("unit" + ":" + itemName)) {
                    logger.debug("{}: already contains a 'unit' metadata, skipping it", itemName);
                } else {
                    Metadata metadata = metadataStorage.get("stateDescription:" + itemName);
                    if (metadata == null) {
                        logger.debug("{}: Nothing to do, no state description found.", itemName);
                    } else {
                        String pattern = (String) metadata.getConfiguration().get("pattern");
                        if (pattern.contains(UnitUtils.UNIT_PLACEHOLDER)) {
                            logger.warn(
                                    "{}: State description contains unit place-holder '%unit%', check if 'unit' metadata is needed!",
                                    itemName);
                        } else {
                            Unit<?> stateDescriptionUnit = UnitUtils.parseUnit(pattern);
                            if (stateDescriptionUnit != null) {
                                String unit = stateDescriptionUnit.toString();
                                MetadataKey defaultUnitMetadataKey = new MetadataKey("unit", itemName);
                                Metadata defaultUnitMetadata = new Metadata(defaultUnitMetadataKey, unit, null);
                                metadataStorage.put(defaultUnitMetadataKey.toString(), defaultUnitMetadata);
                                logger.info("{}: Wrote 'unit={}' to metadata.", itemName, unit);
                            }
                        }
                    }
                }
            }
        });

        metadataStorage.flush();
        upgradeRecords.put(ITEM_COPY_UNIT_TO_METADATA, new UpgradeRecord(ZonedDateTime.now()));
    }

    public void linkUpgradeJsProfile() {
        if (checkUpgradeRecord(LINK_UPGRADE_JS_PROFILE)) {
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
    }

    private static class UpgradeRecord {
        public final ZonedDateTime executionDate;

        public UpgradeRecord(ZonedDateTime executionDate) {
            this.executionDate = executionDate;
        }
    }
}
