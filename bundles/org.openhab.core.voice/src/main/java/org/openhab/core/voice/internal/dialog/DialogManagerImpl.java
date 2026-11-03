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

import java.util.ArrayList;
import java.util.HashMap;
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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.voice.dialog.DialogContext;
import org.openhab.core.voice.dialog.DialogRegistration;
import org.openhab.core.voice.dialog.trigger.DTService;
import org.openhab.core.voice.dialog.trigger.DTServiceHandle;
import org.openhab.core.voice.dialog.trigger.ks.KSService;
import org.openhab.core.voice.dialog.trigger.ks.KSServiceRegistry;
import org.openhab.core.voice.internal.VoiceConfigurationConstants;
import org.openhab.core.voice.internal.VoiceI18nProvider;
import org.openhab.core.voice.internal.VoiceManagerConfiguration;
import org.openhab.core.voice.internal.text.InterpretationService;
import org.openhab.core.voice.internal.tts.SpeechSynthesizer;
import org.openhab.core.voice.stt.STTService;
import org.openhab.core.voice.stt.STTServiceRegistry;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.HumanLanguageInterpreterRegistry;
import org.openhab.core.voice.text.conversation.Conversation;
import org.openhab.core.voice.text.conversation.ConversationManager;
import org.openhab.core.voice.text.interpreter.llm.LLMTool;
import org.openhab.core.voice.text.interpreter.llm.LLMToolRegistry;
import org.openhab.core.voice.tts.TTSService;
import org.openhab.core.voice.tts.TTSServiceRegistry;
import org.openhab.core.voice.tts.Voice;
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
 * Implementation of {@link DialogManager} that manages dialog registrations, debounced restart jobs, reactivity to
 * service changes, and concurrency group locking.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(service = DialogManager.class, configurationPid = VoiceConfigurationConstants.CONFIGURATION_PID)
@NonNullByDefault
public class DialogManagerImpl implements DialogManager, DialogProcessor.DialogEventListener {

    private final Logger logger = LoggerFactory.getLogger(DialogManagerImpl.class);

    private final ScheduledExecutorService scheduledExecutorService = ThreadPoolManager
            .getScheduledPool(ThreadPoolManager.THREAD_POOL_NAME_COMMON);

    private final LocaleProvider localeProvider;
    private final AudioManager audioManager;
    private final EventPublisher eventPublisher;
    private final Storage<DialogRegistration> dialogRegistrationStorage;
    private final LLMToolRegistry llmToolRegistry;
    private final TTSServiceRegistry ttsRegistry;
    private final STTServiceRegistry sttRegistry;
    private final KSServiceRegistry ksRegistry;
    private final HumanLanguageInterpreterRegistry hliRegistry;
    private final ConversationManager conversationManager;
    private final InterpretationService interpretationService;
    private final SpeechSynthesizer speechSynthesizer;
    private final VoiceI18nProvider i18nProvider;
    private final VoiceManagerConfiguration configuration;

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

    private final Map<String, DialogContext> activeDialogGroups = new WeakHashMap<>();
    private final Map<String, DialogProcessor> dialogProcessors = new HashMap<>();
    private final Map<String, DialogProcessor> singleDialogProcessors = new ConcurrentHashMap<>();
    private @Nullable DialogContext lastDialogContext;
    private @Nullable ScheduledFuture<?> dialogRegistrationFuture;

    @Activate
    public DialogManagerImpl(final @Reference LocaleProvider localeProvider, final @Reference AudioManager audioManager,
            final @Reference EventPublisher eventPublisher, final @Reference StorageService storageService,
            final @Reference LLMToolRegistry llmToolRegistry, final @Reference TTSServiceRegistry ttsRegistry,
            final @Reference STTServiceRegistry sttRegistry, final @Reference KSServiceRegistry ksRegistry,
            final @Reference HumanLanguageInterpreterRegistry hliRegistry,
            final @Reference ConversationManager conversationManager,
            final @Reference InterpretationService interpretationService,
            final @Reference SpeechSynthesizer speechSynthesizer, final @Reference VoiceI18nProvider i18nProvider,
            final @Reference ConfigDescriptionRegistry configDescriptionRegistry) {
        this.localeProvider = localeProvider;
        this.audioManager = audioManager;
        this.eventPublisher = eventPublisher;
        this.dialogRegistrationStorage = storageService.getStorage(DialogRegistration.class.getName(),
                this.getClass().getClassLoader());
        this.llmToolRegistry = llmToolRegistry;
        this.ttsRegistry = ttsRegistry;
        this.sttRegistry = sttRegistry;
        this.ksRegistry = ksRegistry;
        this.hliRegistry = hliRegistry;
        this.conversationManager = conversationManager;
        this.interpretationService = interpretationService;
        this.speechSynthesizer = speechSynthesizer;
        this.i18nProvider = i18nProvider;
        this.configuration = new VoiceManagerConfiguration(configDescriptionRegistry);
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        ksRegistry.addRegistryChangeListener(ksListener);
        sttRegistry.addRegistryChangeListener(sttListener);
        llmToolRegistry.addRegistryChangeListener(llmToolListener);
        hliRegistry.addRegistryChangeListener(hliListener);
        ttsRegistry.addRegistryChangeListener(ttsListener);
        modified(config);
        scheduleDialogRegistrations();
    }

    @Deactivate
    protected void deactivate() {
        ttsRegistry.removeRegistryChangeListener(ttsListener);
        hliRegistry.removeRegistryChangeListener(hliListener);
        llmToolRegistry.removeRegistryChangeListener(llmToolListener);
        sttRegistry.removeRegistryChangeListener(sttListener);
        ksRegistry.removeRegistryChangeListener(ksListener);
        dialogProcessors.values().forEach(DialogProcessor::stop);
        dialogProcessors.clear();
        ScheduledFuture<?> future = this.dialogRegistrationFuture;
        if (future != null) {
            future.cancel(true);
            this.dialogRegistrationFuture = null;
        }
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        configuration.update(config);
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
    public DialogContext.Builder getDialogContextBuilder() {
        return new DialogContext.Builder(configuration.getKeyword(), localeProvider.getLocale()) //
                .withSink(audioManager.getSink()) //
                .withSource(audioManager.getSource()) //
                .withConversation(conversationManager.getConversation("")) //
                .withKS(getKS()) //
                .withSTT(getSTT()) //
                .withTTS(getTTS()) //
                .withHLI(getHLI()) //
                .withVoice(getDefaultVoice()) //
                .withMelody(configuration.getListeningMelody()) //
                .withListeningItem(configuration.getListeningItem()) //
                .withLLMTools(llmToolRegistry.getAll()) //
                .withSystemPrompt(configuration.getSystemPrompt());
    }

    @Override
    public @Nullable DialogContext getLastDialogContext() {
        return lastDialogContext;
    }

    @Override
    public List<DialogContext> getDialogsContexts() {
        return dialogProcessors.values().stream().map(DialogProcessor::getContext).toList();
    }

    @Override
    public @Nullable DTServiceHandle startDialog(DialogContext context) throws IllegalStateException {
        validateLocaleCompatibility(context);

        DialogProcessor processor = dialogProcessors.get(context.source().getId());
        if (processor == null) {
            logger.debug("Starting a new dialog for source {} ({})", context.source().getLabel(null),
                    context.source().getId());
            DialogAudioDriver audioDriver = new DialogAudioDriver(context);
            DialogFeedbackService feedbackService = new DialogFeedbackService(context, eventPublisher);
            processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                    speechSynthesizer, i18nProvider, this);
            dialogProcessors.put(context.source().getId(), processor);
            return processor.start();
        } else {
            throw new IllegalStateException(
                    String.format("Cannot start dialog as a dialog is already started for audio source '%s'.",
                            context.source().getLabel(null)));
        }
    }

    @Override
    public void stopDialog(@Nullable AudioSource source) throws IllegalStateException {
        AudioSource audioSource = (source == null) ? audioManager.getSource() : source;
        if (audioSource != null) {
            DialogProcessor processor = dialogProcessors.remove(audioSource.getId());
            singleDialogProcessors.values().removeIf(p -> !p.isProcessing());
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
        validateLocaleCompatibility(context);

        synchronized (activeDialogGroups) {
            if (!activeDialogGroups.containsKey(context.dialogGroup())) {
                logger.debug("Acquiring dialog group '{}'", context.dialogGroup());
                activeDialogGroups.put(context.dialogGroup(), context);
            } else {
                logger.warn("Ignoring listenAndAnswer, dialog group '{}' running", context.dialogGroup());
                return;
            }
        }

        boolean isSingleDialog = false;
        var audioSource = context.source();
        DialogProcessor activeProcessor = dialogProcessors.get(audioSource.getId());
        singleDialogProcessors.values().removeIf(e -> !e.isProcessing());
        if (activeProcessor == null) {
            isSingleDialog = true;
            activeProcessor = singleDialogProcessors.get(audioSource.getId());
        }

        DialogAudioDriver audioDriver = new DialogAudioDriver(context);
        DialogFeedbackService feedbackService = new DialogFeedbackService(context, eventPublisher);
        var processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, this);

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
            synchronized (activeDialogGroups) {
                activeDialogGroups.remove(context.dialogGroup());
            }
            throw new IllegalStateException(String.format(
                    "Cannot execute a simple dialog as a dialog is already started for audio source '%s'.",
                    audioSource.getLabel(null)));
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
        DialogRegistration registration;

        synchronized (dialogRegistrationStorage) {
            registration = dialogRegistrationStorage.remove(sourceId);
        }

        if (registration != null) {
            var dialog = dialogProcessors.get(sourceId);
            if (dialog != null) {
                stopDialog(dialog.dialogContext);
            }
        }
    }

    @Override
    public List<DialogRegistration> getDialogRegistrations() {
        var list = new ArrayList<DialogRegistration>();
        dialogRegistrationStorage.getValues().forEach(dr -> {
            if (dr != null) {
                dr.running = dialogProcessors.containsKey(dr.sourceId);
                list.add(dr);
            }
        });
        return list;
    }

    @Override
    public void onBeforeDialogInterpretation(DialogContext context) {
        lastDialogContext = context;
    }

    @Override
    public void onDialogStopped(DialogContext context) {
        synchronized (activeDialogGroups) {
            activeDialogGroups.remove(context.dialogGroup());
        }
        var registration = dialogRegistrationStorage.get(context.source().getId());
        if (registration != null) {
            scheduleDialogRegistrations();
        }
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

    private void scheduleDialogRegistrations() {
        ScheduledFuture<?> job = this.dialogRegistrationFuture;
        if (job != null) {
            job.cancel(false);
        }
        dialogRegistrationFuture = scheduledExecutorService.schedule(this::buildDialogRegistrations, 5,
                TimeUnit.SECONDS);
    }

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
                                .withHLIs(hliRegistry.getByIds(dr.hliIds)) //
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

    private static void validateLocaleCompatibility(DialogContext context) throws IllegalStateException {
        final var dtService = getDtService(context);

        if ((dtService instanceof KSService ksService)
                && !checkLocales(ksService.getSupportedLocales(), context.locale())) {
            throw new IllegalStateException(
                    "Cannot start dialog as provided locale is not supported by KS service " + ksService.getUID());
        }
        if (!checkLocales(context.stt().getSupportedLocales(), context.locale())) {
            throw new IllegalStateException(
                    "Cannot start dialog as provided locale is not supported by STT service " + context.stt().getUID());
        }
        if (!context.hlis().stream()
                .allMatch(interpreter -> checkLocales(interpreter.getSupportedLocales(), context.locale()))) {
            throw new IllegalStateException(
                    "Cannot start dialog as provided locale is not supported by all HLI services.");
        }
        if (context.voice() != null && !context.voice().getLocale().equals(context.locale())) {
            throw new IllegalStateException(
                    "Cannot start dialog as provided locale is not supported by TTS voice " + context.voice().getUID());
        }
    }

    private static DTService getDtService(DialogContext context) {
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
        return dtService;
    }

    private static boolean checkLocales(Set<Locale> supportedLocales, Locale locale) {
        if (supportedLocales.isEmpty()) {
            return true;
        }
        return supportedLocales.stream().anyMatch(sLocale -> {
            var country = sLocale.getCountry();
            return Objects.equals(sLocale.getLanguage(), locale.getLanguage()) && (country.isBlank() || country.equals(locale.getCountry()));
        });
    }

    private @Nullable KSService getKS() {
        String defaultKS = configuration.getDefaultKS();
        if (defaultKS != null) {
            KSService ks = ksRegistry.get(defaultKS);
            if (ks == null) {
                logger.warn("Default KS service '{}' not available!", defaultKS);
            }
            return ks;
        }
        return ksRegistry.stream().findFirst().orElse(null);
    }

    private @Nullable KSService getKS(@Nullable String id) {
        return id == null ? null : ksRegistry.get(id);
    }

    private @Nullable STTService getSTT() {
        String defaultSTT = configuration.getDefaultSTT();
        if (defaultSTT != null) {
            STTService stt = sttRegistry.get(defaultSTT);
            if (stt == null) {
                logger.warn("Default STT service '{}' not available!", defaultSTT);
            }
            return stt;
        }
        return sttRegistry.stream().findFirst().orElse(null);
    }

    private @Nullable STTService getSTT(@Nullable String id) {
        return id == null ? null : sttRegistry.get(id);
    }
    private @Nullable HumanLanguageInterpreter getHLI() {
        String defaultHLI = configuration.getDefaultHLI();
        if (defaultHLI != null) {
            HumanLanguageInterpreter hli = hliRegistry.get(defaultHLI);
            if (hli == null) {
                logger.warn("Default HumanLanguageInterpreter '{}' not available!", defaultHLI);
            }
            return hli;
        }
        return hliRegistry.stream().findFirst().orElse(null);
    }

    private @Nullable TTSService getTTS() {
        String defaultTTS = configuration.getDefaultTTS();
        if (defaultTTS != null) {
            TTSService tts = ttsRegistry.get(defaultTTS);
            if (tts == null) {
                logger.warn("Default TTS service '{}' not available!", defaultTTS);
            }
            return tts;
        }
        return ttsRegistry.stream().findFirst().orElse(null);
    }

    private @Nullable TTSService getTTS(@Nullable String id) {
        return id == null ? null : ttsRegistry.get(id);
    }

    private @Nullable Voice getDefaultVoice() {
        String localDefaultVoice = configuration.getDefaultVoice();
        return localDefaultVoice != null ? getVoice(localDefaultVoice) : null;
    }

    private @Nullable Voice getVoice(@Nullable String id) {
        if (id == null) {
            return null;
        } else if (id.contains(":")) {
            String[] segments = id.split(":");
            TTSService tts = getTTS(segments[0]);
            if (tts != null) {
                return getVoice(tts.getAvailableVoices(), segments[1]);
            }
        } else {
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
}
