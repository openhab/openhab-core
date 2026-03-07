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
package org.openhab.core.common;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A generic interface for parsers that parse strings into specific object types like Things, Items, Rules etc.
 *
 * @param <T> The object type.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public interface ObjectParser<T> {

    /**
     * Get the name of the format.
     *
     * @return The format name.
     */
    String getParserFormat();

    /**
     * Parse the provided syntax in format without impacting any object registries.
     *
     * @param syntax the syntax in format.
     * @param errors the {@link List} to use to report errors.
     * @param warnings the {@link List} to be used to report warnings.
     * @return The model name used for parsing if the parsing succeeded without errors; {@code null} otherwise.
     */
    @Nullable
    String startParsingFormat(String syntax, List<String> errors, List<String> warnings);

    /**
     * Get the objects found when parsing the format.
     *
     * @param modelName the model name whose objects to get.
     * @return The {@link Collection} of objects.
     */
    Collection<T> getParsedObjects(String modelName);

    /**
     * Release the resources from a previously started format parsing.
     *
     * @param modelName the model name whose resources to release.
     */
    void finishParsingFormat(String modelName);
}
