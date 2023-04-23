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
package org.openhab.core.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import javax.measure.Unit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.items.ManagedItemProvider.PersistedItem;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.storage.json.internal.JsonStorage;
import org.openhab.core.thing.internal.link.ItemChannelLinkConfigDescriptionProvider;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.types.util.UnitUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link UpgradeTool} is a tool for upgrading openHAB to mitigate breaking changes
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class UpgradeTool {
    private static final Logger LOGGER = LoggerFactory.getLogger("UpgradeTool");

    private static final String CMD_OPT_ITEM = "item";
    private static final String CMD_OPT_LINK = "link";
    private static final String CMD_OPT_DIR = "dir";

    private static Options getOptions() {
        Options options = new Options();

        options.addOption(
                Option.builder().longOpt(CMD_OPT_DIR).desc("directory to process").numberOfArgs(1).required().build());

        // add the group for available options
        OptionGroup operation = new OptionGroup();
        operation.setRequired(true);
        operation.addOption(Option.builder().longOpt(CMD_OPT_ITEM).numberOfArgs(1)
                .desc("perform the given operation on the item database").build());
        operation.addOption(Option.builder().longOpt(CMD_OPT_LINK).numberOfArgs(1)
                .desc("perform the given operation on the link database").build());

        options.addOptionGroup(operation);

        return options;
    }

    private static void itemCopyUnitToMetadata(String baseDir) {
        Path itemJsonDatabasePath = Path.of(baseDir, "jsondb", "org.openhab.core.items.Item.json");
        Path metadataJsonDatabasePath = Path.of(baseDir, "jsondb", "org.openhab.core.items.Metadata.json");
        LOGGER.info("Copying item unit from state description to metadata in database '{}'", itemJsonDatabasePath);

        if (!Files.isReadable(itemJsonDatabasePath)) {
            LOGGER.error("Cannot access item database '{}', check path and access rights.", itemJsonDatabasePath);
            return;
        }
        if (!Files.isWritable(metadataJsonDatabasePath)) {
            LOGGER.error("Cannot access metadata database '{}', check path and access rights.",
                    metadataJsonDatabasePath);
            return;
        }

        JsonStorage<PersistedItem> itemStorage = new JsonStorage<>(itemJsonDatabasePath.toFile(), null, 5, 0, 0,
                List.of());
        JsonStorage<Metadata> metadataStorage = new JsonStorage<>(metadataJsonDatabasePath.toFile(), null, 5, 0, 0,
                List.of());

        itemStorage.getKeys().forEach(itemName -> {
            PersistedItem item = itemStorage.get(itemName);
            if (item != null && item.itemType.startsWith("Number:")) {
                if (metadataStorage.containsKey("unit:" + itemName)) {
                    LOGGER.info("{}: already contains a 'unit' metadata, skipping it", itemName);
                } else {
                    Metadata metadata = metadataStorage.get("stateDescription:" + itemName);
                    if (metadata == null) {
                        LOGGER.info("{}: Nothing to do, no state description found.", itemName);
                    } else {
                        String pattern = (String) metadata.getConfiguration().get("pattern");
                        if (pattern.contains(UnitUtils.UNIT_PLACEHOLDER)) {
                            LOGGER.info(
                                    "{}: State description contains unit place-holder '%unit%', check if 'unit' metadata is needed!",
                                    itemName);
                        } else {
                            Unit<?> stateDescriptionUnit = UnitUtils.parseUnit(pattern);
                            if (stateDescriptionUnit != null) {
                                String unit = stateDescriptionUnit.toString();
                                MetadataKey defaultUnitMetadataKey = new MetadataKey("unit", itemName);
                                Metadata defaultUnitMetadata = new Metadata(defaultUnitMetadataKey, unit, null);
                                metadataStorage.put(defaultUnitMetadataKey.toString(), defaultUnitMetadata);
                                LOGGER.info("{}: Wrote 'unit={}' to metadata.", itemName, unit);
                            }
                        }
                    }
                }
            }
        });

        metadataStorage.flush();
    }

    private static void upgradeJsProfile(String baseDir) {
        Path linkJsonDatabasePath = Path.of(baseDir, "jsondb", "org.openhab.core.thing.link.ItemChannelLink.json");
        LOGGER.info("Upgrading JS profile configuration in database '{}'", linkJsonDatabasePath);

        if (!Files.isWritable(linkJsonDatabasePath)) {
            LOGGER.error("Cannot access link database '{}', check path and access rights.", linkJsonDatabasePath);
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
                    LOGGER.info("{}: rewrote JS profile link to new format", linkUid);
                } else {
                    LOGGER.info("{}: link already has correct configuration", linkUid);
                }
            }
        });
        linkStorage.flush();
    }

    public static void main(String[] args) {
        Options options = getOptions();

        try {
            CommandLine commandLine = new DefaultParser().parse(options, args);
            String baseDir = commandLine.hasOption(CMD_OPT_DIR) ? commandLine.getOptionValue(CMD_OPT_DIR) : "";
            if (commandLine.hasOption(CMD_OPT_ITEM)) {
                String operation = commandLine.getOptionValue(CMD_OPT_ITEM);
                switch (operation) {
                    case "copyUnitToMetadata":
                        itemCopyUnitToMetadata(baseDir);
                        break;
                    default:
                        System.out.println("Available tasks for operation 'item': copyUnitToMetadata");
                }
            } else if (commandLine.hasOption(CMD_OPT_LINK)) {
                String operation = commandLine.getOptionValue(CMD_OPT_LINK);
                switch (operation) {
                    case "upgradeJsProfile":
                        upgradeJsProfile(baseDir);
                        break;
                    default:
                        System.out.println("Available tasks for operation 'link': upgradeJsProfile");
                }
            }
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("upgradetool", options);
        }

        System.exit(0);
    }
}
