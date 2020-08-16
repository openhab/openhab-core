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
package org.openhab.core.io.console.internal.extension;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ManagedItemProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Console command extension to get item list
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Create DS for command extension
 * @author Dennis Nobel - Changed service references to be injected via DS
 * @author Simon Kaufmann - Added commands to clear and remove items
 * @author Stefan Triller - Added commands for adding and removing tags
 */
@Component(service = ConsoleCommandExtension.class)
@NonNullByDefault
public class ItemConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private static final String SUBCMD_LIST = "list";
    private static final String SUBCMD_CLEAR = "clear";
    private static final String SUBCMD_REMOVE = "remove";
    private static final String SUBCMD_ADDTAG = "addTag";
    private static final String SUBCMD_RMTAG = "rmTag";

    private final ItemRegistry itemRegistry;
    private final ManagedItemProvider managedItemProvider;

    @Activate
    public ItemConsoleCommandExtension(final @Reference ItemRegistry itemRegistry,
            final @Reference ManagedItemProvider managedItemProvider) {
        super("items", "Access the item registry.");
        this.itemRegistry = itemRegistry;
        this.managedItemProvider = managedItemProvider;
    }

    @Override
    public List<String> getUsages() {
        return Arrays.asList(new String[] {
                buildCommandUsage(SUBCMD_LIST + " [<pattern>]",
                        "lists names and types of all items (matching the pattern, if given)"),
                buildCommandUsage(SUBCMD_CLEAR, "removes all items"),
                buildCommandUsage(SUBCMD_REMOVE + " <itemName>", "removes the given item"),
                buildCommandUsage(SUBCMD_ADDTAG + " <itemName> <tag>", "adds a tag to the given item"),
                buildCommandUsage(SUBCMD_RMTAG + " <itemName> <tag>", "removes a tag from the given item") });
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length > 0) {
            String subCommand = args[0];
            switch (subCommand) {
                case SUBCMD_LIST:
                    listItems(console, (args.length < 2) ? "*" : args[1]);
                    break;
                case SUBCMD_CLEAR:
                    removeItems(console, itemRegistry.getAll());
                    break;
                case SUBCMD_REMOVE:
                    if (args.length > 1) {
                        Item item = itemRegistry.get(args[1]);
                        if (item != null) {
                            removeItems(console, Set.of(item));
                        } else {
                            console.println("0 item(s) removed.");
                        }
                    } else {
                        console.println("Specify the name of the item to remove: " + getCommand() + " " + SUBCMD_REMOVE
                                + " <itemName>");
                    }
                    break;
                case SUBCMD_ADDTAG:
                    if (args.length > 2) {
                        Item item = itemRegistry.get(args[1]);
                        if (item instanceof GenericItem) {
                            GenericItem gItem = (GenericItem) item;
                            handleTags(gItem::addTag, args[2], gItem, console);
                        }
                    } else {
                        console.println("Specify the name of the item and the tag: " + getCommand() + " "
                                + SUBCMD_ADDTAG + " <itemName> <tag>");
                    }
                    break;
                case SUBCMD_RMTAG:
                    if (args.length > 2) {
                        Item item = itemRegistry.get(args[1]);
                        if (item instanceof GenericItem) {
                            GenericItem gItem = (GenericItem) item;
                            handleTags(gItem::removeTag, args[2], gItem, console);
                        }
                    } else {
                        console.println("Specify the name of the item and the tag: " + getCommand() + " " + SUBCMD_RMTAG
                                + " <itemName> <tag>");
                    }
                    break;
                default:
                    console.println("Unknown command '" + subCommand + "'");
                    printUsage(console);
                    break;
            }
        } else {
            printUsage(console);
        }
    }

    private <T> void handleTags(final Consumer<T> func, final T tag, GenericItem gItem, Console console) {
        // allow adding/removing of tags only for managed items
        if (managedItemProvider.get(gItem.getName()) != null) {
            // add or remove tag method is passed here
            func.accept(tag);

            Item oldItem = itemRegistry.update(gItem);
            if (oldItem != null) {
                console.println("Successfully changed tag " + tag + " on item " + gItem.getName());
            }
        } else {
            console.println("Error: Cannot change tag " + tag + " on item " + gItem.getName()
                    + " because this item does not belong to a ManagedProvider");
        }
    }

    private void removeItems(Console console, Collection<Item> items) {
        int count = items.size();
        for (Item item : items) {
            itemRegistry.remove(item.getName());
        }
        console.println(count + " item(s) removed successfully.");
    }

    private void listItems(Console console, String pattern) {
        Collection<Item> items = itemRegistry.getItems(pattern);
        if (!items.isEmpty()) {
            for (Item item : items) {
                console.println(item.toString());
            }
        } else {
            if (pattern.isEmpty()) {
                console.println("No item found.");
            } else {
                console.println("No item found for this pattern.");
            }
        }
    }
}
