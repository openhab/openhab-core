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
package org.eclipse.smarthome.core.thing.internal.console;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ManagedThingProvider;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingManager;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.events.ThingEventFactory;
import org.eclipse.smarthome.core.thing.i18n.ThingStatusInfoI18nLocalizationService;
import org.eclipse.smarthome.io.console.Console;
import org.eclipse.smarthome.io.console.extensions.AbstractConsoleCommandExtension;
import org.eclipse.smarthome.io.console.extensions.ConsoleCommandExtension;
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
public class ThingConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private static final String CMD_THINGS = "things";
    private static final String SUBCMD_LIST = "list";
    private static final String SUBCMD_SHOW = "show";
    private static final String SUBCMD_CLEAR = "clear";
    private static final String SUBCMD_REMOVE = "remove";
    private static final String SUBCMD_TRIGGER = "trigger";
    private static final String SUBCMD_DISABLE = "disable";
    private static final String SUBCMD_ENABLE = "enable";

    private ManagedThingProvider managedThingProvider;
    private ThingRegistry thingRegistry;
    private ThingStatusInfoI18nLocalizationService thingStatusInfoI18nLocalizationService;
    private EventPublisher eventPublisher;
    private ThingManager thingManager;

    public ThingConsoleCommandExtension() {
        super(CMD_THINGS, "Access your thing registry.");
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
                    printThingsDetails(console, asList(args).subList(1, args.length));
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
                        enableThing(console, thingUID, subCommand.equals(SUBCMD_ENABLE));
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

    private void triggerChannel(Console console, String channelUid, String event) {
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
        return Arrays.asList(new String[] { buildCommandUsage(SUBCMD_LIST, "lists all things"),
                buildCommandUsage(SUBCMD_SHOW + " <thingUID>*",
                        "show details about one or more things; show details for all things if no thingUID provided"),
                buildCommandUsage(SUBCMD_CLEAR, "removes all managed things"),
                buildCommandUsage(SUBCMD_REMOVE + " <thingUID>", "removes a thing"),
                buildCommandUsage(SUBCMD_TRIGGER + " <channelUID> [<event>]",
                        "triggers the <channelUID> with <event> (if given)"),
                buildCommandUsage(SUBCMD_DISABLE + " <thingUID>", "disables a thing"),
                buildCommandUsage(SUBCMD_ENABLE + " <thingUID>", "enables a thing") });
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

    @Reference
    protected void setManagedThingProvider(ManagedThingProvider managedThingProvider) {
        this.managedThingProvider = managedThingProvider;
    }

    @Reference
    protected void setThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    protected void unsetManagedThingProvider(ManagedThingProvider managedThingProvider) {
        this.managedThingProvider = null;
    }

    protected void unsetThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = null;
    }

    @Reference
    protected void setThingStatusInfoI18nLocalizationService(
            ThingStatusInfoI18nLocalizationService thingStatusInfoI18nLocalizationService) {
        this.thingStatusInfoI18nLocalizationService = thingStatusInfoI18nLocalizationService;
    }

    protected void unsetThingStatusInfoI18nLocalizationService(
            ThingStatusInfoI18nLocalizationService thingStatusInfoI18nLocalizationService) {
        this.thingStatusInfoI18nLocalizationService = null;
    }

    @Reference
    protected void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }

    @Reference
    protected void setThingManager(ThingManager thingManager) {
        this.thingManager = thingManager;
    }

    protected void unsetThingManager(ThingManager thingManager) {
        this.thingManager = null;
    }

}
