/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Completer for a set of strings
 *
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault
public class StringsCompleter implements Completer {
    private final SortedSet<String> strings;
    private final boolean caseSensitive;

    public StringsCompleter() {
        this(false);
    }

    public StringsCompleter(final boolean caseSensitive) {
        this.strings = new TreeSet<>(caseSensitive ? String::compareTo : String::compareToIgnoreCase);
        this.caseSensitive = caseSensitive;
    }

    public StringsCompleter(final Collection<String> strings) {
        this(strings, false);
    }

    public StringsCompleter(final Collection<String> strings, boolean caseSensitive) {
        this(caseSensitive);
        getStrings().addAll(strings);
    }

    public StringsCompleter(final String[] strings, boolean caseSensitive) {
        this(Arrays.asList(strings), caseSensitive);
    }

    public StringsCompleter(final String[] strings) {
        this(Arrays.asList(strings), false);
    }

    public SortedSet<String> getStrings() {
        return strings;
    }

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
