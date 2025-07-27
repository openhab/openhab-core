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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
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
     * Parse the provided syntax in file format without impacting the item and metadata registries.
     *
     * @param syntax the syntax in file format
     * @param errors the list to be used to fill the errors
     * @param warnings the list to be used to fill the warnings
     * @return the model name used for parsing if the parsing succeeded without errors; null otherwise
     */
    @Nullable
    String startParsingFileFormat(String syntax, List<String> errors, List<String> warnings);

    /**
     * Get the {@link Item} objects found when parsing the file format.
     *
     * @param modelName the model name used for parsing
     * @return the collection of items
     */
    Collection<Item> getParsedItems(String modelName);

    /**
     * Get the {@link Metadata} objects found when parsing the file format.
     *
     * @param modelName the model name used for parsing
     * @return the collection of metadata
     */
    Collection<Metadata> getParsedMetadata(String modelName);

    /**
     * Get the state formatters found when parsing the file format.
     *
     * @param modelName the model name used for parsing
     * @return the state formatters as a Map per item name
     */
    Map<String, String> getParsedStateFormatters(String modelName);

    /**
     * Release the data from a previously started file format parsing.
     *
     * @param modelName the model name used for parsing
     */
    void finishParsingFileFormat(String modelName);
}
