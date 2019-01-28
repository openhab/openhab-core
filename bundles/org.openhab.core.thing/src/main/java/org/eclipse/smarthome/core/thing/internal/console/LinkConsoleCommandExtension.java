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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.link.ItemChannelLink;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.io.console.Console;
import org.eclipse.smarthome.io.console.extensions.AbstractConsoleCommandExtension;
import org.eclipse.smarthome.io.console.extensions.ConsoleCommandExtension;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * {@link LinkConsoleCommandExtension} provides console commands for listing,
 * addding and removing links.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Alex Tugarev - Added support for links between items and things
 * @author Kai Kreuzer - Removed Thing link commands
 */
@Component(immediate = true, service = ConsoleCommandExtension.class)
public class LinkConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private static final String SUBCMD_LIST = "list";
    private static final String SUBCMD_CL_ADD = "addChannelLink";
    private static final String SUBCMD_CL_REMOVE = "removeChannelLink";
    private static final String SUBCMD_CLEAR = "clear";

    private ItemChannelLinkRegistry itemChannelLinkRegistry;

    public LinkConsoleCommandExtension() {
        super("links", "Manage your links.");
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length > 0) {
            String subCommand = args[0];
            switch (subCommand) {
                case SUBCMD_LIST:
                    list(console, itemChannelLinkRegistry.getAll());
                    return;
                case SUBCMD_CL_ADD:
                    if (args.length > 2) {
                        String itemName = args[1];
                        ChannelUID channelUID = new ChannelUID(args[2]);
                        addChannelLink(console, itemName, channelUID);
                    } else {
                        console.println("Specify item name and channel UID to link: link <itemName> <channelUID>");
                    }
                    return;
                case SUBCMD_CL_REMOVE:
                    if (args.length > 2) {
                        String itemName = args[1];
                        ChannelUID channelUID = new ChannelUID(args[2]);
                        removeChannelLink(console, itemName, channelUID);
                    } else {
                        console.println("Specify item name and channel UID to unlink: link <itemName> <channelUID>");
                    }
                    return;
                case SUBCMD_CLEAR:
                    clear(console);
                    return;
                default:
                    console.println("Unknown command '" + subCommand + "'");
                    printUsage(console);
                    break;
            }
        } else {
            printUsage(console);
        }
    }

    @Override
    public List<String> getUsages() {
        return Arrays.asList(new String[] { buildCommandUsage(SUBCMD_LIST, "lists all links"),
                buildCommandUsage(SUBCMD_CL_ADD + " <itemName> <channelUID>", "links an item with a channel"),
                buildCommandUsage(SUBCMD_CL_REMOVE + " <itemName> <thingUID>", "unlinks an item with a channel"),
                buildCommandUsage(SUBCMD_CLEAR, "removes all managed links") });
    }

    private void clear(Console console) {
        Collection<ItemChannelLink> itemChannelLinks = itemChannelLinkRegistry.getAll();
        for (ItemChannelLink itemChannelLink : itemChannelLinks) {
            itemChannelLinkRegistry.remove(itemChannelLink.getUID());
        }
        console.println(itemChannelLinks.size() + " links successfully removed.");
    }

    private void addChannelLink(Console console, String itemName, ChannelUID channelUID) {
        ItemChannelLink itemChannelLink = new ItemChannelLink(itemName, channelUID);
        itemChannelLinkRegistry.add(itemChannelLink);
        console.println("Link " + itemChannelLink.toString() + " successfully added.");
    }

    private void list(Console console, Collection<ItemChannelLink> itemChannelLinks) {
        for (ItemChannelLink itemChannelLink : itemChannelLinks) {
            console.println(itemChannelLink.toString());
        }
    }

    private void removeChannelLink(Console console, String itemName, ChannelUID channelUID) {
        ItemChannelLink itemChannelLink = new ItemChannelLink(itemName, channelUID);
        ItemChannelLink removedItemChannelLink = itemChannelLinkRegistry.remove(itemChannelLink.getUID());
        if (removedItemChannelLink != null) {
            console.println("Link " + itemChannelLink.toString() + "successfully removed.");
        } else {
            console.println("Could not remove link " + itemChannelLink.toString() + ".");
        }
    }

    @Reference
    protected void setItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
    }

    protected void unsetItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = null;
    }

}
