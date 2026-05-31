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
package org.openhab.core.voice.text.interpreter.llm;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link LLMToolRegistry} interface provides access to the available {@link LLMTool}s.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public interface LLMToolRegistry {

    /**
     * Retrieves a {@link LLMTool} collection.
     * If no services match the provided ids returns an empty list.
     *
     * @param ids Comma separated list of LLM tool ids to use
     * @return a list of {@link LLMTool} or an empty list if none of them is available
     */
    default List<LLMTool> getLLMToolsByIds(@Nullable String ids) {
        return ids == null || ids.isBlank() ? List.of() : getLLMToolsByIds(Arrays.asList(ids.split(",")));
    }

    /**
     * Retrieves a {@link LLMTool} collection.
     * If no services are available returns an empty list.
     *
     * @param ids List of LLM tool ids to use
     * @return a list of {@link LLMTool} or an empty list if none of them is available
     */
    List<LLMTool> getLLMToolsByIds(List<String> ids);

    /**
     * Retrieves all {@link LLMTool}s.
     *
     * @return a collection of all available LLM tools
     */
    Collection<LLMTool> getLLMTools();

    /**
     * Retrieves a {@link LLMTool} by its id.
     *
     * @param id the tool id
     * @return the LLM tool or null if it's not available
     */
    @Nullable
    LLMTool getLLMTool(String id);

    /**
     * Adds a {@link LLMToolRegistryListener} to the registry.
     *
     * @param listener the listener to add
     */
    void addLLMToolRegistryListener(LLMToolRegistryListener listener);

    /**
     * Removes a {@link LLMToolRegistryListener} from the registry.
     *
     * @param listener the listener to remove
     */
    void removeLLMToolRegistryListener(LLMToolRegistryListener listener);
}
