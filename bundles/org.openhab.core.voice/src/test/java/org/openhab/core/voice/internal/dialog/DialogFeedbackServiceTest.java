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
package org.openhab.core.voice.internal.dialog;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.voice.dialog.DialogContext;
import org.openhab.core.voice.stt.STTService;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.tts.TTSService;

/**
 * Tests for {@link DialogFeedbackService}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class DialogFeedbackServiceTest {

    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final AudioSink audioSink = mock(AudioSink.class);
    private final AudioSource audioSource = mock(AudioSource.class);
    private final STTService stt = mock(STTService.class);
    private final TTSService tts = mock(TTSService.class);
    private final HumanLanguageInterpreter hli = mock(HumanLanguageInterpreter.class);

    private DialogContext createContext(@NonNullByDefault({}) String listeningItem,
            @NonNullByDefault({}) String melody) {
        return new DialogContext.Builder("keyword", Locale.ENGLISH).withSink(audioSink).withSource(audioSource)
                .withSTT(stt).withTTS(tts).withHLI(hli).withListeningItem(listeningItem).withMelody(melody).build();
    }

    @Test
    public void setListeningTogglesItemCommandEvent() {
        DialogContext context = createContext("TestItem", null);
        DialogFeedbackService feedback = new DialogFeedbackService(context, eventPublisher);

        feedback.setListening(true);
        verify(eventPublisher, times(1)).post(argThat(event -> event instanceof ItemCommandEvent ice
                && ice.getItemName().equals("TestItem") && ice.getItemCommand().equals(OnOffType.ON)));

        // Duplicate call should not send another event
        feedback.setListening(true);
        verify(eventPublisher, times(1)).post(any());

        feedback.setListening(false);
        verify(eventPublisher, times(1)).post(argThat(event -> event instanceof ItemCommandEvent ice
                && ice.getItemName().equals("TestItem") && ice.getItemCommand().equals(OnOffType.OFF)));
    }

    @Test
    public void setListeningWithoutItemDoesNotPostEvent() {
        DialogContext context = createContext(null, null);
        DialogFeedbackService feedback = new DialogFeedbackService(context, eventPublisher);

        feedback.setListening(true);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    public void playSoundsToAudioSinkWhenCompatible() throws Exception {
        when(audioSink.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(audioSink.getSupportedStreams()).thenReturn(Set.of(AudioStream.class));

        DialogContext context = createContext(null, "A B C");
        DialogFeedbackService feedback = new DialogFeedbackService(context, eventPublisher);

        feedback.playStartSound();
        feedback.playListeningSound();
        feedback.playStopSound();

        verify(audioSink, times(3)).process(any(AudioStream.class));
    }
}
