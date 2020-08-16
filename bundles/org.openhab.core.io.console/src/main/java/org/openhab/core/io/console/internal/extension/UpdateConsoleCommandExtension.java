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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemNotUniqueException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.types.State;
import org.openhab.core.types.TypeParser;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Console command extension to send status update to item
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Create DS for command extension
 * @author Dennis Nobel - Changed service references to be injected via DS
 * @author Stefan Bußweiler - Migration to new ESH event concept
 */
@Component(service = ConsoleCommandExtension.class)
@NonNullByDefault
public class UpdateConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private final ItemRegistry itemRegistry;
    private final EventPublisher eventPublisher;

    @Activate
    public UpdateConsoleCommandExtension(final @Reference ItemRegistry itemRegistry,
            final @Reference EventPublisher eventPublisher) {
        super("update", "Send a state update to an item.");
        this.itemRegistry = itemRegistry;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public List<String> getUsages() {
        return List.of(buildCommandUsage("<item> <state>", "sends a status update for an item"));
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length > 0) {
            String itemName = args[0];
            try {
                Item item = itemRegistry.getItemByPattern(itemName);
                if (args.length > 1) {
                    String stateName = args[1];
                    State state = TypeParser.parseState(item.getAcceptedDataTypes(), stateName);
                    if (state != null) {
                        eventPublisher.post(ItemEventFactory.createStateEvent(item.getName(), state));
                        console.println("Update has been sent successfully.");
                    } else {
                        console.println("Error: State '" + stateName + "' is not valid for item '" + itemName + "'");
                        console.print("Valid data types are: ( ");
                        for (Class<? extends State> acceptedType : item.getAcceptedDataTypes()) {
                            console.print(acceptedType.getSimpleName() + " ");
                        }
                        console.println(")");
                    }
                } else {
                    printUsage(console);
                }
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
