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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigOptionProvider;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.voice.VoiceManager;
import org.openhab.core.voice.dialog.DialogContext;
import org.openhab.core.voice.dialog.DialogRegistration;
import org.openhab.core.voice.dialog.trigger.DTServiceHandle;
import org.openhab.core.voice.dialog.trigger.ks.KSService;
import org.openhab.core.voice.dialog.trigger.ks.KSServiceRegistry;
import org.openhab.core.voice.internal.dialog.DialogProcessor;
import org.openhab.core.voice.internal.stt.AudioTranscriber;
import org.openhab.core.voice.internal.text.InterpretationService;
import org.openhab.core.voice.internal.tts.SpeechSynthesizer;
import org.openhab.core.voice.stt.STTService;
import org.openhab.core.voice.stt.STTServiceRegistry;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.HumanLanguageInterpreterRegistry;
import org.openhab.core.voice.text.InterpretationArguments;
import org.openhab.core.voice.text.InterpretationException;
import org.openhab.core.voice.text.conversation.Conversation;
import org.openhab.core.voice.text.conversation.ConversationManager;
import org.openhab.core.voice.text.interpreter.llm.LLMTool;
import org.openhab.core.voice.text.interpreter.llm.LLMToolRegistry;
import org.openhab.core.voice.tts.TTSException;
import org.openhab.core.voice.tts.TTSService;
import org.openhab.core.voice.tts.TTSServiceRegistry;
import org.openhab.core.voice.tts.Voice;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
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
 *         dedicated services
 */
@Component(immediate = true, configurationPid = VoiceConfigurationConstants.CONFIGURATION_PID, //
        property = Constants.SERVICE_PID + "=org.openhab.voice")
@ConfigurableService(category = "system", label = "Voice", description_uri = VoiceConfigurationConstants.CONFIG_URI)
@NonNullByDefault
public class VoiceManagerImpl implements VoiceManager, ConfigOptionProvider, DialogProcessor.DialogEventListener {
    private final Logger logger = LoggerFactory.getLogger(VoiceManagerImpl.class);
    private final ScheduledExecutorService scheduledExecutorService = ThreadPoolManager
            .getScheduledPool(ThreadPoolManager.THREAD_POOL_NAME_COMMON);

    private final TTSServiceRegistry ttsRegistry;
    private final STTServiceRegistry sttRegistry;
    private final KSServiceRegistry ksRegistry;
    private final HumanLanguageInterpreterRegistry hliRegistry;
    private final LLMToolRegistry llmToolRegistry;
    private final ConversationManager conversationManager;
    private final AudioTranscriber audioTranscriber;
    private final InterpretationService interpretationService;
    private final SpeechSynthesizer speechSynthesizer;

    private final RegistryChangeListener<LLMTool> llmToolListener = new RegistryChangeListener<>() {
        @Override
        public void added(LLMTool tool) {
            scheduleDialogRegistrations();
        }

        @Override
        public void removed(LLMTool tool) {
            stopDialogs(
                    dialog -> dialog.dialogContext.llmTools().stream().anyMatch(t -> t.getUID().equals(tool.getUID())));
        }

        @Override
        public void updated(LLMTool oldElement, LLMTool element) {
        }
    };

    private final RegistryChangeListener<TTSService> ttsListener = new RegistryChangeListener<>() {
        @Override
        public void added(TTSService ttsService) {
            scheduleDialogRegistrations();
        }

        @Override
        public void removed(TTSService ttsService) {
            stopDialogs(dialog -> dialog.dialogContext.tts().getId().equals(ttsService.getId()));
        }

        @Override
        public void updated(TTSService oldElement, TTSService element) {
        }
    };

    private final RegistryChangeListener<STTService> sttListener = new RegistryChangeListener<>() {
        @Override
        public void added(STTService sttService) {
            scheduleDialogRegistrations();
        }

        @Override
        public void removed(STTService sttService) {
            stopDialogs(dialog -> dialog.dialogContext.stt().getId().equals(sttService.getId()));
        }

        @Override
        public void updated(STTService oldElement, STTService element) {
        }
    };

    private final RegistryChangeListener<KSService> ksListener = new RegistryChangeListener<>() {
        @Override
        public void added(KSService ksService) {
            scheduleDialogRegistrations();
        }

        @Override
        public void removed(KSService ksService) {
            stopDialogs(dialog -> {
                var dt = dialog.dialogContext.dt();
                return dt != null && dt.getId().equals(ksService.getId());
            });
        }

        @Override
        public void updated(KSService oldElement, KSService element) {
        }
    };

    private final RegistryChangeListener<HumanLanguageInterpreter> hliListener = new RegistryChangeListener<>() {
        @Override
        public void added(HumanLanguageInterpreter humanLanguageInterpreter) {
            scheduleDialogRegistrations();
        }

        @Override
        public void removed(HumanLanguageInterpreter humanLanguageInterpreter) {
            stopDialogs(dialog -> dialog.dialogContext.hlis().stream()
                    .anyMatch(hli -> hli.getId().equals(humanLanguageInterpreter.getId())));
        }

        @Override
        public void updated(HumanLanguageInterpreter oldElement, HumanLanguageInterpreter element) {
        }
    };

    private final WeakHashMap<String, DialogContext> activeDialogGroups = new WeakHashMap<>();

    private final LocaleProvider localeProvider;
    private final AudioManager audioManager;
    private final EventPublisher eventPublisher;
    private final TranslationProvider i18nProvider;
    private final Storage<DialogRegistration> dialogRegistrationStorage;
    private final VoiceManagerConfiguration configuration;

    private @Nullable Bundle bundle;

    private final Map<String, DialogProcessor> dialogProcessors = new HashMap<>();
    private final Map<String, DialogProcessor> singleDialogProcessors = new ConcurrentHashMap<>();
    private @Nullable DialogContext lastDialogContext;
    private @Nullable ScheduledFuture<?> dialogRegistrationFuture;

    @Activate
    public VoiceManagerImpl(final @Reference LocaleProvider localeProvider, final @Reference AudioManager audioManager,
            final @Reference EventPublisher eventPublisher, final @Reference TranslationProvider i18nProvider,
            final @Reference StorageService storageService, final @Reference LLMToolRegistry llmToolRegistry,
            final @Reference TTSServiceRegistry ttsRegistry, final @Reference STTServiceRegistry sttRegistry,
            final @Reference KSServiceRegistry ksRegistry,
            final @Reference HumanLanguageInterpreterRegistry hliRegistry,
            final @Reference ConversationManager conversationManager,
            final @Reference ConfigDescriptionRegistry configDescriptionRegistry,
            final @Reference AudioTranscriber audioTranscriber,
            final @Reference InterpretationService interpretationService,
            final @Reference SpeechSynthesizer speechSynthesizer) {
        this.localeProvider = localeProvider;
        this.audioManager = audioManager;
        this.eventPublisher = eventPublisher;
        this.i18nProvider = i18nProvider;
        this.dialogRegistrationStorage = storageService.getStorage(DialogRegistration.class.getName(),
                this.getClass().getClassLoader());
        this.conversationManager = conversationManager;
        this.llmToolRegistry = llmToolRegistry;
        this.ttsRegistry = ttsRegistry;
        this.sttRegistry = sttRegistry;
        this.ksRegistry = ksRegistry;
        this.hliRegistry = hliRegistry;
        this.audioTranscriber = audioTranscriber;
        this.interpretationService = interpretationService;
        this.speechSynthesizer = speechSynthesizer;
        this.configuration = new VoiceManagerConfiguration(configDescriptionRegistry);
    }

    @Activate
    protected void activate(BundleContext bundleContext, Map<String, Object> config) {
        this.bundle = bundleContext.getBundle();
        llmToolRegistry.addRegistryChangeListener(llmToolListener);
        ttsRegistry.addRegistryChangeListener(ttsListener);
        sttRegistry.addRegistryChangeListener(sttListener);
        ksRegistry.addRegistryChangeListener(ksListener);
        hliRegistry.addRegistryChangeListener(hliListener);
        modified(config);
        scheduleDialogRegistrations();
    }

    @Deactivate
    protected void deactivate() {
        llmToolRegistry.removeRegistryChangeListener(llmToolListener);
        ttsRegistry.removeRegistryChangeListener(ttsListener);
        sttRegistry.removeRegistryChangeListener(sttListener);
        ksRegistry.removeRegistryChangeListener(ksListener);
        hliRegistry.removeRegistryChangeListener(hliListener);
        dialogProcessors.values().forEach(DialogProcessor::stop);
        dialogProcessors.clear();
        ScheduledFuture<?> dialogRegistrationFuture = this.dialogRegistrationFuture;
        if (dialogRegistrationFuture != null) {
            dialogRegistrationFuture.cancel(true);
            this.dialogRegistrationFuture = null;
        }
    }

    @SuppressWarnings("null")
    @Modified
    protected void modified(Map<String, Object> config) {
        if (config != null) {
            configuration.update(config);
        }
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
            Voice voice = null;
            String selectedVoiceId = voiceId;
            if (selectedVoiceId == null) {
                // use the configured default, if set
                selectedVoiceId = configuration.getDefaultVoice();
            }
            if (selectedVoiceId == null) {
                tts = getTTS();
                if (tts != null) {
                    voice = getPreferredVoice(tts.getAvailableVoices());
                }
            } else {
                voice = getVoice(selectedVoiceId);
                if (voice != null) {
                    tts = getTTS(voice);
                }
            }
            if (tts == null) {
                throw new TTSException("No TTS service can be found for voice " + selectedVoiceId);
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
        var sttService = sttId != null ? getSTT(sttId) : getSTT();
        if (sttService == null) {
            logger.warn("Speech-to-text service '{}' not available", sttId != null ? sttId : "default");
            return "";
        }
        return audioTranscriber.transcribe(audioSource, sttService, locale);
    }

    @Override
    public String transcribe(AudioStream audioStream, @Nullable String sttId, @Nullable Locale locale) {
        var sttService = sttId != null ? getSTT(sttId) : getSTT();
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
            HumanLanguageInterpreter interpreter = getHLI();
            if (interpreter != null) {
                interpreters.add(interpreter);
            }
        } else {
            interpreters = getHLIsByIds(nullSafeArgs.hliIdList());
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
                    configuration.getSystemPrompt());
        }

        if (nullSafeArgs.hliIdList().isEmpty()) {
            throw new InterpretationException("No human language interpreter available!");
        } else {
            throw new InterpretationException(
                    "No human language interpreter can be found for " + nullSafeArgs.hliIdList());
        }
    }

    private @Nullable Voice getVoice(@Nullable String id) {
        if (id == null) {
            return null;
        } else if (id.contains(":")) {
            // it is a fully qualified unique id
            String[] segments = id.split(":");
            TTSService tts = getTTS(segments[0]);
            if (tts != null) {
                return getVoice(tts.getAvailableVoices(), segments[1]);
            }
        } else {
            // voiceId is not fully qualified
            TTSService tts = getTTS();
            if (tts != null) {
                return getVoice(tts.getAvailableVoices(), id);
            }
        }
        return null;
    }

    private @Nullable Voice getVoice(Set<Voice> voices, String id) {
        for (Voice voice : voices) {
            if (voice.getUID().endsWith(":" + id)) {
                return voice;
            }
        }
        return null;
    }

    @Override
    public @Nullable Voice getPreferredVoice(Set<Voice> voices) {
        // Express preferences with a Language Priority List
        Locale locale = localeProvider.getLocale();

        // Get collection of voice locales
        Collection<Locale> locales = new ArrayList<>();
        for (Voice currentVoice : voices) {
            locales.add(currentVoice.getLocale());
        }

        // Determine preferred locale based on RFC 4647
        String ranges = locale.toLanguageTag();
        List<Locale.LanguageRange> languageRanges = Locale.LanguageRange.parse(ranges + "-*");
        Locale preferredLocale = Locale.lookup(languageRanges, locales);

        // As a last resort choose some Locale
        if (preferredLocale == null && !voices.isEmpty()) {
            preferredLocale = locales.iterator().next();
        }
        if (preferredLocale == null) {
            return null;
        }

        // Determine preferred voice
        Voice preferredVoice = null;
        for (Voice currentVoice : voices) {
            if (preferredLocale.equals(currentVoice.getLocale())) {
                preferredVoice = currentVoice;
            }
        }

        // Return preferred voice
        return preferredVoice;
    }

    @Override
    public DialogContext.Builder getDialogContextBuilder() {
        return new DialogContext.Builder(configuration.getKeyword(), localeProvider.getLocale()) //
                .withSink(audioManager.getSink()) //
                .withSource(audioManager.getSource()) //
                .withConversation(conversationManager.getConversation("")) //
                .withKS(this.getKS()) //
                .withSTT(this.getSTT()) //
                .withTTS(this.getTTS()) //
                .withHLI(this.getHLI()) //
                .withVoice(this.getDefaultVoice()) //
                .withMelody(configuration.getListeningMelody()) //
                .withListeningItem(configuration.getListeningItem()) //
                .withLLMTools(llmToolRegistry.getAll()) //
                .withSystemPrompt(configuration.getSystemPrompt());
    }

    @Override
    public List<DialogContext> getDialogsContexts() {
        return dialogProcessors.values().stream().map(DialogProcessor::getContext).toList();
    }

    @Override
    public @Nullable DialogContext getLastDialogContext() {
        return lastDialogContext;
    }

    @Override
    public @Nullable DTServiceHandle startDialog(DialogContext context) throws IllegalStateException {
        var dtService = context.dt();
        var ksKeyword = context.keyword();
        if (dtService == null) {
            throw new IllegalStateException(
                    "Invalid dialog context for persistent dialog: missing dialog trigger implementation");
        }
        if (dtService instanceof KSService && (ksKeyword == null || ksKeyword.isEmpty())) {
            throw new IllegalStateException(
                    "Invalid dialog context for persistent dialog: missing keyword spot configuration");
        }
        Bundle b = bundle;
        if (b == null) {
            throw new IllegalStateException("Bundle is not (yet?) set.");
        }
        if ((dtService instanceof KSService ksService
                && !checkLocales(ksService.getSupportedLocales(), context.locale()))
                || !checkLocales(context.stt().getSupportedLocales(), context.locale()) || !context.hlis().stream()
                        .allMatch(interpreter -> checkLocales(interpreter.getSupportedLocales(), context.locale()))) {
            throw new IllegalStateException("Cannot start dialog as provided locale is not supported by all services.");
        } else {
            DialogProcessor processor = dialogProcessors.get(context.source().getId());
            if (processor == null) {
                logger.debug("Starting a new dialog for source {} ({})", context.source().getLabel(null),
                        context.source().getId());
                processor = new DialogProcessor(context, this, this.eventPublisher, this.activeDialogGroups,
                        this.i18nProvider, b, this.interpretationService, this.speechSynthesizer);
                dialogProcessors.put(context.source().getId(), processor);
                return processor.start();
            } else {
                throw new IllegalStateException(
                        String.format("Cannot start dialog as a dialog is already started for audio source '%s'.",
                                context.source().getLabel(null)));
            }
        }
    }

    @Override
    public void stopDialog(@Nullable AudioSource source) throws IllegalStateException {
        AudioSource audioSource = (source == null) ? audioManager.getSource() : source;
        if (audioSource != null) {
            DialogProcessor processor = dialogProcessors.remove(audioSource.getId());
            singleDialogProcessors.values().removeIf(e -> !e.isProcessing());
            if (processor == null) {
                processor = singleDialogProcessors.get(audioSource.getId());
            }
            if (processor != null) {
                processor.stop();
                logger.debug("Dialog stopped for source {} ({})", audioSource.getLabel(null), audioSource.getId());
            } else {
                throw new IllegalStateException(
                        String.format("Cannot stop dialog as no dialog is started for audio source '%s'.",
                                audioSource.getLabel(null)));
            }
        } else {
            throw new IllegalStateException("Cannot stop dialog as audio source is missing.");
        }
    }

    @Override
    public void stopDialog(DialogContext context) throws IllegalStateException {
        stopDialog(context.source());
    }

    @Override
    public void listenAndAnswer(DialogContext context) throws IllegalStateException {
        Bundle b = bundle;
        if (b == null) {
            throw new IllegalStateException("Cannot execute a simple dialog as services are missing.");
        } else if (!checkLocales(context.stt().getSupportedLocales(), context.locale()) || !context.hlis().stream()
                .allMatch(interpreter -> checkLocales(interpreter.getSupportedLocales(), context.locale()))) {
            throw new IllegalStateException(
                    "Cannot execute a simple dialog as provided locale is not supported by all services.");
        } else {
            boolean isSingleDialog = false;
            var audioSource = context.source();
            DialogProcessor activeProcessor = dialogProcessors.get(audioSource.getId());
            singleDialogProcessors.values().removeIf(e -> !e.isProcessing());
            if (activeProcessor == null) {
                isSingleDialog = true;
                activeProcessor = singleDialogProcessors.get(audioSource.getId());
            }
            var processor = new DialogProcessor(context, this, this.eventPublisher, this.activeDialogGroups,
                    this.i18nProvider, b, this.interpretationService, this.speechSynthesizer);
            if (activeProcessor == null) {
                logger.debug("Executing a simple dialog for source {} ({})", audioSource.getLabel(null),
                        audioSource.getId());
                processor.startSimpleDialog();
                singleDialogProcessors.put(audioSource.getId(), processor);
            } else if (!isSingleDialog && activeProcessor.isCompatible(processor)) {
                logger.debug("Executing a simple dialog for active source {} ({})", audioSource.getLabel(null),
                        audioSource.getId());
                activeProcessor.startSimpleDialog();
            } else {
                throw new IllegalStateException(String.format(
                        "Cannot execute a simple dialog as a dialog is already started for audio source '%s'.",
                        audioSource.getLabel(null)));
            }
        }
    }

    @Override
    public void registerDialog(DialogRegistration registration) throws IllegalStateException {
        if (dialogRegistrationStorage.containsKey(registration.sourceId)) {
            throw new IllegalStateException(String.format(
                    "Cannot register dialog as a dialog is registered for audio source '%s'.", registration.sourceId));
        }
        synchronized (dialogRegistrationStorage) {
            dialogRegistrationStorage.put(registration.sourceId, registration);
        }
        scheduleDialogRegistrations();
    }

    @Override
    public void unregisterDialog(DialogRegistration registration) {
        unregisterDialog(registration.sourceId);
    }

    @Override
    public void unregisterDialog(String sourceId) {
        synchronized (dialogRegistrationStorage) {
            var registrationRef = dialogRegistrationStorage.remove(sourceId);
            if (registrationRef != null) {
                var dialog = dialogProcessors.get(sourceId);
                if (dialog != null) {
                    stopDialog(dialog.dialogContext);
                }
            }
        }
    }

    @Override
    public List<DialogRegistration> getDialogRegistrations() {
        var list = new ArrayList<DialogRegistration>();
        dialogRegistrationStorage.getValues().forEach(dr -> {
            if (dr != null) {
                // update running state
                dr.running = dialogProcessors.containsKey(dr.sourceId);
                list.add(dr);
            }
        });
        return list;
    }

    private boolean checkLocales(Set<Locale> supportedLocales, Locale locale) {
        if (supportedLocales.isEmpty()) {
            return true;
        }
        return supportedLocales.stream().anyMatch(sLocale -> {
            var country = sLocale.getCountry();
            return Objects.equals(sLocale.getLanguage(), locale.getLanguage())
                    && (country == null || country.isBlank() || country.equals(locale.getCountry()));
        });
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addAudioSink(AudioSink audioSink) {
        scheduleDialogRegistrations();
    }

    protected void removeAudioSink(AudioSink audioSink) {
        stopDialogs(dialog -> dialog.dialogContext.sink().getId().equals(audioSink.getId()));
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addAudioSource(AudioSource audioSource) {
        scheduleDialogRegistrations();
    }

    protected void removeAudioSource(AudioSource audioSource) {
        stopDialogs(dialog -> dialog.dialogContext.source().getId().equals(audioSource.getId()));
    }

    @Override
    public @Nullable TTSService getTTS() {
        TTSService tts = null;
        String defaultTTS = configuration.getDefaultTTS();
        if (defaultTTS != null) {
            tts = ttsRegistry.get(defaultTTS);
            if (tts == null) {
                logger.warn("Default TTS service '{}' not available!", defaultTTS);
            }
        } else {
            tts = ttsRegistry.stream().findFirst().orElse(null);
            if (tts == null) {
                logger.debug("No TTS service available!");
            }
        }
        return tts;
    }

    @Override
    public @Nullable TTSService getTTS(@Nullable String id) {
        return id == null ? null : ttsRegistry.get(id);
    }

    private @Nullable TTSService getTTS(Voice voice) {
        return getTTS(voice.getUID().split(":")[0]);
    }

    @Override
    public Collection<TTSService> getTTSs() {
        return new HashSet<>(ttsRegistry.getAll());
    }

    @Override
    public @Nullable STTService getSTT() {
        STTService stt = null;
        String defaultSTT = configuration.getDefaultSTT();
        if (defaultSTT != null) {
            stt = sttRegistry.get(defaultSTT);
            if (stt == null) {
                logger.warn("Default STT service '{}' not available!", defaultSTT);
            }
        } else {
            stt = sttRegistry.stream().findFirst().orElse(null);
            if (stt == null) {
                logger.debug("No STT service available!");
            }
        }
        return stt;
    }

    @Override
    public @Nullable STTService getSTT(@Nullable String id) {
        return id == null ? null : sttRegistry.get(id);
    }

    @Override
    public Collection<STTService> getSTTs() {
        return new HashSet<>(sttRegistry.getAll());
    }

    @Override
    public @Nullable KSService getKS() {
        KSService ks = null;
        String defaultKS = configuration.getDefaultKS();
        if (defaultKS != null) {
            ks = ksRegistry.get(defaultKS);
            if (ks == null) {
                logger.warn("Default KS service '{}' not available!", defaultKS);
            }
        } else {
            ks = ksRegistry.stream().findFirst().orElse(null);
            if (ks == null) {
                logger.debug("No KS service available!");
            }
        }
        return ks;
    }

    @Override
    public @Nullable KSService getKS(@Nullable String id) {
        return id == null ? null : ksRegistry.get(id);
    }

    @Override
    public Collection<KSService> getKSs() {
        return new HashSet<>(ksRegistry.getAll());
    }

    @Override
    public @Nullable HumanLanguageInterpreter getHLI() {
        HumanLanguageInterpreter hli = null;
        String defaultHLI = configuration.getDefaultHLI();
        if (defaultHLI != null) {
            hli = hliRegistry.get(defaultHLI);
            if (hli == null) {
                logger.warn("Default HumanLanguageInterpreter '{}' not available!", defaultHLI);
            }
        } else {
            hli = hliRegistry.stream().findFirst().orElse(null);
            if (hli == null) {
                logger.debug("No HumanLanguageInterpreter available!");
            }
        }
        return hli;
    }

    @Override
    public @Nullable HumanLanguageInterpreter getHLI(@Nullable String id) {
        return id == null ? null : hliRegistry.get(id);
    }

    @Override
    public List<HumanLanguageInterpreter> getHLIsByIds(@Nullable String ids) {
        return hliRegistry.getByIds(ids);
    }

    @Override
    public List<HumanLanguageInterpreter> getHLIsByIds(List<String> ids) {
        return hliRegistry.getByIds(ids);
    }

    @Override
    public Collection<HumanLanguageInterpreter> getHLIs() {
        return new HashSet<>(hliRegistry.getAll());
    }

    @Override
    public Set<Voice> getAllVoices() {
        return getAllVoicesSorted(localeProvider.getLocale());
    }

    private Set<Voice> getAllVoicesSorted(Locale locale) {
        return ttsRegistry.stream().map(TTSService::getAvailableVoices).flatMap(Collection::stream)
                .sorted(createVoiceComparator(locale)).collect(Collectors
                        .collectingAndThen(Collectors.toCollection(LinkedHashSet::new), Collections::unmodifiableSet));
    }

    /**
     * Creates a comparator which compares voices using the given locale in the following order:
     * <ol>
     * <li>Voice TTSService label (localized with the given locale)
     * <li>Voice locale display name (localized with the given locale)
     * <li>Voice label
     * </ol>
     *
     * @param locale the locale used for comparing {@link TTSService} labels and {@link Voice} locale display names
     * @return the localized voice comparator
     */
    private Comparator<Voice> createVoiceComparator(Locale locale) {
        Comparator<Voice> byTTSLabel = (Voice v1, Voice v2) -> {
            TTSService tts1 = getTTS(v1);
            TTSService tts2 = getTTS(v2);
            return (tts1 == null || tts2 == null) ? 0
                    : tts1.getLabel(locale).compareToIgnoreCase(tts2.getLabel(locale));
        };
        Comparator<Voice> byVoiceLocale = (Voice v1, Voice v2) -> v1.getLocale().getDisplayName(locale)
                .compareToIgnoreCase(v2.getLocale().getDisplayName(locale));
        return byTTSLabel.thenComparing(byVoiceLocale).thenComparing(Voice::getLabel);
    }

    @Override
    public @Nullable Voice getDefaultVoice() {
        String localDefaultVoice = configuration.getDefaultVoice();
        return localDefaultVoice != null ? getVoice(localDefaultVoice) : null;
    }

    @Override
    public @Nullable Collection<ParameterOption> getParameterOptions(URI uri, String param, @Nullable String context,
            @Nullable Locale locale) {
        if (VoiceConfigurationConstants.CONFIG_URI.equals(uri.toString())) {
            switch (param) {
                case VoiceConfigurationConstants.CONFIG_DEFAULT_HLI:
                    return hliRegistry.stream()
                            .sorted((hli1, hli2) -> hli1.getLabel(locale).compareToIgnoreCase(hli2.getLabel(locale)))
                            .map(hli -> new ParameterOption(hli.getId(), hli.getLabel(locale))).toList();
                case VoiceConfigurationConstants.CONFIG_DEFAULT_KS:
                    return ksRegistry.stream()
                            .sorted((ks1, ks2) -> ks1.getLabel(locale).compareToIgnoreCase(ks2.getLabel(locale)))
                            .map(ks -> new ParameterOption(ks.getId(), ks.getLabel(locale))).toList();
                case VoiceConfigurationConstants.CONFIG_DEFAULT_STT:
                    return sttRegistry.stream()
                            .sorted((stt1, stt2) -> stt1.getLabel(locale).compareToIgnoreCase(stt2.getLabel(locale)))
                            .map(stt -> new ParameterOption(stt.getId(), stt.getLabel(locale))).toList();
                case VoiceConfigurationConstants.CONFIG_DEFAULT_TTS:
                    return ttsRegistry.stream()
                            .sorted((tts1, tts2) -> tts1.getLabel(locale).compareToIgnoreCase(tts2.getLabel(locale)))
                            .map(tts -> new ParameterOption(tts.getId(), tts.getLabel(locale))).toList();
                case VoiceConfigurationConstants.CONFIG_DEFAULT_VOICE:
                    Locale nullSafeLocale = locale != null ? locale : localeProvider.getLocale();
                    return getAllVoicesSorted(nullSafeLocale)
                            .stream().filter(v -> getTTS(v) != null).map(
                                    v -> new ParameterOption(v.getUID(),
                                            String.format("%s - %s - %s", getTTS(v).getLabel(nullSafeLocale),
                                                    v.getLocale().getDisplayName(nullSafeLocale), v.getLabel())))
                            .toList();
            }
        }
        return null;
    }

    private void stopDialogs(Predicate<DialogProcessor> filter) {
        synchronized (dialogRegistrationStorage) {
            var dialogsToStop = dialogProcessors.values().stream().filter(filter).toList();
            if (dialogsToStop.isEmpty()) {
                return;
            }
            for (var dialog : dialogsToStop) {
                stopDialog(dialog.dialogContext.source());
            }
        }
    }

    /**
     * In order to reduce the number of dialog registration builds
     * this method schedules a call to {@link #buildDialogRegistrations() buildDialogRegistrations} in five seconds
     * and cancel the previous scheduled call if any.
     */
    private void scheduleDialogRegistrations() {
        ScheduledFuture<?> job = this.dialogRegistrationFuture;
        if (job != null) {
            job.cancel(false);
        }
        dialogRegistrationFuture = scheduledExecutorService.schedule(this::buildDialogRegistrations, 5,
                TimeUnit.SECONDS);
    }

    /**
     * This method tries to start a dialog for each dialog registration.
     * It's only called from {@link #scheduleDialogRegistrations() scheduleDialogRegistrations} in order to
     * reduce the number of executions.
     */
    private void buildDialogRegistrations() {
        synchronized (dialogRegistrationStorage) {
            dialogRegistrationStorage.getValues().forEach(dr -> {
                if (dr != null && !dialogProcessors.containsKey(dr.sourceId)) {
                    try {
                        Conversation conversation = conversationManager
                                .getConversation(Objects.requireNonNullElse(dr.conversationId, ""));
                        startDialog(getDialogContextBuilder() //
                                .withSink(audioManager.getSink(dr.sinkId)) //
                                .withSource(audioManager.getSource(dr.sourceId)) //
                                .withKS(getKS(dr.ksId)) //
                                .withKeyword(dr.keyword) //
                                .withSTT(getSTT(dr.sttId)) //
                                .withTTS(getTTS(dr.ttsId)) //
                                .withVoice(getVoice(dr.voiceId)) //
                                .withHLIs(getHLIsByIds(dr.hliIds)) //
                                .withLLMTools(llmToolRegistry.getByIds(dr.llmToolIds)) //
                                .withLocale(dr.locale) //
                                .withDialogGroup(dr.dialogGroup) //
                                .withConversation(conversation) //
                                .withLocationItem(dr.locationItem) //
                                .withListeningItem(dr.listeningItem) //
                                .withMelody(dr.listeningMelody) //
                                .build());
                    } catch (IllegalStateException e) {
                        logger.debug("Unable to start dialog registration: {}", e.getMessage());
                    }
                }
            });
        }
    }

    @Override
    public void onBeforeDialogInterpretation(DialogContext context) {
        lastDialogContext = context;
    }

    @Override
    public void onDialogStopped(DialogContext context) {
        var registration = dialogRegistrationStorage.get(context.source().getId());
        if (registration != null) {
            // try to rebuild in case it was manually stopped
            scheduleDialogRegistrations();
        }
    }
}
