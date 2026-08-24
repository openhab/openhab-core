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
 * General purpose {@link LLMTool} exception.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public class LLMToolException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    public LLMToolException() {
        super();
    }

    public LLMToolException(String message, Throwable cause) {
        super(message, cause);
    }

    public LLMToolException(String message) {
        super(message);
    }

    public LLMToolException(Throwable cause) {
        super(cause);
    }
}
