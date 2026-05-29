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
package org.openhab.core.ui.components.converter;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.converter.ObjectParser;
import org.openhab.core.ui.components.UIComponent;

/**
 * {@link UIComponentParser} is the interface to implement by any format parser for {@link UIComponent} objects.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public interface UIComponentParser extends ObjectParser<UIComponent> {

    /**
     * Parse the provided {@code syntax} string without impacting the model.
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
     * Get a copy of the collection of {@link UIComponent} objects that were found when parsing the format.
     *
     * @param modelName the model name used when parsing.
     * @return The {@link Collection} of {@link UIComponent}s.
     *
     * @implNote It's important that a copy of the {@link Collection} is returned, so that invoking
     *           {@link #finishParsingFormat(String)} doesn't modify the returned result.
     */
    @Override
    Collection<? extends UIComponent> getParsedObjects(String modelName);
}
