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
package org.eclipse.smarthome.io.console.internal.extension;

import java.util.Collections;
import java.util.List;

import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.items.ItemNotUniqueException;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.events.ItemEventFactory;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.TypeParser;
import org.eclipse.smarthome.io.console.Console;
import org.eclipse.smarthome.io.console.extensions.AbstractConsoleCommandExtension;
import org.eclipse.smarthome.io.console.extensions.ConsoleCommandExtension;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Console command extension to send status update to item
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Markus Rathgeb - Create DS for command extension
 * @author Dennis Nobel - Changed service references to be injected via DS
 * @author Stefan Bußweiler - Migration to new ESH event concept
 *
 */
@Component(service = ConsoleCommandExtension.class)
public class UpdateConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private ItemRegistry itemRegistry;
    private EventPublisher eventPublisher;

    public UpdateConsoleCommandExtension() {
        super("update", "Send a state update to an item.");
    }

    @Override
    public List<String> getUsages() {
        return Collections.singletonList(buildCommandUsage("<item> <state>", "sends a status update for an item"));
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length > 0) {
            String itemName = args[0];
            try {
                Item item = this.itemRegistry.getItemByPattern(itemName);
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

    @Reference
    protected void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    protected void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    @Reference
    protected void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }
}
