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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link LLMToolRegistryListener} interface provides a way to listen for changes in the {@link LLMToolRegistry}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public interface LLMToolRegistryListener {

    /**
     * Called when a {@link LLMTool} has been added to the registry.
     *
     * @param tool the added LLM tool
     */
    void onLLMToolAdded(LLMTool tool);

    /**
     * Called when a {@link LLMTool} has been removed from the registry.
     *
     * @param tool the removed LLM tool
     */
    void onLLMToolRemoved(LLMTool tool);
}
