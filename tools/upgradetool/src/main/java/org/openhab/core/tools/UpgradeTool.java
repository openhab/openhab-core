/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import javax.measure.Unit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.items.ManagedItemProvider.PersistedItem;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.storage.json.internal.JsonStorage;
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

        options.addOptionGroup(operation);

        return options;
    }

    private static void itemCopyDefaultUnit(String baseDir) {
        Path itemJsonDatabasePath = Path.of(baseDir, "jsondb", "org.openhab.core.items.Item.json");
        Path metadataJsonDatabasePath = Path.of(baseDir, "jsondb", "org.openhab.core.items.Metadata.json");

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
                Metadata metadata = metadataStorage.get("stateDescription:" + itemName);
                if (metadata == null) {
                    LOGGER.info("{}: Nothing to do, no state description found.", itemName);
                } else {
                    String pattern = (String) metadata.getConfiguration().get("pattern");
                    if (pattern.contains(UnitUtils.UNIT_PLACEHOLDER)) {
                        LOGGER.info(
                                "{}: State description contains unit place-holder '%unit%', check if 'defaultUnit' metadata is needed!",
                                itemName);
                    } else {
                        Unit<?> stateDescriptionUnit = UnitUtils.parseUnit(pattern);
                        if (stateDescriptionUnit != null) {
                            String defaultUnit = stateDescriptionUnit.toString();
                            MetadataKey defaultUnitMetadataKey = new MetadataKey("defaultUnit", itemName);
                            Metadata defaultUnitMetadata = new Metadata(defaultUnitMetadataKey, defaultUnit, null);
                            metadataStorage.put(defaultUnitMetadataKey.toString(), defaultUnitMetadata);
                            LOGGER.info("{}: Wrote 'defaultUnit={}' to metadata.", itemName, defaultUnit);
                        }
                    }
                }
            }
        });

        metadataStorage.flush();
    }

    public static void main(String[] args) {
        Options options = getOptions();

        try {
            CommandLine commandLine = new DefaultParser().parse(options, args);
            String baseDir = commandLine.hasOption(CMD_OPT_DIR) ? commandLine.getOptionValue(CMD_OPT_DIR) : "";
            if (commandLine.hasOption(CMD_OPT_ITEM)) {
                String operation = commandLine.getOptionValue(CMD_OPT_ITEM);
                switch (operation) {
                    case "copyDefaultUnit":
                        itemCopyDefaultUnit(baseDir);
                        break;
                    default:
                        System.out.println("Available tasks for operation 'item': copyDefaultUnit");
                }
            }
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("upgradetool", options);
        }
    }
}
