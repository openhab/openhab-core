/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
import org.openhab.core.common.ObjectParser;
import org.openhab.core.items.Item;
import org.openhab.core.items.Metadata;

/**
 * {@link ItemParser} is the interface to implement by any file parser for {@link Item} object.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public interface ItemParser extends ObjectParser<Item> {

    /**
     * Parse the provided {@code syntax} string without impacting the item and metadata registries.
     *
     * @param syntax the syntax in format.
     * @param errors the {@link List} to use to report errors.
     * @param warnings the {@link List} to be used to report warnings.
     * @return The model name used for parsing if the parsing succeeded without errors; {@code null} otherwise.
     */
    @Override
    @Nullable
    String startParsingFormat(String syntax, List<String> errors, List<String> warnings);

    /**
     * Get the {@link Item} objects found when parsing the format.
     *
     * @param modelName the model name used when parsing.
     * @return The {@link Collection} of {@link Item}s.
     */
    @Override
    Collection<Item> getParsedObjects(String modelName);

    /**
     * Get the {@link Metadata} objects found when parsing the format.
     *
     * @param modelName the model name used when parsing.
     * @return The {@link Collection} of {@link Metadata}.
     */
    Collection<Metadata> getParsedMetadata(String modelName);

    /**
     * Get the state formatters found when parsing the format.
     *
     * @param modelName the model name used when parsing.
     * @return the state formatters as a {@link Map} per item name.
     */
    Map<String, String> getParsedStateFormatters(String modelName);
}
