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
package org.openhab.core.thing.fileconverter;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.link.ItemChannelLink;

/**
 * {@link ThingFileParser} is the interface to implement by any file parser for {@link Thing} object.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public interface ThingFileParser {

    /**
     * Returns the format of the syntax.
     *
     * @return the syntax format
     */
    String getFileFormatParser();

    /**
     * Parse the provided syntax in file format without impacting the thing registry.
     *
     * @param syntax the syntax in file format
     * @param errors the list to be used to fill the errors
     * @param warnings the list to be used to fill the warnings
     * @return the model name used for parsing if the parsing succeeded without errors; null otherwise
     */
    @Nullable
    String startParsingFileFormat(String syntax, List<String> errors, List<String> warnings);

    /**
     * Get the {@link Thing} objects found when parsing the file format.
     *
     * @param modelName the model name used for parsing
     * @return the collection of things
     */
    Collection<Thing> getParsedThings(String modelName);

    /**
     * Get the {@link ItemChannelLink} objects found when parsing the file format.
     *
     * @param modelName the model name used for parsing
     * @return the collection of items channel links
     */
    Collection<ItemChannelLink> getParsedChannelLinks(String modelName);

    /**
     * Release the data from a previously started file format parsing.
     *
     * @param modelName the model name used for parsing
     */
    void finishParsingFileFormat(String modelName);
}
