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
package org.openhab.core.config.discovery.internal.console;

import static org.openhab.core.config.discovery.inbox.InboxPredicates.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultFlag;
import org.openhab.core.config.discovery.inbox.Inbox;
import org.openhab.core.config.discovery.internal.PersistentInbox;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.ConsoleCommandCompleter;
import org.openhab.core.io.console.StringsCompleter;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This class provides console commands around the inbox functionality.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Laurent Garnier - New optional parameter for command approve
 */
@Component(immediate = true, service = ConsoleCommandExtension.class)
@NonNullByDefault
public class InboxConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private static final String SUBCMD_APPROVE = "approve";
    private static final String SUBCMD_IGNORE = "ignore";
    private static final String SUBCMD_LIST = "list";
    private static final String SUBCMD_LIST_IGNORED = "listignored";
    private static final String SUBCMD_CLEAR = "clear";
    private static final String SUBCMD_REMOVE = "remove";

    private static final StringsCompleter SUBCMD_COMPLETER = new StringsCompleter(
            List.of(SUBCMD_APPROVE, SUBCMD_IGNORE, SUBCMD_LIST, SUBCMD_LIST_IGNORED, SUBCMD_CLEAR, SUBCMD_REMOVE),
            false);

    private final Inbox inbox;

    private class InboxConsoleCommandCompleter implements ConsoleCommandCompleter {
        @Override
        public boolean complete(String[] args, int cursorArgumentIndex, int cursorPosition, List<String> candidates) {
            if (cursorArgumentIndex <= 0) {
                return SUBCMD_COMPLETER.complete(args, cursorArgumentIndex, cursorPosition, candidates);
            } else if (cursorArgumentIndex == 1) {
                if (SUBCMD_IGNORE.equalsIgnoreCase(args[0]) || SUBCMD_APPROVE.equalsIgnoreCase(args[0])) {
                    return new StringsCompleter(getThingUIDs(), true).complete(args, cursorArgumentIndex,
                            cursorPosition, candidates);
                } else if (SUBCMD_REMOVE.equalsIgnoreCase(args[0])) {
                    return new StringsCompleter(getThingAndThingTypeUIDs(), true).complete(args, cursorArgumentIndex,
                            cursorPosition, candidates);
                }
            }
            return false;
        }

        private Collection<String> getThingUIDs() {
            return inbox.stream().map(r -> r.getThingUID().getAsString()).toList();
        }

        private Collection<String> getThingAndThingTypeUIDs() {
            return inbox.stream()
                    .flatMap(r -> Stream.of(r.getThingUID().getAsString(), r.getThingTypeUID().getAsString())).toList();
        }
    }

    @Activate
    public InboxConsoleCommandExtension(final @Reference Inbox inbox) {
        super("inbox", "Manage your inbox.");
        this.inbox = inbox;
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length > 0) {
            final String subCommand = args[0];
            switch (subCommand) {
                case SUBCMD_APPROVE:
                    if (args.length > 2) {
                        String label = args[2];
                        String newThingId = null;
                        if (args.length > 3) {
                            newThingId = args[3];
                        }
                        try {
                            ThingUID thingUID = new ThingUID(args[1]);
                            List<DiscoveryResult> results = inbox.stream().filter(forThingUID(thingUID)).toList();
                            if (results.isEmpty()) {
                                console.println("No matching inbox entry could be found.");
                                return;
                            }
                            inbox.approve(thingUID, label, newThingId);
                        } catch (IllegalArgumentException e) {
                            console.println(Objects.requireNonNullElse(e.getMessage(),
                                    String.format("An error occurred while approving '%s'", args[1])));
                        }
                    } else {
                        console.println("Specify thing id to approve: inbox approve <thingUID> <label> [<newThingID>]");
                    }
                    break;
                case SUBCMD_IGNORE:
                    if (args.length > 1) {
                        try {
                            ThingUID thingUID = new ThingUID(args[1]);
                            PersistentInbox persistentInbox = (PersistentInbox) inbox;
                            persistentInbox.setFlag(thingUID, DiscoveryResultFlag.IGNORED);
                        } catch (IllegalArgumentException e) {
                            console.println("'" + args[1] + "' is no valid thing UID.");
                        }
                    } else {
                        console.println("Cannot approve thing as managed thing provider is missing.");
                    }
                    break;
                case SUBCMD_LIST:
                    printInboxEntries(console, inbox.stream().filter(withFlag((DiscoveryResultFlag.NEW))).toList());
                    break;
                case SUBCMD_LIST_IGNORED:
                    printInboxEntries(console, inbox.stream().filter(withFlag((DiscoveryResultFlag.IGNORED))).toList());
                    break;
                case SUBCMD_CLEAR:
                    clearInboxEntries(console, inbox.getAll());
                    break;
                case SUBCMD_REMOVE:
                    if (args.length > 1) {
                        boolean validParam = true;
                        try {
                            ThingUID thingUID = new ThingUID(args[1]);
                            List<DiscoveryResult> results = inbox.stream().filter(forThingUID(thingUID)).toList();
                            if (results.isEmpty()) {
                                console.println("No matching inbox entry could be found.");
                            } else {
                                clearInboxEntries(console, results);
                            }
                        } catch (IllegalArgumentException e) {
                            validParam = false;
                        }
                        if (!validParam) {
                            try {
                                ThingTypeUID thingTypeUID = new ThingTypeUID(args[1]);
                                List<DiscoveryResult> results = inbox.stream().filter(forThingTypeUID(thingTypeUID))
                                        .toList();
                                if (results.isEmpty()) {
                                    console.println("No matching inbox entry could be found.");
                                } else {
                                    clearInboxEntries(console, results);
                                }
                            } catch (IllegalArgumentException e) {
                                console.println("'" + args[1] + "' is no valid thing UID or thing type.");
                            }
                        }
                    } else {
                        console.println(
                                "Specify thing id or thing type to remove: inbox remove [<thingUID>|<thingTypeUID>]");
                    }
                    break;
                default:
                    printUsage(console);
                    break;
            }
        } else {
            printUsage(console);
        }
    }

    private void printInboxEntries(Console console, List<DiscoveryResult> discoveryResults) {
        if (discoveryResults.isEmpty()) {
            console.println("No inbox entries found.");
        }

        for (DiscoveryResult discoveryResult : discoveryResults) {
            ThingTypeUID thingTypeUID = discoveryResult.getThingTypeUID();
            ThingUID thingUID = discoveryResult.getThingUID();
            String label = discoveryResult.getLabel();
            DiscoveryResultFlag flag = discoveryResult.getFlag();
            ThingUID bridgeId = discoveryResult.getBridgeUID();
            Map<String, Object> properties = discoveryResult.getProperties();
            String representationProperty = discoveryResult.getRepresentationProperty();
            String timestamp = discoveryResult.getCreationTime().toString();
            String timeToLive = discoveryResult.getTimeToLive() == DiscoveryResult.TTL_UNLIMITED ? "UNLIMITED"
                    : "" + discoveryResult.getTimeToLive();
            console.println(String.format(
                    "%s [%s]: %s [thingId=%s, bridgeId=%s, properties=%s, representationProperty=%s, timestamp=%s, timeToLive=%s]",
                    flag.name(), thingTypeUID, label, thingUID, bridgeId, properties, representationProperty, timestamp,
                    timeToLive));

        }
    }

    private void clearInboxEntries(Console console, List<DiscoveryResult> discoveryResults) {
        if (discoveryResults.isEmpty()) {
            console.println("No inbox entries found.");
        }

        for (DiscoveryResult discoveryResult : discoveryResults) {
            ThingTypeUID thingTypeUID = discoveryResult.getThingTypeUID();
            ThingUID thingUID = discoveryResult.getThingUID();
            String label = discoveryResult.getLabel();
            DiscoveryResultFlag flag = discoveryResult.getFlag();
            ThingUID bridgeId = discoveryResult.getBridgeUID();
            Map<String, Object> properties = discoveryResult.getProperties();
            console.println(String.format("REMOVED [%s]: %s [label=%s, thingId=%s, bridgeId=%s, properties=%s]",
                    flag.name(), thingTypeUID, label, thingUID, bridgeId, properties));
            inbox.remove(thingUID);
        }
    }

    @Override
    public List<String> getUsages() {
        return List.of(buildCommandUsage(SUBCMD_LIST, "lists all current inbox entries"),
                buildCommandUsage(SUBCMD_LIST_IGNORED, "lists all ignored inbox entries"),
                buildCommandUsage(SUBCMD_APPROVE + " <thingUID> <label> [<newThingID>]",
                        "creates a thing for an inbox entry"),
                buildCommandUsage(SUBCMD_CLEAR, "clears all current inbox entries"),
                buildCommandUsage(SUBCMD_REMOVE + " [<thingUID>|<thingTypeUID>]",
                        "remove the inbox entries of a given thing id or thing type"),
                buildCommandUsage(SUBCMD_IGNORE + " <thingUID>", "ignores an inbox entry permanently"));
    }

    @Override
    public @Nullable ConsoleCommandCompleter getCompleter() {
        return new InboxConsoleCommandCompleter();
    }
}
