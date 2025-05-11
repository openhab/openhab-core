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
package org.openhab.core.tools;

import java.io.PrintStream;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.storage.json.internal.JsonStorage;
import org.openhab.core.tools.internal.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link UpgradeTool} is a tool for upgrading openHAB to mitigate breaking changes
 *
 * @author Jan N. Klug - Initial contribution
 * @author Jimmy Tanagra - Refactor upgraders into individual classes
 */
@NonNullByDefault
public class UpgradeTool {
    private static final Set<String> LOG_LEVELS = Set.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR");
    private static final String OPT_COMMAND = "command";
    private static final String OPT_LIST_COMMANDS = "list-commands";
    private static final String OPT_USERDATA_DIR = "userdata";
    private static final String OPT_CONF_DIR = "conf";
    private static final String OPT_LOG = "log";
    private static final String OPT_FORCE = "force";

    private static final List<Upgrader> UPGRADERS = List.of( //
            new ItemUnitToMetadataUpgrader(), //
            new JSProfileUpgrader(), //
            new ScriptProfileUpgrader(), //
            new YamlConfigurationV1TagsUpgrader() // Added in 5.0
    );

    private static final Logger logger = LoggerFactory.getLogger(UpgradeTool.class);
    private static @Nullable JsonStorage<UpgradeRecord> upgradeRecords = null;

    private static Options getOptions() {
        Options options = new Options();

        options.addOption(Option.builder().longOpt(OPT_USERDATA_DIR).desc("USERDATA directory to process")
                .numberOfArgs(1).build());
        options.addOption(
                Option.builder().longOpt(OPT_CONF_DIR).desc("CONF directory to process").numberOfArgs(1).build());
        options.addOption(Option.builder().longOpt(OPT_COMMAND).numberOfArgs(1)
                .desc("command to execute (executes all if omitted)").build());
        options.addOption(Option.builder().longOpt(OPT_LIST_COMMANDS).desc("list available commands").build());
        options.addOption(Option.builder().longOpt(OPT_LOG).numberOfArgs(1).desc("log verbosity").build());
        options.addOption(Option.builder().longOpt(OPT_FORCE).desc("force execution (even if already done)").build());

        return options;
    }

    public static void main(String[] args) {
        Options options = getOptions();
        try {
            CommandLine commandLine = new DefaultParser().parse(options, args);

            String loglevel = commandLine.hasOption(OPT_LOG) ? commandLine.getOptionValue(OPT_LOG).toUpperCase()
                    : "INFO";
            if (!LOG_LEVELS.contains(loglevel)) {
                println("Allowed log-levels are " + LOG_LEVELS);
                System.exit(0);
            }

            if (commandLine.hasOption(OPT_LIST_COMMANDS)) {
                println("Available commands:");
                UPGRADERS.stream().forEach(upgrader -> {
                    println(" - " + upgrader.getName() + ": " + upgrader.getDescription());
                });
                System.exit(0);
            }

            System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, loglevel);

            boolean force = commandLine.hasOption(OPT_FORCE);
            String command = commandLine.hasOption(OPT_COMMAND) ? commandLine.getOptionValue(OPT_COMMAND) : null;

            if (command != null && UPGRADERS.stream().filter(u -> u.getName().equals(command)).findAny().isEmpty()) {
                println("Unknown command: " + command);
                System.exit(0);
            }

            String userdataDir = commandLine.hasOption(OPT_USERDATA_DIR) ? commandLine.getOptionValue(OPT_USERDATA_DIR)
                    : System.getenv("OPENHAB_USERDATA");
            if (command == null && (userdataDir == null || userdataDir.isBlank())) {
                println("Please either set the environment variable ${OPENHAB_USERDATA} or provide a directory through the --userdata option.");
                System.exit(0);
                return;
            } else if (userdataDir != null) {
                logger.info("Using userdataDir: {}", userdataDir);
            }

            String confDir = commandLine.hasOption(OPT_CONF_DIR) ? commandLine.getOptionValue(OPT_CONF_DIR)
                    : System.getenv("OPENHAB_CONF");
            if (confDir == null || confDir.isBlank()) {
                println("Please either set the environment variable ${OPENHAB_CONF} or provide a directory through the --conf option.");
                System.exit(0);
                return;
            } else {
                logger.info("Using confDir: {}", confDir);
            }

            Path upgradeJsonDatabasePath = Path.of(userdataDir, "jsondb", "org.openhab.core.tools.UpgradeTool");
            upgradeRecords = new JsonStorage<>(upgradeJsonDatabasePath.toFile(), null, 5, 0, 0, List.of());

            UPGRADERS.forEach(upgrader -> {
                String upgraderName = upgrader.getName();
                if (command != null && !upgraderName.equals(command)) {
                    return;
                }
                if (!force && lastExecuted(upgraderName) instanceof String executionDate) {
                    logger.info("Already executed '{}' on {}. Use '--force' to execute it again.", upgraderName,
                            executionDate);
                    return;
                }
                try {
                    logger.info("Executing {}: {}", upgraderName, upgrader.getDescription());
                    if (upgrader.execute(userdataDir, confDir)) {
                        if (userdataDir != null) {
                            updateUpgradeRecord(upgraderName);
                        } else {
                            logger.warn(
                                    "The Upgrade record for '{}' is not updated because user data directory wasn't specified",
                                    upgraderName);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error executing upgrader {}: {}", upgraderName, e.getMessage());
                }
            });
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("upgradetool", "", options, "", true);
        }

        System.exit(0);
    }

    private static @Nullable String lastExecuted(String upgrader) {
        JsonStorage<UpgradeRecord> records = upgradeRecords;
        if (records != null) {
            UpgradeRecord upgradeRecord = records.get(upgrader);
            if (upgradeRecord != null) {
                return upgradeRecord.executionDate;
            }
        } else {
            logger.error("Upgrade records storage is not initialized.");
        }
        return null;
    }

    private static void updateUpgradeRecord(String upgrader) {
        JsonStorage<UpgradeRecord> records = upgradeRecords;
        if (records != null) {
            records.put(upgrader, new UpgradeRecord(ZonedDateTime.now()));
            records.flush();
        } else {
            logger.error("Upgrade records storage is not initialized.");
        }
    }

    // to avoid compiler's null pointer warnings
    private static void println(String message) {
        PrintStream out = System.out;
        if (out != null) {
            out.println(message);
        }
    }

    private static class UpgradeRecord {
        public final String executionDate;

        public UpgradeRecord(ZonedDateTime executionDate) {
            this.executionDate = executionDate.toString();
        }
    }
}
