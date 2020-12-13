/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.thing.internal.console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingManager;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.events.ThingEventFactory;
import org.openhab.core.thing.i18n.ThingStatusInfoI18nLocalizationService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * {@link ThingConsoleCommandExtension} provides console commands for listing and removing things.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Thomas Höfer - Added localization of thing status
 * @author Stefan Triller - Added trigger channel command
 * @author Henning Sudbrock - Added show command
 */
@Component(immediate = true, service = ConsoleCommandExtension.class)
@NonNullByDefault
public class ThingConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private static final String CMD_THINGS = "things";
    private static final String SUBCMD_LIST = "list";
    private static final String SUBCMD_SHOW = "show";
    private static final String SUBCMD_CLEAR = "clear";
    private static final String SUBCMD_REMOVE = "remove";
    private static final String SUBCMD_TRIGGER = "trigger";
    private static final String SUBCMD_DISABLE = "disable";
    private static final String SUBCMD_ENABLE = "enable";

    private final ManagedThingProvider managedThingProvider;
    private final ThingRegistry thingRegistry;
    private final ThingStatusInfoI18nLocalizationService thingStatusInfoI18nLocalizationService;
    private final EventPublisher eventPublisher;
    private final ThingManager thingManager;

    @Activate
    public ThingConsoleCommandExtension(final @Reference ManagedThingProvider managedThingProvider,
            final @Reference ThingRegistry thingRegistry,
            final @Reference ThingStatusInfoI18nLocalizationService thingStatusInfoI18nLocalizationService,
            final @Reference EventPublisher eventPublisher, final @Reference ThingManager thingManager) {
        super(CMD_THINGS, "Access your thing registry.");
        this.managedThingProvider = managedThingProvider;
        this.thingRegistry = thingRegistry;
        this.thingStatusInfoI18nLocalizationService = thingStatusInfoI18nLocalizationService;
        this.eventPublisher = eventPublisher;
        this.thingManager = thingManager;
    }

    @Override
    public void execute(String[] args, Console console) {
        Collection<Thing> things = thingRegistry.getAll();
        if (args.length > 0) {
            String subCommand = args[0];
            switch (subCommand) {
                case SUBCMD_LIST:
                    printThings(console, things);
                    return;
                case SUBCMD_SHOW:
                    printThingsDetails(console, Arrays.asList(args).subList(1, args.length));
                    return;
                case SUBCMD_CLEAR:
                    removeAllThings(console, things);
                    return;
                case SUBCMD_REMOVE:
                    if (args.length > 1) {
                        ThingUID thingUID = new ThingUID(args[1]);
                        removeThing(console, thingUID);
                    } else {
                        console.println("Specify thing id to remove: things remove <thingUID> (e.g. \"hue:light:1\")");
                    }
                    return;
                case SUBCMD_TRIGGER:
                    if (args.length == 3) {
                        triggerChannel(console, args[1], args[2]);
                    } else if (args.length == 2) {
                        triggerChannel(console, args[1], null);
                    } else {
                        console.println("Command '" + subCommand + "' needs arguments <channelUID> [<event>]");
                    }
                    break;
                case SUBCMD_DISABLE:
                case SUBCMD_ENABLE:
                    if (args.length > 1) {
                        ThingUID thingUID = new ThingUID(args[1]);
                        enableThing(console, thingUID, SUBCMD_ENABLE.equals(subCommand));
                    } else {
                        console.println(
                                "Command '" + subCommand + "' needs argument <thingUID> (e.g. \"hue:light:1\")");
                    }
                    return;
                default:
                    break;
            }
        } else {
            printUsage(console);
        }
    }

    private void triggerChannel(Console console, String channelUid, @Nullable String event) {
        eventPublisher.post(ThingEventFactory.createTriggerEvent(event, new ChannelUID(channelUid)));
    }

    private void removeThing(Console console, ThingUID thingUID) {
        Thing removedThing = this.managedThingProvider.remove(thingUID);
        if (removedThing != null) {
            console.println("Thing '" + thingUID + "' successfully removed.");
        } else {
            console.println("Could not delete thing " + thingUID + ".");
        }
    }

    private void removeAllThings(Console console, Collection<Thing> things) {
        int numberOfThings = things.size();
        for (Thing thing : things) {
            managedThingProvider.remove(thing.getUID());
        }
        console.println(numberOfThings + " things successfully removed.");
    }

    private void enableThing(Console console, ThingUID thingUID, boolean isEnabled) {
        if (thingRegistry.get(thingUID) == null) {
            console.println("unknown thing for thingUID '" + thingUID.getAsString() + "'.");
            return;
        }
        thingManager.setEnabled(thingUID, isEnabled);
        String command = isEnabled ? "enabled" : "disabled";
        console.println(thingUID.getAsString() + " successfully " + command + ".");
    }

    @Override
    public List<String> getUsages() {
        return List.of(buildCommandUsage(SUBCMD_LIST, "lists all things"),
                buildCommandUsage(SUBCMD_SHOW + " <thingUID>*",
                        "show details about one or more things; show details for all things if no thingUID provided"),
                buildCommandUsage(SUBCMD_CLEAR, "removes all managed things"),
                buildCommandUsage(SUBCMD_REMOVE + " <thingUID>", "removes a thing"),
                buildCommandUsage(SUBCMD_TRIGGER + " <channelUID> [<event>]",
                        "triggers the <channelUID> with <event> (if given)"),
                buildCommandUsage(SUBCMD_DISABLE + " <thingUID>", "disables a thing"),
                buildCommandUsage(SUBCMD_ENABLE + " <thingUID>", "enables a thing"));
    }

    private void printThings(Console console, Collection<Thing> things) {
        if (things.isEmpty()) {
            console.println("No things found.");
        }

        for (Thing thing : things) {
            String id = thing.getUID().toString();
            String thingType = thing instanceof Bridge ? "Bridge" : "Thing";
            ThingStatusInfo status = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing, null);
            ThingUID bridgeUID = thing.getBridgeUID();
            String label = thing.getLabel();

            console.println(String.format("%s (Type=%s, Status=%s, Label=%s, Bridge=%s)", id, thingType, status, label,
                    bridgeUID));
        }
    }

    private void printThingsDetails(Console console, List<String> thingUIDStrings) {
        Collection<Thing> things;

        if (thingUIDStrings.isEmpty()) {
            things = thingRegistry.getAll();
        } else {
            things = new ArrayList<>();
            for (String thingUIDString : thingUIDStrings) {
                ThingUID thingUID;
                try {
                    thingUID = new ThingUID(thingUIDString);
                } catch (IllegalArgumentException e) {
                    console.println("This is not a valid thing UID: " + thingUIDString);
                    return;
                }

                Thing thing = thingRegistry.get(thingUID);
                if (thing == null) {
                    console.println("Could not find thing with UID " + thingUID);
                    return;
                }
                things.add(thing);
            }
        }

        printThingsDetails(console, things);
    }

    private void printThingsDetails(Console console, Collection<Thing> things) {
        for (Iterator<Thing> iter = things.iterator(); iter.hasNext();) {
            printThingDetails(console, iter.next());
            if (iter.hasNext()) {
                console.println("");
                console.println("--- --- --- --- ---");
                console.println("");
            }
        }
    }

    private void printThingDetails(Console console, Thing thing) {
        console.println("UID: " + thing.getUID());
        console.println("Type: " + thing.getThingTypeUID());
        console.println("Label: " + thing.getLabel());
        console.println("Status: " + thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing, null));
        if (thing.getBridgeUID() != null) {
            console.println("Bridge: " + thing.getBridgeUID());
        }
        console.println("");

        if (thing.getProperties().isEmpty()) {
            console.println("No properties");
        } else {
            console.println("Properties:");
            for (Map.Entry<String, String> entry : thing.getProperties().entrySet()) {
                console.println("\t" + entry.getKey() + " : " + entry.getValue());
            }
        }
        console.println("");

        if (thing.getConfiguration().getProperties().isEmpty()) {
            console.println("No configuration parameters");
        } else {
            console.println("Configuration parameters:");
            for (Map.Entry<String, Object> entry : thing.getConfiguration().getProperties().entrySet()) {
                console.println("\t" + entry.getKey() + " : " + entry.getValue());
            }
        }
        console.println("");

        if (thing.getChannels().isEmpty()) {
            console.println("No channels");
        } else {
            console.println("Channels:");
            for (Iterator<Channel> iter = thing.getChannels().iterator(); iter.hasNext();) {
                Channel channel = iter.next();
                console.println("\tID: " + channel.getUID().getId());
                console.println("\tLabel: " + channel.getLabel());
                console.println("\tType: " + channel.getChannelTypeUID());
                if (channel.getDescription() != null) {
                    console.println("\tDescription: " + channel.getDescription());
                }
                if (iter.hasNext()) {
                    console.println("");
                }
            }
        }
    }
}
