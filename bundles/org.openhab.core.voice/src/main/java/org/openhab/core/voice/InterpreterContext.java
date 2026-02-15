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
package org.openhab.core.voice;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.voice.text.Conversation;
import org.openhab.core.voice.text.LLMTool;

/**
 * Context passed to the {@link org.openhab.core.voice.text.HumanLanguageInterpreter}
 * when interpreting a new input text.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public record InterpreterContext(Conversation conversation, List<LLMTool> tools, @Nullable String locationItem) {

}
