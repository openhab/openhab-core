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
package org.openhab.core.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.storage.json.internal.JsonStorage;
import org.openhab.core.tools.internal.HomeAssistantAddonUpgrader;
import org.openhab.core.tools.internal.HomieAddonUpgrader;
import org.openhab.core.tools.internal.ItemUnitToMetadataUpgrader;
import org.openhab.core.tools.internal.JSProfileUpgrader;
import org.openhab.core.tools.internal.PersistenceUpgrader;
import org.openhab.core.tools.internal.ScriptProfileUpgrader;
import org.openhab.core.tools.internal.SemanticTagUpgrader;
import org.openhab.core.tools.internal.YamlConfigurationV1TagsUpgrader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link UpgradeTool} is a tool for upgrading openHAB to mitigate breaking changes
 *
 * @author Jan N. Klug - Initial contribution
 * @author Jimmy Tanagra - Refactor upgraders into individual classes
 * @author Mark Herwege - Added persistence strategy upgrader
 * @author Mark Herwege - Added semantic tag upgrader
 * @author Mark Herwege - Track OH versions and let upgraders set a target version
 */
@NonNullByDefault
public class UpgradeTool {
    private static final Set<String> LOG_LEVELS = Set.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR");
    private static final String OPT_COMMAND = "command";
    private static final String OPT_LIST_COMMANDS = "list-commands";
    private static final String OPT_USERDATA_DIR = "userdata";
    private static final String OPT_CONF_DIR = "conf";
    private static final String OPT_OH_VERSION = "version";
    private static final String OPT_LOG = "log";
    private static final String OPT_FORCE = "force";

    private static final String ENV_USERDATA = "OPENHAB_USERDATA";
    private static final String ENV_CONF = "OPENHAB_CONF";

    private static final String UPGRADE_TOOL_VERSION_KEY = "UpgradeTool";

    private static final Pattern BUILD_VERSION_PATTERN = Pattern.compile("^build-no\\s*:\\s*(\\S.*\\S)\\s*$");
    private static final Pattern DISTRO_VERSION_PATTERN = Pattern.compile("^openhab-distro\\s*:\\s*(\\S+)\\s*$");
    private static final Pattern CORE_VERSION_PATTERN = Pattern.compile("^openhab-core\\s*:\\s*(\\S+)\\s*$");
    private static final Pattern ADDONS_VERSION_PATTERN = Pattern.compile("^openhab-addons\\s*:\\s*(\\S+)\\s*$");
    private static final Pattern KARAF_VERSION_PATTERN = Pattern.compile("^karaf\\s*:\\s*(\\S+)\\s*$");

    private static final List<Upgrader> UPGRADERS = List.of( //
            new ItemUnitToMetadataUpgrader(), // Since 4.0.0
            new JSProfileUpgrader(), // Since 4.0.0
            new ScriptProfileUpgrader(), // Since 4.2.0
            new YamlConfigurationV1TagsUpgrader(), // Since 5.0
            new HomeAssistantAddonUpgrader(), // Since 5.1
            new HomieAddonUpgrader(), // Since 5.1
            new PersistenceUpgrader(), // Since 5.1
            new SemanticTagUpgrader() // Since 5.2
    );

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeTool.class);
    private static @Nullable JsonStorage<UpgradeRecord> upgradeRecords = null;
    private static @Nullable JsonStorage<VersionRecord> ohVersionRecords = null;

    private static VersionRecord ohTargetVersion = new VersionRecord();

    private static Options getOptions() {
        Options options = new Options();

        options.addOption(Option.builder().longOpt(OPT_USERDATA_DIR).desc(
                "USERDATA directory to process. Enclose it in double quotes to ensure that any backslashes are not ignored by your command shell.")
                .numberOfArgs(1).get());
        options.addOption(Option.builder().longOpt(OPT_CONF_DIR).desc(
                "CONF directory to process. Enclose it in double quotes to ensure that any backslashes are not ignored by your command shell.")
                .numberOfArgs(1).get());
        options.addOption(Option.builder().longOpt(OPT_OH_VERSION).desc(
                "openHAB target version. Upgraders will be executed again if they have been executed in an upgrade to an earlier openHAB version and there are new changes.")
                .numberOfArgs(1).get());
        options.addOption(Option.builder().longOpt(OPT_COMMAND).numberOfArgs(1)
                .desc("command to execute (executes all if omitted)").get());
        options.addOption(Option.builder().longOpt(OPT_LIST_COMMANDS).desc("list available commands").get());
        options.addOption(Option.builder().longOpt(OPT_LOG).numberOfArgs(1).desc("log verbosity").get());
        options.addOption(Option.builder().longOpt(OPT_FORCE).desc("force execution (even if already done)").get());

        return options;
    }

    public static void main(String[] args) throws IOException {
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

            Path userdataPath = getPath("userdata", commandLine, OPT_USERDATA_DIR, ENV_USERDATA);
            Path confPath = getPath("conf", commandLine, OPT_CONF_DIR, ENV_CONF);

            if (userdataPath != null) {
                Path upgradeJsonDatabasePath = userdataPath
                        .resolve(Path.of("jsondb", "org.openhab.core.tools.UpgradeTool"));
                upgradeRecords = new JsonStorage<>(upgradeJsonDatabasePath.toFile(), null, 5, 0, 0, List.of());
            } else {
                LOGGER.warn("Upgrade records storage is not initialized.");
            }

            if (userdataPath != null) {
                Path ohVersionJsonDatabasePath = userdataPath
                        .resolve(Path.of("jsondb", "org.openhab.core.tools.ohVersion"));
                ohVersionRecords = new JsonStorage<>(ohVersionJsonDatabasePath.toFile(), null, 5, 0, 0, List.of());
            } else {
                LOGGER.warn("OH version storage is not initialized.");
            }
            ohTargetVersion = commandLine.hasOption(OPT_OH_VERSION)
                    ? new VersionRecord(commandLine.getOptionValue(OPT_OH_VERSION))
                    : getTargetVersion(userdataPath);

            UPGRADERS.forEach(upgrader -> {
                String upgraderName = upgrader.getName();
                if (command != null && !upgraderName.equals(command)) {
                    return;
                }
                if (!force) {
                    if (upgrader.getTargetVersion() instanceof String targetVersion) {
                        if (lastExecutedVersion(upgraderName) instanceof String executionVersion) {
                            if (!isBeforeVersion(executionVersion, targetVersion)) {
                                LOGGER.info("Already executed '{}' to version {}. Use '--force' to execute it again.",
                                        upgraderName, executionVersion);
                                return;
                            }
                        } else if (lastExecuted(upgraderName) instanceof String executionDate) {
                            LOGGER.info("Already executed '{}' on {}. Use '--force' to execute it again.", upgraderName,
                                    executionDate);
                            return;
                        }
                    } else if (lastExecuted(upgraderName) instanceof String executionDate) {
                        LOGGER.info("Already executed '{}' on {}. Use '--force' to execute it again.", upgraderName,
                                executionDate);
                        return;
                    }
                }
                try {
                    LOGGER.info("Executing {}: {}", upgraderName, upgrader.getDescription());
                    if (upgrader.execute(userdataPath, confPath)) {
                        updateUpgradeRecord(upgraderName);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error executing upgrader {}: {}", upgraderName, e.getMessage());
                }
            });

            // Only save version record if all upgraders have been executed
            if (command == null) {
                updateVersionRecord();
            }

        } catch (ParseException e) {
            HelpFormatter formatter = HelpFormatter.builder().get();
            formatter.printHelp("upgradetool", "", options, "", true);
        }

        System.exit(0);
    }

    /**
     * Returns the path to the given directory, either from the command line or from the environment variable.
     * If neither is set, it defaults to a relative subdirectory of the given pathName ('./userdata' or './conf').
     *
     * @param pathName the name of the directory (e.g., "userdata" or "conf").
     * @param commandLine a CommandLine instance.
     * @param option the command line option for the directory (e.g., "userdata" or "conf").
     * @param env the environment variable name for the directory (e.g., "OPENHAB_USERDATA" or "OPENHAB_CONF").
     * @return the absolute path to the directory, or null if it does not exist.
     */
    private static @Nullable Path getPath(String pathName, CommandLine commandLine, String option, String env) {
        Path path = Path.of(pathName);

        String optionValue = commandLine.getOptionValue(option);
        String envValue = System.getenv(env);

        if (optionValue != null && !optionValue.isBlank()) {
            path = Path.of(optionValue);
        } else if (envValue != null && !envValue.isBlank()) {
            path = Path.of(envValue);
        }

        path = path.toAbsolutePath();

        if (path.toFile().isDirectory()) {
            return path;
        } else {
            LOGGER.warn(
                    "The '{}' directory '{}' does not exist. Some tasks may fail. To set it, either set the environment variable ${{}} or provide a directory through the --{} option.",
                    pathName, path, env, option);
            return null;
        }
    }

    private static VersionRecord getTargetVersion(@Nullable Path userdataPath) {
        if (userdataPath != null) {
            Path versionFilePath = userdataPath.resolve(Path.of("etc", "version.properties"));
            try (BufferedReader reader = Files.newBufferedReader(versionFilePath, StandardCharsets.UTF_8)) {
                String build = null;
                String distro = null;
                String core = null;
                String addons = null;
                String karaf = null;

                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = BUILD_VERSION_PATTERN.matcher(line.trim());
                    if (matcher.matches()) {
                        build = matcher.group(1);
                        continue;
                    }
                    matcher = DISTRO_VERSION_PATTERN.matcher(line.trim());
                    if (matcher.matches()) {
                        distro = matcher.group(1);
                        continue;
                    }
                    matcher = CORE_VERSION_PATTERN.matcher(line.trim());
                    if (matcher.matches()) {
                        core = matcher.group(1);
                        continue;
                    }
                    matcher = ADDONS_VERSION_PATTERN.matcher(line.trim());
                    if (matcher.matches()) {
                        addons = matcher.group(1);
                        continue;
                    }
                    matcher = KARAF_VERSION_PATTERN.matcher(line.trim());
                    if (matcher.matches()) {
                        karaf = matcher.group(1);
                        continue;
                    }
                }
                return new VersionRecord(build, distro, core, addons, karaf);
            } catch (IOException | SecurityException e) {
                LOGGER.warn(
                        "Cannot retrieve OH core version from '{}' file. Some tasks may fail. You can provide the target version through the --version option.",
                        versionFilePath);
            }
        }
        return new VersionRecord();
    }

    private static boolean isBeforeVersion(String versionA, String versionB) {
        String version1 = versionA.replaceFirst("[-M].*", "");
        String version2 = versionB.replaceFirst("[-M].*", "");
        return version1.compareTo(version2) < 0;
    }

    private static @Nullable String lastExecuted(String upgrader) {
        if (upgradeRecords != null) {
            UpgradeRecord upgradeRecord = upgradeRecords.get(upgrader);
            if (upgradeRecord != null) {
                return upgradeRecord.executionDate();
            }
        }
        return null;
    }

    private static @Nullable String lastExecutedVersion(String upgrader) {
        if (upgradeRecords != null) {
            UpgradeRecord upgradeRecord = upgradeRecords.get(upgrader);
            if (upgradeRecord != null) {
                return upgradeRecord.executionVersion();
            }
        }
        if (ohVersionRecords != null) {
            VersionRecord versionRecord = ohVersionRecords.get(UPGRADE_TOOL_VERSION_KEY);
            if (versionRecord != null) {
                return versionRecord.core();
            }
        }
        return null;
    }

    private static void updateUpgradeRecord(String upgrader) {
        JsonStorage<UpgradeRecord> records = upgradeRecords;
        if (records != null && ohTargetVersion.isDefined()) {
            records.put(upgrader, new UpgradeRecord(ZonedDateTime.now(), ohTargetVersion.distro));
            records.flush();
        }
    }

    private static void updateVersionRecord() {
        JsonStorage<VersionRecord> records = ohVersionRecords;
        if (records != null && ohTargetVersion.isDefined()) {
            records.put(UPGRADE_TOOL_VERSION_KEY, ohTargetVersion);
            records.flush();
        }
    }

    // to avoid compiler's null pointer warnings
    private static void println(String message) {
        PrintStream out = System.out;
        if (out != null) {
            out.println(message);
        }
    }

    private record UpgradeRecord(String executionDate, @Nullable String executionVersion) {
        public UpgradeRecord(ZonedDateTime executionDate, @Nullable String executionVersion) {
            this(executionDate.toString(), executionVersion);
        }
    }

    private record VersionRecord(String executionDate, @Nullable String build, @Nullable String distro,
            @Nullable String core, @Nullable String addons, @Nullable String karaf) {

        protected VersionRecord() {
            this(null);
        }

        protected VersionRecord(@Nullable String core) {
            this(null, null, core, null, null);
        }

        protected VersionRecord(@Nullable String build, @Nullable String distro, @Nullable String core,
                @Nullable String addons, @Nullable String karaf) {
            this(ZonedDateTime.now(), build, distro, core, addons, karaf);
        }

        protected VersionRecord(ZonedDateTime executionDate, @Nullable String build, @Nullable String distro,
                @Nullable String core, @Nullable String addons, @Nullable String karaf) {
            this(executionDate.toString(), build, distro, core, addons, karaf);
        }

        protected boolean isDefined() {
            return distro != null;
        }
    };
}
