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

import static org.openhab.core.tools.internal.Upgrader.*;

import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.tools.internal.Upgrader;

/**
 * The {@link UpgradeTool} is a tool for upgrading openHAB to mitigate breaking changes
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class UpgradeTool {
    private static final Set<String> LOG_LEVELS = Set.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR");
    private static final String OPT_COMMAND = "command";
    private static final String OPT_DIR = "dir";
    private static final String OPT_LOG = "log";
    private static final String OPT_FORCE = "force";

    private static Options getOptions() {
        Options options = new Options();

        options.addOption(Option.builder().longOpt(OPT_DIR).desc("directory to process").numberOfArgs(1).build());
        options.addOption(Option.builder().longOpt(OPT_COMMAND).numberOfArgs(1).desc("command to execute").build());
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
                System.out.println("Allowed log-levels are " + LOG_LEVELS);
                System.exit(0);
            }

            System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, loglevel);

            String baseDir = commandLine.hasOption(OPT_DIR) ? commandLine.getOptionValue(OPT_DIR)
                    : System.getenv("OPENHAB_USERDATA");
            if (baseDir == null || baseDir.isBlank()) {
                System.out.println(
                        "Please either set the environment variable ${OPENHAB_USERDATA} or provide a directory through the --dir option.");
                System.exit(0);
            } else {
                boolean force = commandLine.hasOption(OPT_FORCE) ? true : false;

                Upgrader upgrader = new Upgrader(baseDir, force);
                if (commandLine.hasOption(ITEM_COPY_UNIT_TO_METADATA)) {
                    upgrader.itemCopyUnitToMetadata();
                } else if (commandLine.hasOption(LINK_UPGRADE_JS_PROFILE)) {
                    upgrader.linkUpgradeJsProfile();
                }
            }
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            String commands = Set.of(ITEM_COPY_UNIT_TO_METADATA, LINK_UPGRADE_JS_PROFILE).toString();
            formatter.printHelp("upgradetool", "", options, "Available commands: " + commands, true);
        }

        System.exit(0);
    }
}
