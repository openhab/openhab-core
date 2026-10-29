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

import java.io.Serial;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Thrown when an {@link LLMTool} invocation encounters an unrecoverable error
 * (e.g., security or permission restrictions, read-only Item, safety interlock).
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class UnrecoverableLLMToolException extends LLMToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UnrecoverableLLMToolException(String message) {
        super(message);
    }

    public UnrecoverableLLMToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
