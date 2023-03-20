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
package org.openhab.core.model.script.actions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.model.script.engine.action.ActionDoc;
import org.openhab.core.model.script.engine.action.ParamDoc;
import org.openhab.core.model.script.internal.engine.action.VoiceActionService;
import org.openhab.core.voice.KSService;
import org.openhab.core.voice.STTService;
import org.openhab.core.voice.TTSService;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.InterpretationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The static methods of this class are made available as functions in the scripts.
 * This allows a script to use voice features.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Christoph Weitkamp - Added parameter to adjust the volume
 */
@NonNullByDefault
public class Voice {

    private static final Logger logger = LoggerFactory.getLogger(Voice.class);

    /**
     * Says the given text.
     *
     * This method uses the default voice and the default audio sink to play the audio.
     *
     * @param text The text to speak
     */
    @ActionDoc(text = "says a given text with the default voice")
    public static void say(@ParamDoc(name = "text") Object text) {
        say(text, null, null, null);
    }

    /**
     * Says the given text with the given volume.
     *
     * This method uses the default voice and the default audio sink to play the audio.
     *
     * @param text The text to speak
     * @param volume The volume to be used
     */
    @ActionDoc(text = "says a given text with the default voice and the given volume")
    public static void say(@ParamDoc(name = "text") Object text,
            @ParamDoc(name = "volume", text = "the volume to be used") @Nullable PercentType volume) {
        say(text, null, null, volume);
    }

    @ActionDoc(text = "says a given text with the default voice and the given volume")
    public static void say(@ParamDoc(name = "text") Object text,
            @ParamDoc(name = "volume", text = "volume in the range [0;1]") float volume) {
        say(text, null, null, floatVolumeToPercentType(volume));
    }

    /**
     * Says the given text with a given voice.
     *
     * This method uses the default audio sink to play the audio.
     *
     * @param text The text to speak
     * @param voice The name of the voice to use or null, if the default voice should be used. If the voiceId is fully
     *            qualified (i.e. with a tts prefix), the according TTS service will be used, otherwise the
     *            voiceId is assumed to be available on the default TTS service.
     */
    @ActionDoc(text = "says a given text with a given voice")
    public static void say(@ParamDoc(name = "text") Object text, @ParamDoc(name = "voice") @Nullable String voice) {
        say(text, voice, null, null);
    }

    /**
     * Says the given text with a given voice and the given volume.
     *
     * This method uses the default audio sink to play the audio.
     *
     * @param text The text to speak
     * @param voice The name of the voice to use or null, if the default voice should be used. If the voiceId is fully
     *            qualified (i.e. with a tts prefix), the according TTS service will be used, otherwise the
     *            voiceId is assumed to be available on the default TTS service.
     * @param volume The volume to be used
     */
    @ActionDoc(text = "says a given text with a given voice and the given volume")
    public static void say(@ParamDoc(name = "text") Object text, @ParamDoc(name = "voice") @Nullable String voice,
            @ParamDoc(name = "volume", text = "the volume to be used") PercentType volume) {
        say(text, voice, null, volume);
    }

    @ActionDoc(text = "says a given text with a given voice and the given volume")
    public static void say(@ParamDoc(name = "text") Object text, @ParamDoc(name = "voice") @Nullable String voice,
            @ParamDoc(name = "volume", text = "volume in the range [0;1]") float volume) {
        say(text, voice, null, floatVolumeToPercentType(volume));
    }

    /**
     * Says the given text with a given voice through the given sink.
     *
     * @param text The text to speak
     * @param voice The name of the voice to use or null, if the default voice should be used. If the voiceId is fully
     *            qualified (i.e. with a tts prefix), the according TTS service will be used, otherwise the
     *            voiceId is assumed to be available on the default TTS service.
     * @param sink The name of audio sink to be used to play the audio or null, if the default sink should
     *            be used
     */
    @ActionDoc(text = "says a given text with a given voice through the given sink")
    public static void say(@ParamDoc(name = "text") Object text, @ParamDoc(name = "voice") @Nullable String voice,
            @ParamDoc(name = "sink") @Nullable String sink) {
        say(text, voice, sink, null);
    }

    /**
     * Says the given text with a given voice and the given volume through the given sink.
     *
     * @param text The text to speak
     * @param voice The name of the voice to use or null, if the default voice should be used. If the voiceId is fully
     *            qualified (i.e. with a tts prefix), the according TTS service will be used, otherwise the
     *            voiceId is assumed to be available on the default TTS service.
     * @param sink The name of audio sink to be used to play the audio or null, if the default sink should
     *            be used
     * @param volume The volume to be used
     */
    @ActionDoc(text = "says a given text with a given voice and the given volume through the given sink")
    public static void say(@ParamDoc(name = "text") Object text, @ParamDoc(name = "voice") @Nullable String voice,
            @ParamDoc(name = "sink") @Nullable String sink,
            @ParamDoc(name = "volume", text = "the volume to be used") @Nullable PercentType volume) {
        String output = text.toString();
        if (!output.isBlank()) {
            VoiceActionService.voiceManager.say(output, voice, sink, volume);
        }
    }

    @ActionDoc(text = "says a given text with a given voice and the given volume through the given sink")
    public static void say(@ParamDoc(name = "text") Object text, @ParamDoc(name = "voice") @Nullable String voice,
            @ParamDoc(name = "sink") @Nullable String sink,
            @ParamDoc(name = "volume", text = "volume in the range [0;1]") float volume) {
        say(text, voice, sink, floatVolumeToPercentType(volume));
    }

    /**
     * Interprets the given text.
     *
     * This method uses the default Human Language Interpreter and passes the text to it.
     * In case of interpretation error, the error message is played using the default audio sink.
     *
     * @param text The text to interpret
     */
    @ActionDoc(text = "interprets a given text by the default human language interpreter", returns = "human language response")
    public static String interpret(@ParamDoc(name = "text") Object text) {
        return interpret(text, null);
    }

    /**
     * Interprets the given text with a given Human Language Interpreter.
     *
     * In case of interpretation error, the error message is played using the default audio sink.
     *
     * @param text The text to interpret
     * @param interpreters Comma separated list of human language text interpreters to use
     */
    @ActionDoc(text = "interprets a given text by given human language interpreter(s)", returns = "human language response")
    public static String interpret(@ParamDoc(name = "text") Object text,
            @ParamDoc(name = "interpreters") @Nullable String interpreters) {
        String response;
        try {
            response = VoiceActionService.voiceManager.interpret(text.toString(), interpreters);
        } catch (InterpretationException e) {
            String message = Objects.requireNonNullElse(e.getMessage(), "");
            say(message);
            response = message;
        }
        return response;
    }

    /**
     * Interprets the given text with a given Human Language Interpreter.
     *
     * In case of interpretation error, the error message is played using the given audio sink.
     * If sink parameter is null, the error message is simply not played.
     *
     * @param text The text to interpret
     * @param interpreters Comma separated list of human language text interpreters to use
     * @param sink The name of audio sink to be used to play the error message
     */
    @ActionDoc(text = "interprets a given text by given human language interpreter(s) and using the given sink", returns = "human language response")
    public static String interpret(@ParamDoc(name = "text") Object text,
            @ParamDoc(name = "interpreters") String interpreters, @ParamDoc(name = "sink") @Nullable String sink) {
        String response;
        try {
            response = VoiceActionService.voiceManager.interpret(text.toString(), interpreters);
        } catch (InterpretationException e) {
            String message = Objects.requireNonNullElse(e.getMessage(), "");
            if (sink != null) {
                say(message, null, sink);
            }
            response = message;
        }
        return response;
    }

    /**
     * Starts dialog processing for a given audio source using default keyword spotting service, default speech-to-text
     * service, default text-to-speech service and default human language text interpreter.
     *
     * @param source the name of audio source to use or null to use the default source
     * @param sink the name of audio sink to use or null to use the default sink
     */
    @ActionDoc(text = "starts dialog processing for a given audio source")
    public static void startDialog(@ParamDoc(name = "source") @Nullable String source,
            @ParamDoc(name = "sink") @Nullable String sink) {
        startDialog(null, null, null, null, null, source, sink, null, null, null);
    }

    /**
     * Starts dialog processing for a given audio source.
     *
     * @param ks the keyword spotting service to use or null to use the default service
     * @param stt the speech-to-text service to use or null to use the default service
     * @param tts the text-to-speech service to use or null to use the default service
     * @param voice the voice to use or null to use the default voice or any voice provided by the text-to-speech
     *            service matching the locale
     * @param interpreters comma separated list of human language text interpreters to use or null to use the default
     *            service
     * @param source the name of audio source to use or null to use the default source
     * @param sink the name of audio sink to use or null to use the default sink
     * @param locale the locale to use or null to use the default locale
     * @param keyword the keyword to use during keyword spotting or null to use the default keyword
     * @param listeningItem the item to switch ON while listening to a question
     */
    @ActionDoc(text = "starts dialog processing for a given audio source")
    public static void startDialog(@ParamDoc(name = "keyword spotting service") @Nullable String ks,
            @ParamDoc(name = "speech-to-text service") @Nullable String stt,
            @ParamDoc(name = "text-to-speech service") @Nullable String tts,
            @ParamDoc(name = "voice") @Nullable String voice,
            @ParamDoc(name = "interpreters") @Nullable String interpreters,
            @ParamDoc(name = "source") @Nullable String source, @ParamDoc(name = "sink") @Nullable String sink,
            @ParamDoc(name = "locale") @Nullable String locale, @ParamDoc(name = "keyword") @Nullable String keyword,
            @ParamDoc(name = "listening item") @Nullable String listeningItem) {
        var dialogContextBuilder = VoiceActionService.voiceManager.getDialogContextBuilder();
        if (source != null) {
            AudioSource audioSource = VoiceActionService.audioManager.getSource(source);
            if (audioSource == null) {
                logger.warn("Failed starting dialog processing: audio source '{}' not found", source);
                return;
            }
            dialogContextBuilder.withSource(audioSource);
        }
        if (ks != null) {
            KSService ksService = VoiceActionService.voiceManager.getKS(ks);
            if (ksService == null) {
                logger.warn("Failed starting dialog processing: keyword spotting service '{}' not found", ks);
                return;
            }
            dialogContextBuilder.withKS(ksService);
        }
        if (keyword != null) {
            dialogContextBuilder.withKeyword(keyword);
        }
        if (stt != null) {
            STTService sttService = VoiceActionService.voiceManager.getSTT(stt);
            if (sttService == null) {
                logger.warn("Failed starting dialog processing: speech-to-text service '{}' not found", stt);
                return;
            }
            dialogContextBuilder.withSTT(sttService);
        }
        if (tts != null) {
            TTSService ttsService = VoiceActionService.voiceManager.getTTS(tts);
            if (ttsService == null) {
                logger.warn("Failed starting dialog processing: text-to-speech service '{}' not found", tts);
                return;
            }
            dialogContextBuilder.withTTS(ttsService);
        }
        if (voice != null) {
            org.openhab.core.voice.Voice prefVoice = getVoice(voice);
            if (prefVoice == null) {
                logger.warn("Failed starting dialog processing: voice '{}' not found", voice);
                return;
            }
            dialogContextBuilder.withVoice(prefVoice);
        }
        if (interpreters != null) {
            List<HumanLanguageInterpreter> hliServices = VoiceActionService.voiceManager.getHLIsByIds(interpreters);
            if (hliServices.isEmpty()) {
                logger.warn("Failed starting dialog processing: interpreters '{}' not found", interpreters);
                return;
            }
            dialogContextBuilder.withHLIs(hliServices);
        }
        if (sink != null) {
            AudioSink audioSink = VoiceActionService.audioManager.getSink(sink);
            if (audioSink == null) {
                logger.warn("Failed starting dialog processing: audio sink '{}' not found", sink);
                return;
            }
            dialogContextBuilder.withSink(audioSink);
        }
        if (listeningItem != null) {
            dialogContextBuilder.withListeningItem(listeningItem);
        }
        if (locale != null) {
            String[] split = locale.split("-");
            Locale loc = null;
            if (split.length == 2) {
                loc = new Locale(split[0], split[1]);
            } else {
                loc = new Locale(split[0]);
            }
            dialogContextBuilder.withLocale(loc);
        }

        try {
            VoiceActionService.voiceManager.startDialog(dialogContextBuilder.build());
        } catch (IllegalStateException e) {
            logger.warn("Failed starting dialog processing: {}", e.getMessage());
        }
    }

    /**
     * Stops dialog processing for a given audio source.
     *
     * @param source the name of audio source or null to consider the default audio source
     */
    @ActionDoc(text = "stops dialog processing for a given audio source")
    public static void stopDialog(@ParamDoc(name = "source") @Nullable String source) {
        try {
            AudioSource audioSource = null;
            if (source != null) {
                audioSource = VoiceActionService.audioManager.getSource(source);
                if (audioSource == null) {
                    logger.warn("Failed stopping dialog processing: audio source '{}' not found", source);
                    return;
                }
            }
            VoiceActionService.voiceManager.stopDialog(audioSource);
        } catch (IllegalStateException e) {
            logger.warn("Failed stopping dialog processing: {}", e.getMessage());
        }
    }

    /**
     * Executes a simple dialog sequence without keyword spotting for a given audio source using default speech-to-text
     * service, default text-to-speech service, default human language text interpreter and default locale.
     *
     * @param source the name of audio source to use or null to use the default source
     * @param sink the name of audio sink to use or null to use the default sink
     */
    @ActionDoc(text = "executes a simple dialog sequence without keyword spotting for a given audio source")
    public static void listenAndAnswer(@ParamDoc(name = "source") @Nullable String source,
            @ParamDoc(name = "sink") @Nullable String sink) {
        listenAndAnswer(null, null, null, null, source, sink, null, null);
    }

    /**
     * Executes a simple dialog sequence without keyword spotting for a given audio source.
     *
     * @param stt the speech-to-text service to use or null to use the default service
     * @param tts the text-to-speech service to use or null to use the default service
     * @param voice the voice to use or null to use the default voice or any voice provided by the text-to-speech
     *            service matching the locale
     * @param interpreters comma separated list of human language text interpreters to use or null to use the default
     *            service
     * @param source the name of audio source to use or null to use the default source
     * @param sink the name of audio sink to use or null to use the default sink
     * @param locale the locale to use or null to use the default locale
     * @param listeningItem the item to switch ON while listening to a question
     */
    @ActionDoc(text = "executes a simple dialog sequence without keyword spotting for a given audio source")
    public static void listenAndAnswer(@ParamDoc(name = "speech-to-text service") @Nullable String stt,
            @ParamDoc(name = "text-to-speech service") @Nullable String tts,
            @ParamDoc(name = "voice") @Nullable String voice,
            @ParamDoc(name = "interpreters") @Nullable String interpreters,
            @ParamDoc(name = "source") @Nullable String source, @ParamDoc(name = "sink") @Nullable String sink,
            @ParamDoc(name = "locale") @Nullable String locale,
            @ParamDoc(name = "listening item") @Nullable String listeningItem) {
        try {
            var dialogContextBuilder = VoiceActionService.voiceManager.getDialogContextBuilder();
            if (source != null) {
                AudioSource audioSource = VoiceActionService.audioManager.getSource(source);
                if (audioSource == null) {
                    logger.warn("Failed executing simple dialog: audio source '{}' not found", source);
                    return;
                }
                dialogContextBuilder.withSource(audioSource);
            }
            if (stt != null) {
                STTService sttService = VoiceActionService.voiceManager.getSTT(stt);
                if (sttService == null) {
                    logger.warn("Failed executing simple dialog: speech-to-text service '{}' not found", stt);
                    return;
                }
                dialogContextBuilder.withSTT(sttService);
            }
            if (tts != null) {
                TTSService ttsService = VoiceActionService.voiceManager.getTTS(tts);
                if (ttsService == null) {
                    logger.warn("Failed executing simple dialog: text-to-speech service '{}' not found", tts);
                    return;
                }
                dialogContextBuilder.withTTS(ttsService);
            }
            if (voice != null) {
                org.openhab.core.voice.Voice prefVoice = getVoice(voice);
                if (prefVoice == null) {
                    logger.warn("Failed executing simple dialog: voice '{}' not found", voice);
                    return;
                }
                dialogContextBuilder.withVoice(prefVoice);
            }
            if (interpreters != null) {
                List<HumanLanguageInterpreter> hliServices = VoiceActionService.voiceManager.getHLIsByIds(interpreters);
                if (hliServices.isEmpty()) {
                    logger.warn("Failed executing simple dialog: interpreters '{}' not found", interpreters);
                    return;
                }
                dialogContextBuilder.withHLIs(hliServices);
            }
            if (sink != null) {
                AudioSink audioSink = VoiceActionService.audioManager.getSink(sink);
                if (audioSink == null) {
                    logger.warn("Failed executing simple dialog: audio sink '{}' not found", sink);
                    return;
                }
                dialogContextBuilder.withSink(audioSink);
            }
            if (listeningItem != null) {
                dialogContextBuilder.withListeningItem(listeningItem);
            }
            if (locale != null) {
                Locale loc = null;
                String[] split = locale.split("-");
                if (split.length == 2) {
                    loc = new Locale(split[0], split[1]);
                } else {
                    loc = new Locale(split[0]);
                }
                dialogContextBuilder.withLocale(loc);
            }
            VoiceActionService.voiceManager.listenAndAnswer(dialogContextBuilder.build());
        } catch (IllegalStateException e) {
            logger.warn("Failed executing simple dialog: {}", e.getMessage());
        }
    }

    private static org.openhab.core.voice.@Nullable Voice getVoice(String id) {
        return VoiceActionService.voiceManager.getAllVoices().stream().filter(voice -> voice.getUID().equals(id))
                .findAny().orElse(null);
    }

    /**
     * Converts a float volume to a {@link PercentType} volume and checks if float volume is in the [0;1] range.
     * 
     * @param volume
     * @return
     */
    private static PercentType floatVolumeToPercentType(float volume) {
        if (volume < 0 || volume > 1) {
            throw new IllegalArgumentException("Volume value must be in the range [0;1]!");
        }
        return new PercentType(new BigDecimal(volume * 100f));
    }
}
