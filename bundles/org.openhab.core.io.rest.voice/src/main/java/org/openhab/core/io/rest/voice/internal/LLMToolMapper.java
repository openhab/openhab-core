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
package org.openhab.core.io.rest.voice.internal;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.voice.text.interpreter.llm.LLMTool;

/**
 * Mapper class that maps {@link LLMTool} instances to their respective DTOs.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class LLMToolMapper {

    /**
     * Maps a {@link LLMTool} to a {@link LLMToolDTO}.
     *
     * @param tool the LLM tool
     * @param locale the locale to use for the DTO
     *
     * @return the corresponding DTO
     */
    public static LLMToolDTO map(LLMTool tool, @Nullable Locale locale) {
        return new LLMToolDTO(tool.getUID(), tool.getLabel(locale), tool.getDescription(locale));
    }
}
