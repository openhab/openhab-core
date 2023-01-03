/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataPredicates;
import org.openhab.core.items.MetadataRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 *
 * Console command extension for the {@link MetadataRegistry}.
 *
 * @author Andre Fuechsel - Initial contribution
 * @author Jan N. Klug - Added removal of orphaned metadata
 */
@Component(service = ConsoleCommandExtension.class)
@NonNullByDefault
public class MetadataConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private static final String SUBCMD_LIST = "list";
    private static final String SUBCMD_LIST_INTERNAL = "listinternal";
    private static final String SUBCMD_ADD = "add";
    private static final String SUBCMD_REMOVE = "remove";
    private static final String SUBCMD_ORPHAN = "orphan";

    private final ItemRegistry itemRegistry;
    private final MetadataRegistry metadataRegistry;

    @Activate
    public MetadataConsoleCommandExtension(final @Reference ItemRegistry itemRegistry,
            final @Reference MetadataRegistry metadataRegistry) {
        super("metadata", "Access the metadata registry.");
        this.itemRegistry = itemRegistry;
        this.metadataRegistry = metadataRegistry;
    }

    @Override
    public List<String> getUsages() {
        return Arrays.asList( //
                buildCommandUsage(SUBCMD_LIST + " [<itemName> [<namespace>]]",
                        "lists all available metadata, can be filtered for a specifc item and namespace"),
                buildCommandUsage(SUBCMD_LIST_INTERNAL + " [<itemName> [<namespace>]]",
                        "lists all available INTERNAL metadata, can be filtered for a specifc item and namespace"),
                buildCommandUsage(SUBCMD_REMOVE + " <itemName> [<namespace>]",
                        "removes metadata for the specific item (for all namespaces or for the given namespace only)"),
                buildCommandUsage(SUBCMD_ADD + " <itemName> <namespace> <value> [\"{key1=value1, key2=value2, ...}\"]",
                        "adds or updates metadata value (and optional config values) for the specific item in the given namespace"),
                buildCommandUsage(SUBCMD_ORPHAN + " list|purge",
                        "lists or removes all metadata for which no corresponding item is present"));
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length > 0) {
            String subCommand = args[0];
            switch (subCommand) {
                case SUBCMD_LIST:
                    listMetadata(console, args.length > 1 ? args[1] : null, args.length > 2 ? args[2] : null, false);
                    break;
                case SUBCMD_LIST_INTERNAL:
                    listMetadata(console, args.length > 1 ? args[1] : null, args.length > 2 ? args[2] : null, true);
                    break;
                case SUBCMD_ADD:
                    if (args.length < 4) {
                        printUsage(console);
                    } else {
                        addMetadata(console, args[1], args[2], args[3], args.length > 4 ? args[4] : null);
                    }
                    break;
                case SUBCMD_ORPHAN:
                    if (args.length == 2 && (args[1].equals("list") || args[1].equals("purge"))) {
                        orphan(console, args[1], metadataRegistry.getAll(), itemRegistry.getAll());
                    } else {
                        console.println("Specify action 'list' or 'purge' to be executed: orphan <list|purge>");
                    }
                    return;

                case SUBCMD_REMOVE:
                    removeMetadata(console, args[1], args.length > 2 ? args[2] : null);
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

    private void listMetadata(Console console, @Nullable String itemName, @Nullable String namespace,
            boolean internal) {
        if (itemName == null) {
            metadataRegistry.stream().filter(m -> isInternal(m, internal)).map(Metadata::toString)
                    .forEach(console::println);
        } else if (namespace == null) {
            metadataRegistry.stream().filter(MetadataPredicates.ofItem(itemName)).filter(m -> isInternal(m, internal))
                    .map(Metadata::toString).forEach(console::println);
        } else {
            MetadataKey key = new MetadataKey(namespace, itemName);
            if (metadataRegistry.isInternalNamespace(namespace) == internal) {
                Metadata metadata = metadataRegistry.get(key);
                if (metadata != null) {
                    console.println(metadata.toString());
                }
            }
        }
    }

    private boolean isInternal(Metadata metadata, boolean internal) {
        return metadataRegistry.isInternalNamespace(metadata.getUID().getNamespace()) == internal;
    }

    private void addMetadata(Console console, String itemName, String namespace, String value,
            @Nullable String config) {
        if (itemRegistry.get(itemName) == null) {
            console.println("Item " + itemName + " does not exist.");
        } else {
            MetadataKey key = new MetadataKey(namespace, itemName);
            Map<String, Object> configMap = getConfigMap(config);
            Metadata metadata = new Metadata(key, value, configMap);
            if (metadataRegistry.get(key) != null) {
                metadataRegistry.update(metadata);
                console.println("Updated: " + metadata);
            } else {
                metadataRegistry.add(metadata);
                console.println("Added: " + metadata);
            }
        }
    }

    private @Nullable Map<String, Object> getConfigMap(@Nullable String config) {
        if (config == null) {
            return null;
        }
        String configStr = config;
        if (configStr.startsWith("{") && configStr.endsWith("}")) {
            configStr = configStr.substring(1, configStr.length() - 1);
        }

        Map<String, Object> map = new HashMap<>();
        for (String part : configStr.split("\\s*,\\s*")) {
            String[] subparts = part.split("=", 2);
            if (subparts.length == 2) {
                map.put(subparts[0].trim(), subparts[1].trim());
            }
        }
        return map;
    }

    private void removeMetadata(Console console, String itemName, @Nullable String namespace) {
        if (itemRegistry.get(itemName) == null) {
            console.println("Warning: Item " + itemName + " does not exist, removing metadata anyway.");
        }
        if (namespace == null) {
            metadataRegistry.stream().filter(MetadataPredicates.ofItem(itemName)).map(Metadata::getUID)
                    .forEach(key -> removeMetadata(console, key));
        } else {
            MetadataKey key = new MetadataKey(namespace, itemName);
            removeMetadata(console, key);
        }
    }

    private void removeMetadata(Console console, MetadataKey key) {
        Metadata metadata = metadataRegistry.remove(key);
        if (metadata != null) {
            console.println("Removed: " + metadata);
        } else {
            console.println("Metadata element for " + key + " could not be found.");
        }
    }

    private void orphan(Console console, String action, Collection<Metadata> metadata, Collection<Item> items) {
        Collection<String> itemNames = items.stream().map(Item::getName).collect(Collectors.toCollection(HashSet::new));

        metadata.forEach(md -> {
            if (!itemNames.contains(md.getUID().getItemName())) {
                console.println("Item missing: " + md.getUID());
                if ("purge".equals(action)) {
                    metadataRegistry.remove(md.getUID());
                }
            }
        });
    }
}
