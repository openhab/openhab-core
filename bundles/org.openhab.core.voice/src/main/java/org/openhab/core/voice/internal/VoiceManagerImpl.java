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
package org.openhab.core.voice.internal;

import java.io.IOException;
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
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.UnsupportedAudioFormatException;
import org.openhab.core.audio.UnsupportedAudioStreamException;
import org.openhab.core.config.core.ConfigOptionProvider;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.voice.KSService;
import org.openhab.core.voice.STTService;
import org.openhab.core.voice.TTSException;
import org.openhab.core.voice.TTSService;
import org.openhab.core.voice.Voice;
import org.openhab.core.voice.VoiceManager;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.InterpretationException;
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
 * @author Yannick Schaus - Added ability to provide a item for feedback during listening phases
 * @author Christoph Weitkamp - Added getSupportedStreams() and UnsupportedAudioStreamException
 * @author Christoph Weitkamp - Added parameter to adjust the volume
 * @author Wouter Born - Sort TTS options
 * @author Laurent Garnier - Updated methods startDialog and added method stopDialog
 */
@Component(immediate = true, configurationPid = VoiceManagerImpl.CONFIGURATION_PID, //
        property = Constants.SERVICE_PID + "=org.openhab.voice")
@ConfigurableService(category = "system", label = "Voice", description_uri = VoiceManagerImpl.CONFIG_URI)
@NonNullByDefault
public class VoiceManagerImpl implements VoiceManager, ConfigOptionProvider {

    public static final String CONFIGURATION_PID = "org.openhab.voice";

    // the default keyword to use if no other is configured
    private static final String DEFAULT_KEYWORD = "Wakeup";

    // constants for the configuration properties
    protected static final String CONFIG_URI = "system:voice";
    private static final String CONFIG_KEYWORD = "keyword";
    private static final String CONFIG_LISTENING_ITEM = "listeningItem";
    private static final String CONFIG_DEFAULT_HLI = "defaultHLI";
    private static final String CONFIG_DEFAULT_KS = "defaultKS";
    private static final String CONFIG_DEFAULT_STT = "defaultSTT";
    private static final String CONFIG_DEFAULT_TTS = "defaultTTS";
    private static final String CONFIG_DEFAULT_VOICE = "defaultVoice";
    private static final String CONFIG_PREFIX_DEFAULT_VOICE = "defaultVoice.";

    private final Logger logger = LoggerFactory.getLogger(VoiceManagerImpl.class);

    // service maps
    private final Map<String, KSService> ksServices = new HashMap<>();
    private final Map<String, STTService> sttServices = new HashMap<>();
    private final Map<String, TTSService> ttsServices = new HashMap<>();
    private final Map<String, HumanLanguageInterpreter> humanLanguageInterpreters = new HashMap<>();

    private final LocaleProvider localeProvider;
    private final AudioManager audioManager;
    private final EventPublisher eventPublisher;
    private final TranslationProvider i18nProvider;

    private @Nullable Bundle bundle;

    /**
     * default settings filled through the service configuration
     */
    private String keyword = DEFAULT_KEYWORD;
    private @Nullable String listeningItem;
    private @Nullable String defaultTTS;
    private @Nullable String defaultSTT;
    private @Nullable String defaultKS;
    private @Nullable String defaultHLI;
    private @Nullable String defaultVoice;
    private final Map<String, String> defaultVoices = new HashMap<>();

    private Map<String, DialogProcessor> dialogProcessors = new HashMap<>();

    @Activate
    public VoiceManagerImpl(final @Reference LocaleProvider localeProvider, final @Reference AudioManager audioManager,
            final @Reference EventPublisher eventPublisher, final @Reference TranslationProvider i18nProvider) {
        this.localeProvider = localeProvider;
        this.audioManager = audioManager;
        this.eventPublisher = eventPublisher;
        this.i18nProvider = i18nProvider;
    }

    @Activate
    protected void activate(BundleContext bundleContext, Map<String, Object> config) {
        this.bundle = bundleContext.getBundle();
        modified(config);
    }

    @Deactivate
    protected void deactivate() {
        stopAllDialogs();
    }

    @SuppressWarnings("null")
    @Modified
    protected void modified(Map<String, Object> config) {
        if (config != null) {
            this.keyword = config.containsKey(CONFIG_KEYWORD) ? config.get(CONFIG_KEYWORD).toString() : DEFAULT_KEYWORD;
            this.listeningItem = config.containsKey(CONFIG_LISTENING_ITEM)
                    ? config.get(CONFIG_LISTENING_ITEM).toString()
                    : null;
            this.defaultTTS = config.containsKey(CONFIG_DEFAULT_TTS) ? config.get(CONFIG_DEFAULT_TTS).toString() : null;
            this.defaultSTT = config.containsKey(CONFIG_DEFAULT_STT) ? config.get(CONFIG_DEFAULT_STT).toString() : null;
            this.defaultKS = config.containsKey(CONFIG_DEFAULT_KS) ? config.get(CONFIG_DEFAULT_KS).toString() : null;
            this.defaultHLI = config.containsKey(CONFIG_DEFAULT_HLI) ? config.get(CONFIG_DEFAULT_HLI).toString() : null;
            this.defaultVoice = config.containsKey(CONFIG_DEFAULT_VOICE) ? config.get(CONFIG_DEFAULT_VOICE).toString()
                    : null;

            for (String key : config.keySet()) {
                if (key.startsWith(CONFIG_PREFIX_DEFAULT_VOICE)) {
                    String tts = key.substring(CONFIG_PREFIX_DEFAULT_VOICE.length());
                    defaultVoices.put(tts, config.get(key).toString());
                }
            }
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
                selectedVoiceId = defaultVoice;
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
            Set<AudioFormat> audioFormats = tts.getSupportedFormats();
            AudioSink sink = audioManager.getSink(sinkId);
            if (sink == null) {
                throw new TTSException("Unable to find the audio sink " + sinkId);
            }

            AudioFormat audioFormat = getBestMatch(audioFormats, sink.getSupportedFormats());
            if (audioFormat == null) {
                throw new TTSException("No compatible audio format found for TTS '" + tts.getId() + "' and sink '"
                        + sink.getId() + "'");
            }

            AudioStream audioStream = tts.synthesize(text, voice, audioFormat);
            if (!sink.getSupportedStreams().stream().anyMatch(clazz -> clazz.isInstance(audioStream))) {
                throw new TTSException(
                        "Failed playing audio stream '" + audioStream + "' as audio sink doesn't support it");
            }

            PercentType oldVolume = null;
            try {
                // get current volume
                oldVolume = audioManager.getVolume(sinkId);
            } catch (IOException e) {
                logger.debug("An exception occurred while getting the volume of sink '{}' : {}", sink.getId(),
                        e.getMessage(), e);
            }
            // set notification sound volume
            if (volume != null) {
                try {
                    audioManager.setVolume(volume, sinkId);
                } catch (IOException e) {
                    logger.debug("An exception occurred while setting the volume of sink '{}' : {}", sink.getId(),
                            e.getMessage(), e);
                }
            }
            try {
                sink.process(audioStream);
            } finally {
                if (volume != null && oldVolume != null) {
                    // restore volume only if it was set before
                    try {
                        audioManager.setVolume(oldVolume, sinkId);
                    } catch (IOException e) {
                        logger.debug("An exception occurred while setting the volume of sink '{}' : {}", sink.getId(),
                                e.getMessage(), e);
                    }
                }
            }
        } catch (TTSException | UnsupportedAudioFormatException | UnsupportedAudioStreamException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error saying '{}': {}", text, e.getMessage(), e);
            } else {
                logger.warn("Error saying '{}': {}", text, e.getMessage());
            }
        }
    }

    @Override
    public String interpret(String text) throws InterpretationException {
        return interpret(text, null);
    }

    @Override
    public String interpret(String text, @Nullable String hliId) throws InterpretationException {
        HumanLanguageInterpreter interpreter;
        if (hliId == null) {
            interpreter = getHLI();
            if (interpreter == null) {
                throw new InterpretationException("No human language interpreter available!");
            }
        } else {
            interpreter = getHLI(hliId);
            if (interpreter == null) {
                throw new InterpretationException("No human language interpreter can be found for " + hliId);
            }
        }
        return interpreter.interpret(localeProvider.getLocale(), text);
    }

    private @Nullable Voice getVoice(String id) {
        if (id.contains(":")) {
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

    public static @Nullable AudioFormat getPreferredFormat(Set<AudioFormat> audioFormats) {
        // Return the first concrete AudioFormat found
        for (AudioFormat currentAudioFormat : audioFormats) {
            // Check if currentAudioFormat is abstract
            if (null == currentAudioFormat.getCodec()) {
                continue;
            }
            if (null == currentAudioFormat.getContainer()) {
                continue;
            }
            if (null == currentAudioFormat.isBigEndian()) {
                continue;
            }
            if (null == currentAudioFormat.getBitDepth()) {
                continue;
            }
            if (null == currentAudioFormat.getBitRate()) {
                continue;
            }
            if (null == currentAudioFormat.getFrequency()) {
                continue;
            }

            // Prefer WAVE container
            if (!AudioFormat.CONTAINER_WAVE.equals(currentAudioFormat.getContainer())) {
                continue;
            }

            // As currentAudioFormat is concrete, use it
            return currentAudioFormat;
        }

        // There's no concrete AudioFormat so we must create one
        for (AudioFormat currentAudioFormat : audioFormats) {
            // Define AudioFormat to return
            AudioFormat format = currentAudioFormat;

            // Not all Codecs and containers can be supported
            if (null == format.getCodec()) {
                continue;
            }
            if (null == format.getContainer()) {
                continue;
            }

            // Prefer WAVE container
            if (!AudioFormat.CONTAINER_WAVE.equals(format.getContainer())) {
                continue;
            }

            // If required set BigEndian, BitDepth, BitRate, and Frequency to default values
            if (null == format.isBigEndian()) {
                format = new AudioFormat(format.getContainer(), format.getCodec(), Boolean.TRUE, format.getBitDepth(),
                        format.getBitRate(), format.getFrequency());
            }
            if (null == format.getBitDepth() || null == format.getBitRate() || null == format.getFrequency()) {
                // Define default values
                int defaultBitDepth = 16;
                long defaultFrequency = 44100;

                // Obtain current values
                Integer bitRate = format.getBitRate();
                Long frequency = format.getFrequency();
                Integer bitDepth = format.getBitDepth();

                // These values must be interdependent (bitRate = bitDepth * frequency)
                if (null == bitRate) {
                    if (null == bitDepth) {
                        bitDepth = Integer.valueOf(defaultBitDepth);
                    }
                    if (null == frequency) {
                        frequency = Long.valueOf(defaultFrequency);
                    }
                    bitRate = Integer.valueOf(bitDepth.intValue() * frequency.intValue());
                } else if (null == bitDepth) {
                    if (null == frequency) {
                        frequency = Long.valueOf(defaultFrequency);
                    }
                    bitDepth = Integer.valueOf(bitRate.intValue() / frequency.intValue());
                } else if (null == frequency) {
                    frequency = Long.valueOf(bitRate.longValue() / bitDepth.longValue());
                }

                format = new AudioFormat(format.getContainer(), format.getCodec(), format.isBigEndian(), bitDepth,
                        bitRate, frequency);
            }

            // Return preferred AudioFormat
            return format;
        }

        // Return null indicating failure
        return null;
    }

    public static @Nullable AudioFormat getBestMatch(Set<AudioFormat> inputs, Set<AudioFormat> outputs) {
        AudioFormat preferredFormat = getPreferredFormat(inputs);
        for (AudioFormat output : outputs) {
            if (output.isCompatible(preferredFormat)) {
                return preferredFormat;
            } else {
                for (AudioFormat input : inputs) {
                    if (output.isCompatible(input)) {
                        return input;
                    }
                }
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
    public void startDialog() throws IllegalStateException {
        startDialog(null, null, null, null, null, null, null, this.keyword, this.listeningItem);
    }

    @Override
    public void startDialog(@Nullable KSService ks, @Nullable STTService stt, @Nullable TTSService tts,
            @Nullable HumanLanguageInterpreter hli, @Nullable AudioSource source, @Nullable AudioSink sink,
            @Nullable Locale locale, @Nullable String keyword, @Nullable String listeningItem)
            throws IllegalStateException {
        // use defaults, if null
        KSService ksService = (ks == null) ? getKS() : ks;
        STTService sttService = (stt == null) ? getSTT() : stt;
        TTSService ttsService = (tts == null) ? getTTS() : tts;
        HumanLanguageInterpreter interpreter = (hli == null) ? getHLI() : hli;
        AudioSource audioSource = (source == null) ? audioManager.getSource() : source;
        AudioSink audioSink = (sink == null) ? audioManager.getSink() : sink;
        Locale loc = (locale == null) ? localeProvider.getLocale() : locale;
        String kw = (keyword == null) ? this.keyword : keyword;
        String item = (listeningItem == null) ? this.listeningItem : listeningItem;
        Bundle b = bundle;

        if (ksService != null && sttService != null && ttsService != null && interpreter != null && audioSource != null
                && audioSink != null && b != null) {
            DialogProcessor processor = dialogProcessors.get(audioSource.getId());
            if (processor == null) {
                logger.debug("Starting a new dialog for source {} ({})", audioSource.getLabel(null),
                        audioSource.getId());
                processor = new DialogProcessor(ksService, sttService, ttsService, interpreter, audioSource, audioSink,
                        loc, kw, item, this.eventPublisher, this.i18nProvider, b);
                dialogProcessors.put(audioSource.getId(), processor);
                processor.start();
            } else {
                throw new IllegalStateException(
                        String.format("Cannot start dialog as a dialog is already started for audio source '%s'.",
                                audioSource.getLabel(null)));
            }
        } else {
            throw new IllegalStateException("Cannot start dialog as services are missing.");
        }
    }

    @Override
    public void stopDialog(@Nullable AudioSource source) throws IllegalStateException {
        AudioSource audioSource = (source == null) ? audioManager.getSource() : source;
        if (audioSource != null) {
            DialogProcessor processor = dialogProcessors.remove(audioSource.getId());
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

    private void stopAllDialogs() {
        dialogProcessors.values().forEach(processor -> {
            processor.stop();
        });
        dialogProcessors.clear();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addKSService(KSService ksService) {
        this.ksServices.put(ksService.getId(), ksService);
    }

    protected void removeKSService(KSService ksService) {
        this.ksServices.remove(ksService.getId());
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addSTTService(STTService sttService) {
        this.sttServices.put(sttService.getId(), sttService);
    }

    protected void removeSTTService(STTService sttService) {
        this.sttServices.remove(sttService.getId());
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addTTSService(TTSService ttsService) {
        this.ttsServices.put(ttsService.getId(), ttsService);
    }

    protected void removeTTSService(TTSService ttsService) {
        this.ttsServices.remove(ttsService.getId());
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addHumanLanguageInterpreter(HumanLanguageInterpreter humanLanguageInterpreter) {
        this.humanLanguageInterpreters.put(humanLanguageInterpreter.getId(), humanLanguageInterpreter);
    }

    protected void removeHumanLanguageInterpreter(HumanLanguageInterpreter humanLanguageInterpreter) {
        this.humanLanguageInterpreters.remove(humanLanguageInterpreter.getId());
    }

    @Override
    public @Nullable TTSService getTTS() {
        TTSService tts = null;
        if (defaultTTS != null) {
            tts = ttsServices.get(defaultTTS);
            if (tts == null) {
                logger.warn("Default TTS service '{}' not available!", defaultTTS);
            }
        } else if (!ttsServices.isEmpty()) {
            tts = ttsServices.values().iterator().next();
        } else {
            logger.debug("No TTS service available!");
        }
        return tts;
    }

    @Override
    public @Nullable TTSService getTTS(String id) {
        return ttsServices.get(id);
    }

    private @Nullable TTSService getTTS(Voice voice) {
        return getTTS(voice.getUID().split(":")[0]);
    }

    @Override
    public Collection<TTSService> getTTSs() {
        return new HashSet<>(ttsServices.values());
    }

    @Override
    public @Nullable STTService getSTT() {
        STTService stt = null;
        if (defaultSTT != null) {
            stt = sttServices.get(defaultSTT);
            if (stt == null) {
                logger.warn("Default STT service '{}' not available!", defaultSTT);
            }
        } else if (!sttServices.isEmpty()) {
            stt = sttServices.values().iterator().next();
        } else {
            logger.debug("No STT service available!");
        }
        return stt;
    }

    @Override
    public @Nullable STTService getSTT(String id) {
        return sttServices.get(id);
    }

    @Override
    public Collection<STTService> getSTTs() {
        return new HashSet<>(sttServices.values());
    }

    @Override
    public @Nullable KSService getKS() {
        KSService ks = null;
        if (defaultKS != null) {
            ks = ksServices.get(defaultKS);
            if (ks == null) {
                logger.warn("Default KS service '{}' not available!", defaultKS);
            }
        } else if (!ksServices.isEmpty()) {
            ks = ksServices.values().iterator().next();
        } else {
            logger.debug("No KS service available!");
        }
        return ks;
    }

    @Override
    public @Nullable KSService getKS(String id) {
        return ksServices.get(id);
    }

    @Override
    public Collection<KSService> getKSs() {
        return new HashSet<>(ksServices.values());
    }

    @Override
    public @Nullable HumanLanguageInterpreter getHLI() {
        HumanLanguageInterpreter hli = null;
        if (defaultHLI != null) {
            hli = humanLanguageInterpreters.get(defaultHLI);
            if (hli == null) {
                logger.warn("Default HumanLanguageInterpreter '{}' not available!", defaultHLI);
            }
        } else if (!humanLanguageInterpreters.isEmpty()) {
            hli = humanLanguageInterpreters.values().iterator().next();
        } else {
            logger.debug("No HumanLanguageInterpreter available!");
        }
        return hli;
    }

    @Override
    public @Nullable HumanLanguageInterpreter getHLI(String id) {
        return humanLanguageInterpreters.get(id);
    }

    @Override
    public Collection<HumanLanguageInterpreter> getHLIs() {
        return new HashSet<>(humanLanguageInterpreters.values());
    }

    @Override
    public Set<Voice> getAllVoices() {
        return getAllVoicesSorted(localeProvider.getLocale());
    }

    private Set<Voice> getAllVoicesSorted(Locale locale) {
        return ttsServices.values().stream().map(s -> s.getAvailableVoices()).flatMap(Collection::stream)
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
        Comparator<Voice> byVoiceLocale = (Voice v1, Voice v2) -> {
            return v1.getLocale().getDisplayName(locale).compareToIgnoreCase(v2.getLocale().getDisplayName(locale));
        };
        return byTTSLabel.thenComparing(byVoiceLocale).thenComparing(Voice::getLabel);
    }

    @Override
    public @Nullable Voice getDefaultVoice() {
        String localDefaultVoice = defaultVoice;
        return localDefaultVoice != null ? getVoice(localDefaultVoice) : null;
    }

    @Override
    public @Nullable Collection<ParameterOption> getParameterOptions(URI uri, String param, @Nullable String context,
            @Nullable Locale locale) {
        if (CONFIG_URI.equals(uri.toString())) {
            switch (param) {
                case CONFIG_DEFAULT_HLI:
                    return humanLanguageInterpreters.values().stream()
                            .sorted((hli1, hli2) -> hli1.getLabel(locale).compareToIgnoreCase(hli2.getLabel(locale)))
                            .map(hli -> new ParameterOption(hli.getId(), hli.getLabel(locale)))
                            .collect(Collectors.toList());
                case CONFIG_DEFAULT_KS:
                    return ksServices.values().stream()
                            .sorted((ks1, ks2) -> ks1.getLabel(locale).compareToIgnoreCase(ks2.getLabel(locale)))
                            .map(ks -> new ParameterOption(ks.getId(), ks.getLabel(locale)))
                            .collect(Collectors.toList());
                case CONFIG_DEFAULT_STT:
                    return sttServices.values().stream()
                            .sorted((stt1, stt2) -> stt1.getLabel(locale).compareToIgnoreCase(stt2.getLabel(locale)))
                            .map(stt -> new ParameterOption(stt.getId(), stt.getLabel(locale)))
                            .collect(Collectors.toList());
                case CONFIG_DEFAULT_TTS:
                    return ttsServices.values().stream()
                            .sorted((tts1, tts2) -> tts1.getLabel(locale).compareToIgnoreCase(tts2.getLabel(locale)))
                            .map(tts -> new ParameterOption(tts.getId(), tts.getLabel(locale)))
                            .collect(Collectors.toList());
                case CONFIG_DEFAULT_VOICE:
                    Locale nullSafeLocale = locale != null ? locale : localeProvider.getLocale();
                    return getAllVoicesSorted(nullSafeLocale).stream().filter(v -> getTTS(v) != null)
                            .map(v -> new ParameterOption(v.getUID(),
                                    String.format("%s - %s - %s", getTTS(v).getLabel(nullSafeLocale),
                                            v.getLocale().getDisplayName(nullSafeLocale), v.getLabel())))
                            .collect(Collectors.toList());
            }
        }
        return null;
    }
}
