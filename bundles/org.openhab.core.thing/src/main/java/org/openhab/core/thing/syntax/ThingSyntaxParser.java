/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.thing.syntax;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Thing;

/**
 * {@link ThingSyntaxParser} is the interface to implement by any syntax parser for {@link Thing} object.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public interface ThingSyntaxParser {

    /**
     * Returns the format of the syntax.
     *
     * @return the syntax format
     */
    String getParserFormat();

    /**
     * Parse the provided syntax and return the corresponding {@link Thing} objects without impacting
     * the thing registry.
     *
     * @param syntax the syntax
     * @param things the collection of {@link Thing} to fill
     * @param errors the string builder used to fill the errors
     * @param warnings the string builder used to fill the warnings
     * @return true if the parsing succeeded without errors
     */
    boolean parseSyntax(String syntax, Collection<Thing> things, StringBuilder errors, StringBuilder warnings);
}
