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
package org.openhab.core.io.console;

import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Completer for a set of strings.
 * 
 * It will provide candidate completions for whichever argument the cursor is located in.
 *
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault
public class StringsCompleter implements ConsoleCommandCompleter {
    private final SortedSet<String> strings;
    private final boolean caseSensitive;

    public StringsCompleter() {
        this(List.of(), false);
    }

    /**
     * @param strings The set of valid strings to be completed
     * @param caseSensitive if strings must match case sensitively when the user is typing them
     */
    public StringsCompleter(final Collection<String> strings, boolean caseSensitive) {
        this.strings = new TreeSet<>(caseSensitive ? String::compareTo : String::compareToIgnoreCase);
        this.caseSensitive = caseSensitive;
        this.strings.addAll(strings);
    }

    /**
     * Gets the strings that are allowed for this completer, so that you can modify the set.
     */
    public SortedSet<String> getStrings() {
        return strings;
    }

    @Override
    public boolean complete(String[] args, int cursorArgumentIndex, int cursorPosition, List<String> candidates) {
        String argument;
        if (cursorArgumentIndex >= 0 && cursorArgumentIndex < args.length) {
            argument = args[cursorArgumentIndex].substring(0, cursorPosition);
        } else {
            argument = "";
        }

        if (!caseSensitive) {
            argument = argument.toLowerCase();
        }

        SortedSet<String> matches = getStrings().tailSet(argument);

        for (String match : matches) {
            String s = caseSensitive ? match : match.toLowerCase();
            if (!s.startsWith(argument)) {
                break;
            }

            candidates.add(match + " ");
        }

        return !candidates.isEmpty();
    }
}
