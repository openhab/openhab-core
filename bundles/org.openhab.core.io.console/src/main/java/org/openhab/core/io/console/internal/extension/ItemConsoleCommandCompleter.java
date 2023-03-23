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

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.console.ConsoleCommandCompleter;
import org.openhab.core.io.console.StringsCompleter;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemNotUniqueException;
import org.openhab.core.items.ItemRegistry;

/**
 * Console command completer for send and update
 *
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault
public class ItemConsoleCommandCompleter implements ConsoleCommandCompleter {
    private final ItemRegistry itemRegistry;
    private final @Nullable Function<Item, Class<?>[]> dataTypeGetter;

    public ItemConsoleCommandCompleter(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
        this.dataTypeGetter = null;
    }

    public ItemConsoleCommandCompleter(ItemRegistry itemRegistry, Function<Item, Class<?>[]> dataTypeGetter) {
        this.itemRegistry = itemRegistry;
        this.dataTypeGetter = dataTypeGetter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean complete(String[] args, int cursorArgumentIndex, int cursorPosition, List<String> candidates) {
        if (cursorArgumentIndex <= 0) {
            return new StringsCompleter(
                    itemRegistry.getAll().stream().map(i -> i.getName()).collect(Collectors.toList()), true)
                    .complete(args, cursorArgumentIndex, cursorPosition, candidates);
        }
        var localDataTypeGetter = dataTypeGetter;
        if (cursorArgumentIndex == 1 && localDataTypeGetter != null) {
            try {
                Item item = itemRegistry.getItemByPattern(args[0]);
                Stream<Class<?>> enums = Stream.of(localDataTypeGetter.apply(item)).filter(Class::isEnum);
                Stream<? super Enum<?>> enumConstants = enums.flatMap(
                        t -> Stream.of(Objects.requireNonNull(((Class<? extends Enum<?>>) t).getEnumConstants())));
                return new StringsCompleter(enumConstants.map(Object::toString).collect(Collectors.toList()), false)
                        .complete(args, cursorArgumentIndex, cursorPosition, candidates);
            } catch (ItemNotFoundException | ItemNotUniqueException e) {
                return false;
            }
        }
        return false;
    }
}
