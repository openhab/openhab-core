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

import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.voice.text.HumanLanguageInterpreter;

/**
 * Describes dialog configured services and options.
 *
 * @author Miguel Álvarez - Initial contribution
 */
@NonNullByDefault
public class DialogContext {
    private final @Nullable KSService ks;
    private final @Nullable String keyword;
    private final STTService stt;
    private final TTSService tts;
    private final @Nullable Voice voice;
    private final List<HumanLanguageInterpreter> hlis;
    private final AudioSource source;
    private final AudioSink sink;
    private final Locale locale;
    private final @Nullable String listeningItem;
    private final @Nullable String listeningMelody;

    public DialogContext(@Nullable KSService ks, @Nullable String keyword, STTService stt, TTSService tts,
            @Nullable Voice voice, List<HumanLanguageInterpreter> hlis, AudioSource source, AudioSink sink,
            Locale locale, @Nullable String listeningItem, @Nullable String listeningMelody) {
        this.ks = ks;
        this.keyword = keyword;
        this.stt = stt;
        this.tts = tts;
        this.voice = voice;
        this.hlis = hlis;
        this.source = source;
        this.sink = sink;
        this.locale = locale;
        this.listeningItem = listeningItem;
        this.listeningMelody = listeningMelody;
    }

    public @Nullable KSService ks() {
        return ks;
    }

    public @Nullable String keyword() {
        return keyword;
    }

    public STTService stt() {
        return stt;
    }

    public TTSService tts() {
        return tts;
    }

    public @Nullable Voice voice() {
        return voice;
    }

    public List<HumanLanguageInterpreter> hlis() {
        return hlis;
    }

    public AudioSource source() {
        return source;
    }

    public AudioSink sink() {
        return sink;
    }

    public Locale locale() {
        return locale;
    }

    public @Nullable String listeningItem() {
        return listeningItem;
    }

    public @Nullable String listeningMelody() {
        return listeningMelody;
    }

    /**
     * @return a new DialogContext instance with the provided source
     */
    public DialogContext withSource(AudioSource source) {
        return new DialogContext(ks, keyword, stt, tts, voice, hlis, source, sink, locale, listeningItem,
                listeningMelody);
    }

    /**
     * @return a new DialogContext instance with the provided sink
     */
    public DialogContext withSink(AudioSink sink) {
        return new DialogContext(ks, keyword, stt, tts, voice, hlis, source, sink, locale, listeningItem,
                listeningMelody);
    }

    /**
     * @return a new DialogContext instance with the provided stt
     */
    public DialogContext withSTT(STTService stt) {
        return new DialogContext(ks, keyword, stt, tts, voice, hlis, source, sink, locale, listeningItem,
                listeningMelody);
    }

    /**
     * @return a new DialogContext instance with the provided tts
     */
    public DialogContext withTTS(TTSService tts) {
        return new DialogContext(ks, keyword, stt, tts, voice, hlis, source, sink, locale, listeningItem,
                listeningMelody);
    }

    /**
     * @return a new DialogContext instance with the provided voice
     */
    public DialogContext withVoice(Voice voice) {
        return new DialogContext(ks, keyword, stt, tts, voice, hlis, source, sink, locale, listeningItem,
                listeningMelody);
    }

    /**
     * @return a new DialogContext instance with the provided list of interpreters
     */
    public DialogContext withHLIs(List<HumanLanguageInterpreter> hlis) {
        return new DialogContext(ks, keyword, stt, tts, voice, hlis, source, sink, locale, listeningItem,
                listeningMelody);
    }

    /**
     * @return a new DialogContext instance with the provided locale
     */
    public DialogContext withLocale(Locale locale) {
        return new DialogContext(ks, keyword, stt, tts, voice, hlis, source, sink, locale, listeningItem,
                listeningMelody);
    }

    /**
     * @return a new DialogContext instance with the provided listening item
     */
    public DialogContext withListeningItem(String listeningItem) {
        return new DialogContext(ks, keyword, stt, tts, voice, hlis, source, sink, locale, listeningItem,
                listeningMelody);
    }

    /**
     * @return a new DialogContext instance with the provided listening melody
     */
    public DialogContext withListeningMelody(String listeningMelody) {
        return new DialogContext(ks, keyword, stt, tts, voice, hlis, source, sink, locale, listeningItem,
                listeningMelody);
    }

    /**
     * @return a new DialogContext instance with the provided ks
     */
    public DialogContext withKS(@Nullable KSService ks) {
        return new DialogContext(ks, keyword, stt, tts, voice, hlis, source, sink, locale, listeningItem,
                listeningMelody);
    }

    /**
     * @return a new DialogContext instance with the provided keyword
     */
    public DialogContext withKeyword(@Nullable String keyword) {
        return new DialogContext(ks, keyword, stt, tts, voice, hlis, source, sink, locale, listeningItem,
                listeningMelody);
    }
}
