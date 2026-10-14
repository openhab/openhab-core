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

import java.io.Serial;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * An exception thrown by {@link HumanLanguageInterpreter}s when an operation encounters
 * an unrecoverable error that must not fall back to other interpreters.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class UnrecoverableInterpretationException extends InterpretationException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UnrecoverableInterpretationException(String msg) {
        super(msg);
    }

    public UnrecoverableInterpretationException(String msg, @Nullable Throwable cause) {
        super(msg, cause);
    }
}
