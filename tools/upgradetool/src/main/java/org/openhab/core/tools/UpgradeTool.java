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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
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

    public static void main(String[] args) {
        Options options = getOptions();
        try {
            CommandLine commandLine = new DefaultParser().parse(options, args);
            System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");

            String baseDir = commandLine.hasOption(CMD_OPT_DIR) ? commandLine.getOptionValue(CMD_OPT_DIR) : "";
            Upgrader upgrader = new Upgrader();
            if (commandLine.hasOption(CMD_OPT_ITEM)) {
                String operation = commandLine.getOptionValue(CMD_OPT_ITEM);
                switch (operation) {
                    case "copyUnitToMetadata":
                        upgrader.itemCopyUnitToMetadata(baseDir);
                        break;
                    default:
                        System.out.println("Available tasks for operation 'item': copyUnitToMetadata");
                }
            } else if (commandLine.hasOption(CMD_OPT_LINK)) {
                String operation = commandLine.getOptionValue(CMD_OPT_LINK);
                switch (operation) {
                    case "upgradeJsProfile":
                        upgrader.upgradeJsProfile(baseDir);
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
