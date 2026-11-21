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
package org.openhab.core.voice.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.voice.VoiceManager;
import org.openhab.core.voice.dialog.DialogContext;
import org.openhab.core.voice.dialog.trigger.BasicDTService;
import org.openhab.core.voice.dialog.trigger.DTListener;
import org.openhab.core.voice.dialog.trigger.DTServiceHandle;
import org.openhab.core.voice.dialog.trigger.ks.KSException;
import org.openhab.core.voice.dialog.trigger.ks.KSListener;
import org.openhab.core.voice.dialog.trigger.ks.KSService;
import org.openhab.core.voice.dialog.trigger.ks.KSServiceHandle;
import org.openhab.core.voice.stt.STTService;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.interpreter.llm.LLMToolRegistry;
import org.openhab.core.voice.tts.TTSService;

/**
 * Tests for {@link AudioDialogProviderImpl}.
 *
 * @author Dan Cunningham - Initial contribution
 */
@NonNullByDefault
public class AudioDialogProviderImplTest {

    private final VoiceManager voiceManager = mock(VoiceManager.class);
    private final AudioSink sink = mock(AudioSink.class);
    private final AudioSource source = mock(AudioSource.class);
    private final AtomicInteger abortCount = new AtomicInteger();
    private final AtomicInteger idleCount = new AtomicInteger();
    private @NonNullByDefault({}) AudioDialogProviderImpl provider;

    @BeforeEach
    public void setUp() {
        when(voiceManager.getDialogContextBuilder()).thenAnswer(
                invocation -> new DialogContext.Builder("keyword", Locale.ENGLISH).withSTT(mock(STTService.class))
                        .withTTS(mock(TTSService.class)).withHLI(mock(HumanLanguageInterpreter.class)));
        provider = new AudioDialogProviderImpl(voiceManager, mock(LLMToolRegistry.class));
    }

    @Test
    public void triggeredDialogRunsOnIdleAndOnAbort() {
        AtomicReference<@Nullable DTServiceHandle> handleRef = new AtomicReference<>();
        when(voiceManager.startDialog(any(DialogContext.class))).thenAnswer(invocation -> {
            DialogContext context = invocation.getArgument(0);
            BasicDTService dt = assertInstanceOf(BasicDTService.class, context.dt());
            DTServiceHandle handle = dt.registerListener(mock(DTListener.class));
            handleRef.set(handle);
            return handle;
        });

        Runnable trigger = provider.startDialog(sink, source, null, null, abortCount::incrementAndGet,
                idleCount::incrementAndGet);
        assertNotNull(trigger);
        DTServiceHandle handle = handleRef.get();
        assertNotNull(handle);

        handle.onDialogIdle();
        handle.onDialogIdle();
        assertEquals(2, idleCount.get());
        assertEquals(0, abortCount.get());

        handle.abort();
        assertEquals(1, abortCount.get());
        handle.onDialogIdle();
        assertEquals(2, idleCount.get());
    }

    @Test
    public void keywordSpottingDialogRunsOnIdleAndPassesItToTheKeywordSpotter() throws KSException {
        KSService ks = mock(KSService.class);
        KSServiceHandle ksHandle = mock(KSServiceHandle.class);
        when(ks.spot(any(), any(), any(), any())).thenReturn(ksHandle);
        when(voiceManager.getKS()).thenReturn(ks);
        when(voiceManager.startDialog(any(DialogContext.class))).thenReturn(mock(DTServiceHandle.class));

        assertTrue(provider.startKeywordSpottingDialog(sink, source, null, null, abortCount::incrementAndGet,
                idleCount::incrementAndGet));
        ArgumentCaptor<DialogContext> contextCaptor = ArgumentCaptor.forClass(DialogContext.class);
        verify(voiceManager).startDialog(contextCaptor.capture());
        KSService wrapped = assertInstanceOf(KSService.class, contextCaptor.getValue().dt());
        KSServiceHandle handle = wrapped.spot(mock(KSListener.class), mock(AudioStream.class), Locale.ENGLISH,
                "keyword");

        handle.onDialogIdle();
        assertEquals(1, idleCount.get());
        verify(ksHandle).onDialogIdle();
        assertEquals(0, abortCount.get());

        handle.abort();
        verify(ksHandle).abort();
        assertEquals(1, abortCount.get());
        handle.onDialogIdle();
        assertEquals(1, idleCount.get());
        verify(ksHandle).onDialogIdle();
    }
}
