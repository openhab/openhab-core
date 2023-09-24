/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
import java.util.Collection;
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
public record DialogContext(@Nullable KSService ks, @Nullable String keyword, STTService stt, TTSService tts,
        @Nullable Voice voice, List<HumanLanguageInterpreter> hlis, AudioSource source, AudioSink sink, Locale locale,
        String dialogGroup, @Nullable String locationItem, @Nullable String listeningItem,
        @Nullable String listeningMelody) {

    /**
     * Builder for {@link DialogContext}
     * Allows to describe a dialog context without requiring the involved services to be loaded
     */
    public static class Builder {
        // services
        private @Nullable AudioSource source;
        private @Nullable AudioSink sink;
        private @Nullable KSService ks;
        private @Nullable STTService stt;
        private @Nullable TTSService tts;
        private @Nullable Voice voice;
        private List<HumanLanguageInterpreter> hlis = List.of();
        // options
        private String dialogGroup = "default";
        private @Nullable String locationItem;
        private @Nullable String listeningItem;
        private @Nullable String listeningMelody;
        private String keyword;
        private Locale locale;

        public Builder(String keyword, Locale locale) {
            this.keyword = keyword;
            this.locale = locale;
        }

        public Builder withSource(@Nullable AudioSource source) {
            this.source = source;
            return this;
        }

        public Builder withSink(@Nullable AudioSink sink) {
            this.sink = sink;
            return this;
        }

        public Builder withKS(@Nullable KSService service) {
            if (service != null) {
                this.ks = service;
            }
            return this;
        }

        public Builder withSTT(@Nullable STTService service) {
            if (service != null) {
                this.stt = service;
            }
            return this;
        }

        public Builder withTTS(@Nullable TTSService service) {
            if (service != null) {
                this.tts = service;
            }
            return this;
        }

        public Builder withHLI(@Nullable HumanLanguageInterpreter service) {
            if (service != null) {
                this.hlis = List.of(service);
            }
            return this;
        }

        public Builder withHLIs(Collection<HumanLanguageInterpreter> services) {
            return withHLIs(new ArrayList<>(services));
        }

        public Builder withHLIs(List<HumanLanguageInterpreter> services) {
            if (!services.isEmpty()) {
                this.hlis = services;
            }
            return this;
        }

        public Builder withKeyword(@Nullable String keyword) {
            if (keyword != null && !keyword.isBlank()) {
                this.keyword = keyword;
            }
            return this;
        }

        public Builder withVoice(@Nullable Voice voice) {
            if (voice != null) {
                this.voice = voice;
            }
            return this;
        }

        public Builder withDialogGroup(@Nullable String dialogGroup) {
            if (dialogGroup != null) {
                this.dialogGroup = dialogGroup;
            }
            return this;
        }

        public Builder withLocationItem(@Nullable String locationItem) {
            if (locationItem != null) {
                this.locationItem = locationItem;
            }
            return this;
        }

        public Builder withListeningItem(@Nullable String listeningItem) {
            if (listeningItem != null) {
                this.listeningItem = listeningItem;
            }
            return this;
        }

        public Builder withMelody(@Nullable String listeningMelody) {
            if (listeningMelody != null) {
                this.listeningMelody = listeningMelody;
            }
            return this;
        }

        public Builder withLocale(@Nullable Locale locale) {
            if (locale != null) {
                this.locale = locale;
            }
            return this;
        }

        /**
         * Creates a new {@link DialogContext}
         *
         * @return a {@link DialogContext} with the configured components and options
         * @throws IllegalStateException if a required dialog component is missing
         */
        public DialogContext build() throws IllegalStateException {
            KSService ksService = ks;
            STTService sttService = stt;
            TTSService ttsService = tts;
            List<HumanLanguageInterpreter> hliServices = hlis;
            AudioSource audioSource = source;
            AudioSink audioSink = sink;
            if (sttService == null || ttsService == null || hliServices.isEmpty() || audioSource == null
                    || audioSink == null) {
                List<String> errors = new ArrayList<>();
                if (sttService == null) {
                    errors.add("missing stt service");
                }
                if (ttsService == null) {
                    errors.add("missing tts service");
                }
                if (hliServices.isEmpty()) {
                    errors.add("missing interpreters");
                }
                if (audioSource == null) {
                    errors.add("missing audio source");
                }
                if (audioSink == null) {
                    errors.add("missing audio sink");
                }
                throw new IllegalStateException("Cannot build dialog context: " + String.join(", ", errors) + ".");
            } else {
                return new DialogContext(ksService, keyword, sttService, ttsService, voice, hliServices, audioSource,
                        audioSink, locale, dialogGroup, locationItem, listeningItem, listeningMelody);
            }
        }
    }
}
