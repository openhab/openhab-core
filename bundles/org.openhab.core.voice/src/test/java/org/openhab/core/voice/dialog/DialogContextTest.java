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
package org.openhab.core.voice.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.voice.stt.STTService;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.interpreter.llm.LLMTool;
import org.openhab.core.voice.tts.TTSService;

/**
 * Tests for {@link DialogContext}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class DialogContextTest {

    @Test
    public void withLLMToolsClearsToolsEvenIfAlreadySet() {
        DialogContext.Builder builder = new DialogContext.Builder("keyword", Locale.ENGLISH)
                .withSource(mock(AudioSource.class)).withSink(mock(AudioSink.class)).withSTT(mock(STTService.class))
                .withTTS(mock(TTSService.class)).withHLI(mock(HumanLanguageInterpreter.class));

        LLMTool tool = mock(LLMTool.class);
        builder.withLLMTools(List.of(tool));
        DialogContext contextWithTools = builder.build();
        assertEquals(List.of(tool), contextWithTools.llmTools());

        builder.withLLMTools(List.of());
        DialogContext contextCleared = builder.build();
        assertTrue(contextCleared.llmTools().isEmpty());
    }
}
