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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.ui.components.RootUIComponent;

/**
 * {@link RootUIComponentParser} is the interface to implement by any format parser for {@link RootUIComponent}
 * objects.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public interface RootUIComponentParser extends UIComponentParser {

    /**
     * Get a copy of the collection of {@link RootUIComponent} objects that were found when parsing the format.
     *
     * @param modelName the model name used when parsing.
     * @return The {@link Collection} of {@link RootUIComponent}s.
     *
     * @implNote It's important that a copy of the {@link Collection} is returned, so that invoking
     *           {@link #finishParsingFormat(String)} doesn't modify the returned result.
     */
    @Override
    Collection<? extends RootUIComponent> getParsedObjects(String modelName);

    /**
     * Get a copy of the collection of {@link RootUIComponent} objects of the specified type, that were found when
     * parsing the format.
     *
     * @param modelName the model name used when parsing.
     * @param type the {@link RootUIComponentType} to retrieve.
     * @return The {@link Collection} of {@link RootUIComponent}s.
     *
     * @implNote It's important that a copy of the {@link Collection} is returned, so that invoking
     *           {@link #finishParsingFormat(String)} doesn't modify the returned result.
     */
    Collection<? extends RootUIComponent> getParsedObjects(String modelName, RootUIComponentType type);

    public enum RootUIComponentType {
        BLOCK_LIBRARY,
        PAGE,
        WIDGET
    }
}
