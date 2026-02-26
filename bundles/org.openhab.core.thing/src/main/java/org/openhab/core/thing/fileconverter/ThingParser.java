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
package org.openhab.core.thing.fileconverter;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ObjectParser;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.link.ItemChannelLink;

/**
 * {@link ThingParser} is the interface to implement by any {@link Thing} parser.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public interface ThingParser extends ObjectParser<Thing> {

    /**
     * Parse the provided {@code syntax} string without impacting the thing registry.
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
     * Get the {@link Thing} objects found when parsing the format.
     *
     * @param modelName the model name used when parsing.
     * @return The {@link Collection} of {@link Thing}s.
     */
    @Override
    Collection<Thing> getParsedObjects(String modelName);

    /**
     * Get the {@link ItemChannelLink} objects found when parsing the format.
     *
     * @param modelName the model name used when parsing.
     * @return The {@link Collection} of {@link ItemChannelLink}s.
     */
    Collection<ItemChannelLink> getParsedChannelLinks(String modelName);
}
