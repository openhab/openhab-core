/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.config.discovery.internal.console;

import static org.eclipse.smarthome.config.discovery.inbox.InboxPredicates.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultFlag;
import org.eclipse.smarthome.config.discovery.inbox.Inbox;
import org.eclipse.smarthome.config.discovery.internal.PersistentInbox;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.io.console.Console;
import org.eclipse.smarthome.io.console.extensions.AbstractConsoleCommandExtension;
import org.eclipse.smarthome.io.console.extensions.ConsoleCommandExtension;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This class provides console commands around the inbox functionality.
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
@Component(immediate = true, service = ConsoleCommandExtension.class)
public class InboxConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private static final String SUBCMD_APPROVE = "approve";
    private static final String SUBCMD_IGNORE = "ignore";
    private static final String SUBCMD_LIST = "list";
    private static final String SUBCMD_LIST_IGNORED = "listignored";
    private static final String SUBCMD_CLEAR = "clear";
    private static final String SUBCMD_REMOVE = "remove";

    private Inbox inbox;

    public InboxConsoleCommandExtension() {
        super("inbox", "Manage your inbox.");
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length > 0) {
            final String subCommand = args[0];
            switch (subCommand) {
                case SUBCMD_APPROVE:
                    if (args.length > 2) {
                        String label = args[2];
                        try {
                            ThingUID thingUID = new ThingUID(args[1]);
                            List<DiscoveryResult> results = inbox.stream().filter(forThingUID(thingUID))
                                    .collect(Collectors.toList());
                            if (results.isEmpty()) {
                                console.println("No matching inbox entry could be found.");
                                return;
                            }
                            inbox.approve(thingUID, label);
                        } catch (IllegalArgumentException e) {
                            console.println(e.getMessage());
                        }
                    } else {
                        console.println("Specify thing id to approve: inbox approve <thingUID> <label>");
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
                    printInboxEntries(console,
                            inbox.stream().filter(withFlag((DiscoveryResultFlag.NEW))).collect(Collectors.toList()));
                    break;
                case SUBCMD_LIST_IGNORED:
                    printInboxEntries(console, inbox.stream().filter(withFlag((DiscoveryResultFlag.IGNORED)))
                            .collect(Collectors.toList()));
                    break;
                case SUBCMD_CLEAR:
                    clearInboxEntries(console, inbox.getAll());
                    break;
                case SUBCMD_REMOVE:
                    if (args.length > 1) {
                        boolean validParam = true;
                        try {
                            ThingUID thingUID = new ThingUID(args[1]);
                            List<DiscoveryResult> results = inbox.stream().filter(forThingUID(thingUID))
                                    .collect(Collectors.toList());
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
                                        .collect(Collectors.toList());
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
            String timestamp = new Date(discoveryResult.getTimestamp()).toString();
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
        return Arrays.asList(new String[] { buildCommandUsage(SUBCMD_LIST, "lists all current inbox entries"),
                buildCommandUsage(SUBCMD_LIST_IGNORED, "lists all ignored inbox entries"),
                buildCommandUsage(SUBCMD_APPROVE + " <thingUID> <label>", "creates a thing for an inbox entry"),
                buildCommandUsage(SUBCMD_CLEAR, "clears all current inbox entries"),
                buildCommandUsage(SUBCMD_REMOVE + " [<thingUID>|<thingTypeUID>]",
                        "remove the inbox entries of a given thing id or thing type"),
                buildCommandUsage(SUBCMD_IGNORE + " <thingUID>", "ignores an inbox entry permanently") });
    }

    @Reference
    protected void setInbox(Inbox inbox) {
        this.inbox = inbox;
    }

    protected void unsetInbox(Inbox inbox) {
        this.inbox = null;
    }

}
