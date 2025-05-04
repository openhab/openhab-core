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
package org.openhab.core.items.fileconverter;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.items.Item;
import org.openhab.core.items.Metadata;

/**
 * {@link ItemFileParser} is the interface to implement by any file parser for {@link Item} object.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public interface ItemFileParser {

    /**
     * Returns the format of the syntax.
     *
     * @return the syntax format
     */
    String getFileFormatParser();

    /**
     * Parse the provided syntax in file format and return the corresponding {@link Item} and {@link Metadata} objects
     * without impacting the item and metadata registries.
     *
     * @param syntax the syntax in file format
     * @param items the list of {@link Item} to fill
     * @param metadata the list of {@link Metadata} to fill
     * @param errors the list to be used to fill the errors
     * @param warnings the list to be used to fill the warnings
     * @return true if the parsing succeeded without errors
     */
    boolean parseFileFormat(String syntax, List<Item> items, List<Metadata> metadata, List<String> errors,
            List<String> warnings);
}
