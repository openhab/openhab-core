/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.io.console;

/**
 * This class provides generic methods for handling console input (i.e. pure strings).
 *
 * NOTE: This class is only kept for backward compatibility so that openHAB 1 Add-ons still compile.
 * It must not be used productively!
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class ConsoleInterpreter {

    /**
     * This method simply takes a list of arguments, where the first one is treated
     * as the console command (such as "update", "send" etc.). The following entries
     * are then the arguments for this command.
     * If the command is unknown, the complete usage is printed to the console.
     *
     * @param args array which contains the console command and all its arguments
     * @param console the console for printing messages for the user
     */
    public static void handleRequest(String[] args, Console console) {
    }

    /**
     * This method handles an update command.
     *
     * @param args array which contains the arguments for the update command
     * @param console the console for printing messages for the user
     */
    public static void handleUpdate(String[] args, Console console) {
    }

    /**
     * This method handles a send command.
     *
     * @param args array which contains the arguments for the send command
     * @param console the console for printing messages for the user
     */
    public static void handleSend(String[] args, Console console) {
    }

    /**
     * This method handles an items command.
     *
     * @param args array which contains the arguments for the items command
     * @param console the console for printing messages for the user
     */
    public static void handleItems(String[] args, Console console) {
    }

    /**
     * This method handles a status command.
     *
     * @param args array which contains the arguments for the status command
     * @param console the console for printing messages for the user
     */
    public static void handleStatus(String[] args, Console console) {
    }

    /**
     * This method handles a say command.
     *
     * @param args array which contains the arguments for the status command
     * @param console the console for printing messages for the user
     */
    public static void handleSay(String[] args, Console console) {
    }

    public static void handleScript(String[] args, Console console) {
    }

    /** returns a CR-separated list of usage texts for all available commands */
    private static String getUsage() {
        StringBuilder sb = new StringBuilder();
        for (String usage : ConsoleInterpreter.getUsages()) {
            sb.append(usage + "\n");
        }
        return sb.toString();
    }

    /** returns an array of the usage texts for all available commands */
    public static String[] getUsages() {
        return new String[] { getUpdateUsage(), getCommandUsage(), getStatusUsage(), getItemsUsage(), getSayUsage(),
                getScriptUsage() };
    }

    public static String getUpdateUsage() {
        return "update <item> <state> - sends a status update for an item";
    }

    public static String getCommandUsage() {
        return "send <item> <command> - sends a command for an item";
    }

    public static String getStatusUsage() {
        return "status <item> - shows the current status of an item";
    }

    public static String getItemsUsage() {
        return "items [<pattern>] - lists names and types of all items matching the pattern";
    }

    public static String getSayUsage() {
        return "say <sentence to say> - Says a message through TTS on the host machine";
    }

    public static String getScriptUsage() {
        return "> <script to execute> - Executes a script";
    }

}
