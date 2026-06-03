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
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Registry;

/**
 * The {@link LLMToolRegistry} interface provides access to the available {@link LLMTool}s.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public interface LLMToolRegistry extends Registry<LLMTool, String> {

    /**
     * Retrieves a {@link LLMTool} collection.
     * If no services match the provided ids returns an empty list.
     *
     * @param ids Comma separated list of LLM tool ids to use
     * @return a list of {@link LLMTool} or an empty list if none of them is available
     */
    default List<LLMTool> getByIds(@Nullable String ids) {
        return ids == null || ids.isBlank() ? List.of()
                : getByIds(Arrays.stream(ids.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
    }

    /**
     * Retrieves a {@link LLMTool} collection.
     * If no services match the provided ids returns an empty list.
     *
     * @param ids List of LLM tool ids to use
     * @return a list of {@link LLMTool} or an empty list if none of them is available
     */
    List<LLMTool> getByIds(List<String> ids);
}
