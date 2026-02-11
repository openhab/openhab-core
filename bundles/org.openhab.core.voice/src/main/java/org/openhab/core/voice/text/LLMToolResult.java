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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link LLMToolResult} interface represents a programmatic tool
 * designed to be consumed by an LLM.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public interface LLMToolResult {

    /**
     * Returns a simple string that uniquely identifies the {@link LLMTool} service that produced th
     *
     * @return an id that identifies this service
     */
    String getToolId();
}
