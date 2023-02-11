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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Implementing this interface allows a {@link ConsoleCommandExtension} to
 * provide completions for the user as they write commands.
 *
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault
public interface ConsoleCommandCompleter {
    /**
     * Populate possible completion candidates.
     * 
     * @param args An array of all arguments to be passed to the ConsoleCommandExtension's execute method
     * @param cursorArgumentIndex the argument index the cursor is currently in
     * @param cursorPosition the position of the cursor within the argument
     * @param candidates a list to fill with possible completion candidates
     * @return if a candidate was found
     */
    boolean complete(String[] args, int cursorArgumentIndex, int cursorPosition, List<String> candidates);
}
