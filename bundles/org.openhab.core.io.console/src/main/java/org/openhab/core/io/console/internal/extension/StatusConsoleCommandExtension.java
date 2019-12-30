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

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemNotUniqueException;
import org.openhab.core.items.ItemRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Console command extension to show the current state of an item
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Create DS for command extension
 * @author Dennis Nobel - Changed service references to be injected via DS
 */
@Component(service = ConsoleCommandExtension.class)
@NonNullByDefault
public class StatusConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private final ItemRegistry itemRegistry;

    @Activate
    public StatusConsoleCommandExtension(final @Reference ItemRegistry itemRegistry) {
        super("status", "Get the current status of an item.");
        this.itemRegistry = itemRegistry;
    }

    @Override
    public List<String> getUsages() {
        return Collections.singletonList(buildCommandUsage("<item>", "shows the current status of an item"));
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length > 0) {
            String itemName = args[0];
            try {
                Item item = itemRegistry.getItemByPattern(itemName);
                console.println(item.getState().toString());
            } catch (ItemNotFoundException e) {
                console.println("Error: Item '" + itemName + "' does not exist.");
            } catch (ItemNotUniqueException e) {
                console.print("Error: Multiple items match this pattern: ");
                for (Item item : e.getMatchingItems()) {
                    console.print(item.getName() + " ");
                }
            }
        } else {
            printUsage(console);
        }
    }

}
