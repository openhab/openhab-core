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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public DialogContext(@Nullable KSService ks, @Nullable String keyword, STTService stt, TTSService tts,
            @Nullable Voice voice, List<HumanLanguageInterpreter> hlis, AudioSource source, AudioSink sink,
            Locale locale, @Nullable String listeningItem) {
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

    /**
     * Builder for {@link DialogContext}
     * Allows to describe a dialog context without requiring the involved services to be loaded
     */
    public static class Builder {
        private final Logger logger = LoggerFactory.getLogger(Builder.class);
        // services
        private @Nullable String sourceId = null;
        private Supplier<@Nullable AudioSource> source = () -> null;
        private @Nullable String sinkId = null;
        private Supplier<@Nullable AudioSink> sink = () -> null;
        private @Nullable String ksId = null;
        private Supplier<@Nullable KSService> ks = () -> null;
        private @Nullable String sttId = null;
        private Supplier<@Nullable STTService> stt = () -> null;
        private @Nullable String ttsId = null;
        private Supplier<@Nullable TTSService> tts = () -> null;
        private @Nullable Voice voice = null;
        private List<String> hliIds = List.of();
        private Supplier<List<HumanLanguageInterpreter>> hlis = List::of;
        // options
        private @Nullable String listeningItem = null;
        private String keyword;
        private Locale locale;

        public Builder(String keyword, Locale locale) {
            this.keyword = keyword;
            this.locale = locale;
        }

        public Builder withSource(AudioSource service) {
            withSource(service.getId(), () -> service);
            return this;
        }

        public Builder withSource(String id, Supplier<@Nullable AudioSource> serviceResolver) {
            sourceId = id;
            source = serviceResolver;
            return this;
        }

        public Builder withSink(AudioSink service) {
            withSink(service.getId(), () -> service);
            return this;
        }

        public Builder withSink(String id, Supplier<@Nullable AudioSink> serviceResolver) {
            sinkId = id;
            sink = serviceResolver;
            return this;
        }

        public Builder withKS(KSService service) {
            withKS(service.getId(), () -> service);
            return this;
        }

        public Builder withKS(String id, Supplier<@Nullable KSService> serviceResolver) {
            ksId = id;
            ks = serviceResolver;
            return this;
        }

        public Builder withSTT(STTService service) {
            withSTT(service.getId(), () -> service);
            return this;
        }

        public Builder withSTT(String id, Supplier<@Nullable STTService> serviceResolver) {
            sttId = id;
            stt = serviceResolver;
            return this;
        }

        public Builder withTTS(TTSService service) {
            withTTS(service.getId(), () -> service);
            return this;
        }

        public Builder withTTS(String id, Supplier<@Nullable TTSService> serviceResolver) {
            ttsId = id;
            tts = serviceResolver;
            return this;
        }

        public Builder withHLIs(List<HumanLanguageInterpreter> services) {
            withHLIs(services.stream().map(HumanLanguageInterpreter::getId).collect(Collectors.toList()),
                    () -> services);
            return this;
        }

        public Builder withHLIs(List<String> ids, Supplier<List<HumanLanguageInterpreter>> serviceResolver) {
            hliIds = ids;
            hlis = serviceResolver;
            return this;
        }

        public Builder withKeyword(String keyword) {
            this.keyword = keyword;
            return this;
        }

        public Builder withVoice(Voice voice) {
            this.voice = voice;
            return this;
        }

        public Builder withListeningItem(String listeningItem) {
            this.listeningItem = listeningItem;
            return this;
        }

        public Builder withLocale(Locale locale) {
            this.locale = locale;
            return this;
        }

        public DialogContext build() throws IllegalStateException {
            KSService ksService = ks.get();
            STTService sttService = stt.get();
            TTSService ttsService = tts.get();
            List<HumanLanguageInterpreter> hliServices = hlis.get();
            AudioSource audioSource = source.get();
            AudioSink audioSink = sink.get();
            List<String> errors = new ArrayList<>();
            if (sttService == null) {
                errors.add("Missing stt service: " + sttId);
            }
            if (ttsService == null) {
                errors.add("Missing tts service: " + ttsId);
            }
            if (hliServices.isEmpty() || hliIds.size() != hliServices.size()) {
                var serviceIds = hliServices.stream().map(HumanLanguageInterpreter::getId).collect(Collectors.toList());
                errors.add("Missing interpreters: "
                        + hliIds.stream().filter(s -> !serviceIds.contains(s)).collect(Collectors.joining(", ")));
            }
            if (audioSource == null) {
                errors.add("Missing audio source: " + sourceId);
            }
            if (audioSink == null) {
                errors.add("Missing audio sink: " + sinkId);
            }
            if (!errors.isEmpty()) {
                errors.forEach(logger::warn);
                throw new IllegalStateException("Cannot build dialog context, services are missing");
            }
            return new DialogContext(ksService, keyword, sttService, ttsService, voice, hliServices, audioSource,
                    audioSink, locale, listeningItem);
        }
    }
}
