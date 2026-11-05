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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.voice.VoiceManager;
import org.openhab.core.voice.dialog.DialogContext;
import org.openhab.core.voice.dialog.DialogRegistration;
import org.openhab.core.voice.dialog.trigger.DTServiceHandle;
import org.openhab.core.voice.dialog.trigger.ks.KSService;
import org.openhab.core.voice.internal.dialog.DialogManager;
import org.openhab.core.voice.internal.stt.AudioTranscriber;
import org.openhab.core.voice.internal.text.InterpretationService;
import org.openhab.core.voice.internal.tts.SpeechSynthesizer;
import org.openhab.core.voice.stt.STTService;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.InterpretationArguments;
import org.openhab.core.voice.text.InterpretationException;
import org.openhab.core.voice.text.conversation.ConversationManager;
import org.openhab.core.voice.text.interpreter.llm.LLMToolRegistry;
import org.openhab.core.voice.tts.TTSException;
import org.openhab.core.voice.tts.TTSService;
import org.openhab.core.voice.tts.Voice;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service provides functionality around voice services and is the central service to be used directly by others.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Yannick Schaus - Added ability to provide an item for feedback during listening phases
 * @author Christoph Weitkamp - Added getSupportedStreams() and UnsupportedAudioStreamException
 * @author Christoph Weitkamp - Added parameter to adjust the volume
 * @author Wouter Born - Sort TTS options
 * @author Laurent Garnier - Updated methods startDialog and added method stopDialog
 * @author Miguel Álvarez - Use dialog context
 * @author Miguel Álvarez - Add transcribe method
 * @author Florian Hotze - Extract OSGi service tracking into
 *         {@link org.openhab.core.common.registry.AbstractServiceRegistry}s; Extract shared functionality into
 *         dedicated services; Extract dialog lifecycle and persistence into {@link DialogManager}
 */
@Component(immediate = true, configurationPid = VoiceConfigurationConstants.CONFIGURATION_PID, //
        property = Constants.SERVICE_PID + "=org.openhab.voice")
@ConfigurableService(category = "system", label = "Voice", description_uri = VoiceConfigurationConstants.CONFIG_URI)
@NonNullByDefault
public class VoiceManagerImpl implements VoiceManager {
    private final Logger logger = LoggerFactory.getLogger(VoiceManagerImpl.class);

    private final LocaleProvider localeProvider;
    private final AudioManager audioManager;
    private final LLMToolRegistry llmToolRegistry;
    private final ConversationManager conversationManager;
    private final AudioTranscriber audioTranscriber;
    private final InterpretationService interpretationService;
    private final SpeechSynthesizer speechSynthesizer;
    private final DialogManager dialogManager;
    private final VoiceServiceResolver voiceServiceResolver;

    @Activate
    public VoiceManagerImpl(final @Reference LocaleProvider localeProvider, final @Reference AudioManager audioManager,
            final @Reference LLMToolRegistry llmToolRegistry, final @Reference ConversationManager conversationManager,
            final @Reference AudioTranscriber audioTranscriber,
            final @Reference InterpretationService interpretationService,
            final @Reference SpeechSynthesizer speechSynthesizer, final @Reference DialogManager dialogManager,
            final @Reference VoiceServiceResolver voiceServiceResolver) {
        this.localeProvider = localeProvider;
        this.audioManager = audioManager;
        this.conversationManager = conversationManager;
        this.llmToolRegistry = llmToolRegistry;
        this.audioTranscriber = audioTranscriber;
        this.interpretationService = interpretationService;
        this.speechSynthesizer = speechSynthesizer;
        this.dialogManager = dialogManager;
        this.voiceServiceResolver = voiceServiceResolver;
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        modified(config);
    }

    @Modified
    protected void modified(Map<String, Object> config) {
    }

    @Override
    public void say(String text) {
        say(text, null, null, null);
    }

    @Override
    public void say(String text, @Nullable PercentType volume) {
        say(text, null, null, volume);
    }

    @Override
    public void say(String text, String voiceId) {
        say(text, voiceId, null, null);
    }

    @Override
    public void say(String text, @Nullable String voiceId, @Nullable PercentType volume) {
        say(text, voiceId, null, volume);
    }

    @Override
    public void say(String text, @Nullable String voiceId, @Nullable String sinkId) {
        say(text, voiceId, sinkId, null);
    }

    @Override
    public void say(String text, @Nullable String voiceId, @Nullable String sinkId, @Nullable PercentType volume) {
        Objects.requireNonNull(text, "Text cannot be said as it is null.");

        try {
            TTSService tts = null;
            Voice voice;
            if (voiceId != null) {
                voice = voiceServiceResolver.getVoice(voiceId);
                if (voice != null) {
                    tts = voiceServiceResolver.getTTS(voice);
                }
            } else {
                voice = voiceServiceResolver.getDefaultVoice();
                if (voice != null) {
                    tts = voiceServiceResolver.getTTS(voice);
                } else {
                    tts = voiceServiceResolver.getTTS();
                    if (tts != null) {
                        voice = voiceServiceResolver.getPreferredVoice(tts.getAvailableVoices());
                    }
                }
            }
            if (tts == null) {
                throw new TTSException("No TTS service can be found for voice " + voiceId);
            }
            if (voice == null) {
                throw new TTSException(
                        "Unable to find a voice for language " + localeProvider.getLocale().getLanguage());
            }
            AudioSink sink = audioManager.getSink(sinkId);
            if (sink == null) {
                throw new TTSException("Unable to find the audio sink " + sinkId);
            }

            speechSynthesizer.say(text, tts, voice, sink, volume);
        } catch (TTSException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error saying '{}': {}", text, e.getMessage(), e);
            } else {
                logger.warn("Error saying '{}': {}", text, e.getMessage());
            }
        }
    }

    @Override
    public String transcribe(@Nullable String audioSourceId, @Nullable String sttId, @Nullable Locale locale) {
        var audioSource = audioSourceId != null ? audioManager.getSource(audioSourceId) : audioManager.getSource();
        if (audioSource == null) {
            logger.warn("Audio source '{}' not available", audioSourceId != null ? audioSourceId : "default");
            return "";
        }
        var sttService = sttId != null ? voiceServiceResolver.getSTT(sttId) : voiceServiceResolver.getSTT();
        if (sttService == null) {
            logger.warn("Speech-to-text service '{}' not available", sttId != null ? sttId : "default");
            return "";
        }
        return audioTranscriber.transcribe(audioSource, sttService, locale);
    }

    @Override
    public String transcribe(AudioStream audioStream, @Nullable String sttId, @Nullable Locale locale) {
        var sttService = sttId != null ? voiceServiceResolver.getSTT(sttId) : voiceServiceResolver.getSTT();
        if (sttService == null) {
            logger.warn("Speech-to-text service '{}' not available", sttId != null ? sttId : "default");
            return "";
        }
        return audioTranscriber.transcribe(audioStream, sttService, locale);
    }

    @Override
    public String interpret(String text) throws InterpretationException {
        return interpret(text, (InterpretationArguments) null);
    }

    @Override
    public String interpret(String text, @Nullable String hliIdList) throws InterpretationException {
        return interpret(text, hliIdList == null ? null : new InterpretationArguments(hliIdList, "", "*", "", null));
    }

    @Override
    public String interpret(String text, @Nullable InterpretationArguments args) throws InterpretationException {
        InterpretationArguments nullSafeArgs = args != null ? args : new InterpretationArguments("", "", "*", "", null);
        List<HumanLanguageInterpreter> interpreters = new ArrayList<>();
        if (nullSafeArgs.hliIdList().isEmpty()) {
            HumanLanguageInterpreter interpreter = voiceServiceResolver.getHLI();
            if (interpreter != null) {
                interpreters.add(interpreter);
            }
        } else {
            interpreters = voiceServiceResolver.getHLIsByIds(nullSafeArgs.hliIdList());
        }

        if (!interpreters.isEmpty()) {
            Locale locale = nullSafeArgs.locale();
            if (locale == null) {
                locale = localeProvider.getLocale();
            }
            String locationItem = null;
            if (nullSafeArgs.locationItem() != null && !nullSafeArgs.locationItem().isBlank()) {
                locationItem = nullSafeArgs.locationItem();
            }
            return interpretationService.interpret(text, interpreters,
                    conversationManager.getConversation(nullSafeArgs.conversationId()),
                    llmToolRegistry.getByIds(nullSafeArgs.toolIdList()), locale, locationItem,
                    voiceServiceResolver.getConfiguration().getSystemPrompt());
        }

        if (nullSafeArgs.hliIdList().isEmpty()) {
            throw new InterpretationException("No human language interpreter available!");
        } else {
            throw new InterpretationException(
                    "No human language interpreter can be found for " + nullSafeArgs.hliIdList());
        }
    }

    @Override
    public @Nullable Voice getPreferredVoice(Set<Voice> voices) {
        return voiceServiceResolver.getPreferredVoice(voices);
    }

    @Override
    public DialogContext.Builder getDialogContextBuilder() {
        return dialogManager.getDialogContextBuilder();
    }

    @Override
    public List<DialogContext> getDialogsContexts() {
        return dialogManager.getDialogsContexts();
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nullable DialogContext getLastDialogContext() {
        return dialogManager.getLastDialogContext();
    }

    @Override
    public @Nullable DTServiceHandle startDialog(DialogContext context) throws IllegalStateException {
        return dialogManager.startDialog(context);
    }

    @Override
    public void stopDialog(@Nullable AudioSource source) throws IllegalStateException {
        dialogManager.stopDialog(source);
    }

    @Override
    public void stopDialog(DialogContext context) throws IllegalStateException {
        dialogManager.stopDialog(context);
    }

    @Override
    public void listenAndAnswer(DialogContext context) throws IllegalStateException {
        dialogManager.listenAndAnswer(context);
    }

    @Override
    public void registerDialog(DialogRegistration registration) throws IllegalStateException {
        dialogManager.registerDialog(registration);
    }

    @Override
    public void unregisterDialog(DialogRegistration registration) {
        dialogManager.unregisterDialog(registration);
    }

    @Override
    public void unregisterDialog(String sourceId) {
        dialogManager.unregisterDialog(sourceId);
    }

    @Override
    public List<DialogRegistration> getDialogRegistrations() {
        return dialogManager.getDialogRegistrations();
    }

    @Override
    public @Nullable TTSService getTTS() {
        return voiceServiceResolver.getTTS();
    }

    @Override
    public @Nullable TTSService getTTS(@Nullable String id) {
        return voiceServiceResolver.getTTS(id);
    }

    @Override
    public Collection<TTSService> getTTSs() {
        return voiceServiceResolver.getTTSs();
    }

    @Override
    public @Nullable STTService getSTT() {
        return voiceServiceResolver.getSTT();
    }

    @Override
    public @Nullable STTService getSTT(@Nullable String id) {
        return voiceServiceResolver.getSTT(id);
    }

    @Override
    public Collection<STTService> getSTTs() {
        return voiceServiceResolver.getSTTs();
    }

    @Override
    public @Nullable KSService getKS() {
        return voiceServiceResolver.getKS();
    }

    @Override
    public @Nullable KSService getKS(@Nullable String id) {
        return voiceServiceResolver.getKS(id);
    }

    @Override
    public Collection<KSService> getKSs() {
        return voiceServiceResolver.getKSs();
    }

    @Override
    public @Nullable HumanLanguageInterpreter getHLI() {
        return voiceServiceResolver.getHLI();
    }

    @Override
    public @Nullable HumanLanguageInterpreter getHLI(@Nullable String id) {
        return voiceServiceResolver.getHLI(id);
    }

    @Override
    public List<HumanLanguageInterpreter> getHLIsByIds(@Nullable String ids) {
        return voiceServiceResolver.getHLIsByIds(ids);
    }

    @Override
    public List<HumanLanguageInterpreter> getHLIsByIds(List<String> ids) {
        return voiceServiceResolver.getHLIsByIds(ids);
    }

    @Override
    public Collection<HumanLanguageInterpreter> getHLIs() {
        return voiceServiceResolver.getHLIs();
    }

    @Override
    public Set<Voice> getAllVoices() {
        return voiceServiceResolver.getAllVoices();
    }

    @Override
    public @Nullable Voice getDefaultVoice() {
        return voiceServiceResolver.getDefaultVoice();
    }
}
