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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link LLMToolParam} class describe each of the parameters of a {@link LLMTool}.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 *
 * @param name unique name of the parameter
 * @param type type of the parameter
 * @param description parameter description
 * @param options if not empty, defines a list of allowed values for the parameter
 * @param required whether the parameter is required
 */
@NonNullByDefault
public record LLMToolParam(String name, LLMToolParamType type, String description, List<String> options,
        boolean required) {
}
