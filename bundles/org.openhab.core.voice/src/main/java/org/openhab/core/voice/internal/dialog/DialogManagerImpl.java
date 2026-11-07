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
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import org.openhab.core.voice.internal.VoiceServiceResolver;
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
    private final VoiceServiceResolver voiceServiceResolver;

    private final RegistryChangeListener<KSService> ksListener = new RegistryChangeListener<>() {
        @Override
        public void added(KSService ksService) {
            scheduleDialogRegistrations();
        }

        @Override
        public void removed(KSService ksService) {
            stopDialogs(dialog -> {
                var dt = dialog.getContext().dt();
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
            stopDialogs(dialog -> dialog.getContext().stt().getId().equals(sttService.getId()));
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
                    dialog -> dialog.getContext().llmTools().stream().anyMatch(t -> t.getUID().equals(tool.getUID())));
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
            stopDialogs(dialog -> dialog.getContext().hlis().stream()
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
            stopDialogs(dialog -> dialog.getContext().tts().getId().equals(ttsService.getId()));
        }

        @Override
        public void updated(TTSService oldElement, TTSService element) {
        }
    };

    /**
     * Maps a dialog group to its active {@link DialogContext} (if any dialog active).
     * This is used to prevent concurrent dialogs from being started for the same dialog group.
     * Dialog groups are user-defined, and e.g., used to prevent multiple dialogs from running simultaneously in an open
     * space.
     */
    private final Map<String, DialogContext> activeDialogGroups = new ConcurrentHashMap<>();
    /**
     * Maps {@link AudioSource} IDs to {@link DialogProcessor} instances for active, persistent dialogs.
     */
    private final Map<String, DialogProcessor> dialogProcessors = new ConcurrentHashMap<>();
    /**
     * Maps {@link AudioSource} IDs to {@link DialogProcessor} instances for one-off/single-run dialogs.
     */
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
            final @Reference VoiceServiceResolver voiceServiceResolver) {
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
        this.voiceServiceResolver = voiceServiceResolver;
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
        singleDialogProcessors.values().forEach(DialogProcessor::stop);
        singleDialogProcessors.clear();
        activeDialogGroups.clear();
        ScheduledFuture<?> future = this.dialogRegistrationFuture;
        if (future != null) {
            future.cancel(true);
            this.dialogRegistrationFuture = null;
        }
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        scheduleDialogRegistrations();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addAudioSink(AudioSink audioSink) {
        scheduleDialogRegistrations();
    }

    protected void removeAudioSink(AudioSink audioSink) {
        stopDialogs(dialog -> dialog.getContext().sink().getId().equals(audioSink.getId()));
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addAudioSource(AudioSource audioSource) {
        scheduleDialogRegistrations();
    }

    protected void removeAudioSource(AudioSource audioSource) {
        stopDialogs(dialog -> dialog.getContext().source().getId().equals(audioSource.getId()));
    }

    @Override
    public DialogContext.Builder getDialogContextBuilder() {
        var config = voiceServiceResolver.getConfiguration();
        return new DialogContext.Builder(config.getKeyword(), localeProvider.getLocale()) //
                .withSink(audioManager.getSink()) //
                .withSource(audioManager.getSource()) //
                .withConversation(conversationManager.getConversation("")) //
                .withKS(voiceServiceResolver.getKS()) //
                .withSTT(voiceServiceResolver.getSTT()) //
                .withTTS(voiceServiceResolver.getTTS()) //
                .withHLI(voiceServiceResolver.getHLI()) //
                .withVoice(voiceServiceResolver.getDefaultVoice()) //
                .withMelody(config.getListeningMelody()) //
                .withListeningItem(config.getListeningItem()) //
                .withLLMTools(llmToolRegistry.getAll()) //
                .withSystemPrompt(config.getSystemPrompt());
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
        validatePersistentDialogContext(context);

        synchronized (dialogProcessors) {
            if (dialogProcessors.containsKey(context.source().getId())) {
                throw new IllegalStateException(
                        String.format("Cannot start dialog as a dialog is already started for audio source '%s'.",
                                context.source().getLabel(null)));
            }

            logger.debug("Starting a new dialog for source {} ({})", context.source().getLabel(null),
                    context.source().getId());
            DialogAudioDriver audioDriver = new DialogAudioDriver(context);
            DialogFeedbackService feedbackService = new DialogFeedbackService(context, eventPublisher);
            DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService,
                    interpretationService, speechSynthesizer, i18nProvider, this);
            dialogProcessors.put(context.source().getId(), processor);
            return processor.start();
        }
    }

    @Override
    public void stopDialog(@Nullable AudioSource source) throws IllegalStateException {
        AudioSource audioSource = (source == null) ? audioManager.getSource() : source;
        if (audioSource == null) {
            throw new IllegalStateException("Cannot stop dialog as audio source is missing.");
        }

        DialogProcessor processor;
        synchronized (dialogProcessors) {
            processor = dialogProcessors.remove(audioSource.getId());
        }

        singleDialogProcessors.values().removeIf(p -> !p.isProcessing());
        if (processor == null) {
            processor = singleDialogProcessors.remove(audioSource.getId());
        }

        if (processor != null) {
            processor.stop();
            logger.debug("Dialog stopped for source {} ({})", audioSource.getLabel(null), audioSource.getId());
        } else {
            throw new IllegalStateException(String.format(
                    "Cannot stop dialog as no dialog is started for audio source '%s'.", audioSource.getLabel(null)));
        }
    }

    @Override
    public void stopDialog(DialogContext context) throws IllegalStateException {
        stopDialog(context.source());
    }

    @Override
    public void listenAndAnswer(DialogContext context) throws IllegalStateException {
        validateLocaleCompatibility(context);

        String group = context.dialogGroup();
        if (activeDialogGroups.containsKey(group)) {
            logger.warn("Ignoring listenAndAnswer, dialog group '{}' already running", group);
            return;
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
            if (processor.isProcessing()) {
                singleDialogProcessors.put(audioSource.getId(), processor);
            }
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

    @Override
    public void registerDialog(DialogRegistration registration) throws IllegalStateException {
        synchronized (dialogRegistrationStorage) {
            if (dialogRegistrationStorage.containsKey(registration.sourceId)) {
                throw new IllegalStateException(
                        String.format("Cannot register dialog as a dialog is registered for audio source '%s'.",
                                registration.sourceId));
            }
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
                stopDialog(dialog.getContext());
            }
        }
    }

    @Override
    public List<DialogRegistration> getDialogRegistrations() {
        var list = new ArrayList<DialogRegistration>();
        Collection<@Nullable DialogRegistration> dialogRegistrations;
        synchronized (dialogRegistrationStorage) {
            dialogRegistrations = dialogRegistrationStorage.getValues();
        }
        dialogRegistrations.forEach(dr -> {
            if (dr != null) {
                dr.running = dialogProcessors.containsKey(dr.sourceId);
                list.add(dr);
            }
        });
        return list;
    }

    @Override
    public boolean onAcquireGroup(DialogContext context) {
        String group = context.dialogGroup();
        DialogContext existing = activeDialogGroups.putIfAbsent(group, context);
        if (existing == null) {
            logger.debug("Acquiring dialog group '{}'", group);
            return true;
        }
        logger.warn("Dialog group '{}' already running", group);
        return false;
    }

    @Override
    public void onReleaseGroup(DialogContext context) {
        String group = context.dialogGroup();
        logger.debug("Releasing dialog group '{}'", group);
        activeDialogGroups.remove(group, context);
    }

    @Override
    public void onBeforeDialogInterpretation(DialogContext context) {
        lastDialogContext = context;
    }

    @Override
    public void onDialogStopped(DialogContext context) {
        DialogRegistration registration;
        synchronized (dialogRegistrationStorage) {
            registration = dialogRegistrationStorage.get(context.source().getId());
        }
        if (registration != null) {
            scheduleDialogRegistrations();
        }
    }

    private void stopDialogs(Predicate<DialogProcessor> filter) {
        List<DialogProcessor> persistentToStop;
        synchronized (dialogProcessors) {
            persistentToStop = dialogProcessors.values().stream().filter(filter).toList();
            for (var dialog : persistentToStop) {
                dialogProcessors.remove(dialog.getContext().source().getId(), dialog);
            }
        }
        persistentToStop.forEach(DialogProcessor::stop);

        List<DialogProcessor> singleToStop;
        synchronized (singleDialogProcessors) {
            singleToStop = singleDialogProcessors.values().stream().filter(filter).toList();
            for (var dialog : singleToStop) {
                singleDialogProcessors.remove(dialog.getContext().source().getId(), dialog);
            }
        }
        singleToStop.forEach(DialogProcessor::stop);
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
                                .withKS(voiceServiceResolver.getKS(dr.ksId)) //
                                .withKeyword(dr.keyword) //
                                .withSTT(voiceServiceResolver.getSTT(dr.sttId)) //
                                .withTTS(voiceServiceResolver.getTTS(dr.ttsId)) //
                                .withVoice(voiceServiceResolver.getVoice(dr.voiceId)) //
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

    private static void validatePersistentDialogContext(DialogContext context) throws IllegalStateException {
        final var dtService = getDtService(context);

        if ((dtService instanceof KSService ksService)
                && !checkLocales(ksService.getSupportedLocales(), context.locale())) {
            throw new IllegalStateException(
                    "Cannot start dialog as provided locale is not supported by KS service " + ksService.getUID());
        }
        validateLocaleCompatibility(context);
    }

    private static void validateLocaleCompatibility(DialogContext context) throws IllegalStateException {
        if (!checkLocales(context.stt().getSupportedLocales(), context.locale())) {
            throw new IllegalStateException(
                    "Cannot start dialog as provided locale is not supported by STT service " + context.stt().getUID());
        }
        if (!context.hlis().stream()
                .allMatch(interpreter -> checkLocales(interpreter.getSupportedLocales(), context.locale()))) {
            throw new IllegalStateException(
                    "Cannot start dialog as provided locale is not supported by all HLI services.");
        }
        if (context.voice() != null
                && !context.voice().getLocale().getLanguage().equals(context.locale().getLanguage())) {
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
            return Objects.equals(sLocale.getLanguage(), locale.getLanguage())
                    && (country.isBlank() || country.equals(locale.getCountry()));
        });
    }
}
