/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.voice.text.HumanLanguageInterpreter;

import java.util.List;
import java.util.Locale;

/**
 * Describes dialog configured services and options.
 *
 * @author Miguel Álvarez - Initial contribution
 */
@NonNullByDefault
public record DialogContext(@Nullable KSService ks, @Nullable String keyword, STTService stt, TTSService tts,
                            @Nullable Voice voice,
                            List<HumanLanguageInterpreter> hlis, AudioSource source, AudioSink sink,
                            Locale locale, @Nullable String listeningItem) {
    /**
     *  @return a new DialogContext instance with the provided option
     */
    public DialogContext withSource(AudioSource source) {
        return new DialogContext(ks, keyword, stt, tts, voice, hlis, source, sink, locale, listeningItem);
    }

    /**
     *  @return a new DialogContext instance with the provided option 
     */
    public DialogContext withSink(AudioSink sink) {
        return new DialogContext(ks, keyword, stt, tts, voice, hlis, source, sink, locale, listeningItem);
    }

    /**
     *  @return a new DialogContext instance with the provided option 
     */
    public DialogContext withSTT(STTService stt) {
        return new DialogContext(ks, keyword, stt, tts, voice, hlis, source, sink, locale, listeningItem);
    }

    /**
     *  @return a new DialogContext instance with the provided option 
     */
    public DialogContext withTTS(TTSService tts) {
        return new DialogContext(ks, keyword, stt, tts, voice, hlis, source, sink, locale, listeningItem);
    }

    /**
     *  @return a new DialogContext instance with the provided option 
     */
    public DialogContext withVoice(Voice voice) {
        return new DialogContext(ks, keyword, stt, tts, voice, hlis, source, sink, locale, listeningItem);
    }

    /**
     *  @return a new DialogContext instance with the provided option 
     */
    public DialogContext withHLIs(List<HumanLanguageInterpreter> hlis) {
        return new DialogContext(ks, keyword, stt, tts, voice, hlis, source, sink, locale, listeningItem);
    }

    /**
     *  @return a new DialogContext instance with the provided option 
     */
    public DialogContext withLocale(Locale locale) {
        return new DialogContext(ks, keyword, stt, tts, voice, hlis, source, sink, locale, listeningItem);
    }

    /**
     *  @return a new DialogContext instance with the provided option 
     */
    public DialogContext withListeningItem(String listeningItem) {
        return new DialogContext(ks, keyword, stt, tts, voice, hlis, source, sink, locale, listeningItem);
    }

    /**
     *  @return a new DialogContext instance with the provided option 
     */
    public DialogContext withKS(@Nullable KSService ks) {
        return new DialogContext(ks, keyword, stt, tts, voice, hlis, source, sink, locale, listeningItem);
    }

    /**
     *  @return a new DialogContext instance with the provided option 
     */
    public DialogContext withKeyword(@Nullable String keyword) {
        return new DialogContext(ks, keyword, stt, tts, voice, hlis, source, sink, locale, listeningItem);
    }
}
