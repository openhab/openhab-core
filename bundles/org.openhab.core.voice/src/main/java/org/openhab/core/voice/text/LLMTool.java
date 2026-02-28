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
package org.openhab.core.voice.text;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link LLMTool} interface represents a programmatic tool
 * designed to be consumed by an LLM.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public interface LLMTool {

    /**
     * Returns a simple string that uniquely identifies this service
     *
     * @return an id that identifies this service
     */
    String getId();

    /**
     * Returns a localized human-readable label that can be used within UIs.
     *
     * @param locale the locale to provide the label for
     * @return a localized string to be used in UIs
     */
    String getLabel(@Nullable Locale locale);

    /**
     * Returns a short localized human-readable description of the tool.
     *
     * @param locale the locale to provide the label for
     * @return a localized string that shorty describes the tool purpose.
     */
    String getShortDescription(@Nullable Locale locale);

    /**
     * Returns a localized human-readable description of the tool.
     *
     * @param locale the locale to provide the label for
     * @return a localized string to be feed to an LLM
     */
    String getDescription(@Nullable Locale locale);

    /**
     * Returns a localized {@link LLMToolParam} list with
     * human-readable descriptions of the tool parameters
     *
     * @param locale the locale to provide the label for
     * @return localized descriptions of the tool params.
     */
    List<LLMToolParam> getParamDescriptions(@Nullable Locale locale);

    /**
     * Invoke the tool implementation .
     *
     * @param params Map of param names and values
     */
    LLMToolResult call(Map<String, String> params, @Nullable Locale locale) throws LLMToolException;
}
