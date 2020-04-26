/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import static java.util.stream.Collectors.*;

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
import org.openhab.core.library.types.PercentType;
import org.openhab.core.voice.KSService;
import org.openhab.core.voice.STTService;
import org.openhab.core.voice.TTSException;
import org.openhab.core.voice.TTSService;
import org.openhab.core.voice.Voice;
import org.openhab.core.voice.VoiceManager;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.InterpretationException;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
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
 */
@Component(immediate = true, configurationPid = VoiceManagerImpl.CONFIGURATION_PID, property = { //
        Constants.SERVICE_PID + "=org.openhab.voice", //
        ConfigurableService.SERVICE_PROPERTY_CATEGORY + "=system", //
        ConfigurableService.SERVICE_PROPERTY_LABEL + "=Voice", //
        ConfigurableService.SERVICE_PROPERTY_DESCRIPTION_URI + "=" + VoiceManagerImpl.CONFIG_URI //
})
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

    /**
     * default settings filled through the service configuration
     */
    private String keyword = DEFAULT_KEYWORD;
    private String listeningItem;
    private String defaultTTS;
    private String defaultSTT;
    private String defaultKS;
    private String defaultHLI;
    private String defaultVoice;
    private final Map<String, String> defaultVoices = new HashMap<>();

    @Activate
    public VoiceManagerImpl(final @Reference LocaleProvider localeProvider, final @Reference AudioManager audioManager,
            final @Reference EventPublisher eventPublisher) {
        this.localeProvider = localeProvider;
        this.audioManager = audioManager;
        this.eventPublisher = eventPublisher;
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        modified(config);
    }

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
    public void say(String text, PercentType volume) {
        say(text, null, null, volume);
    }

    @Override
    public void say(String text, String voiceId) {
        say(text, voiceId, null, null);
    }

    @Override
    public void say(String text, String voiceId, PercentType volume) {
        say(text, voiceId, null, volume);
    }

    @Override
    public void say(String text, String voiceId, String sinkId) {
        say(text, voiceId, sinkId, null);
    }

    @Override
    public void say(String text, String voiceId, String sinkId, PercentType volume) {
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
            logger.warn("Error saying '{}': {}", text, e.getMessage(), e);
        }
    }

    @Override
    public String interpret(String text) throws InterpretationException {
        return interpret(text, null);
    }

    @Override
    public String interpret(String text, String hliId) throws InterpretationException {
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

    private Voice getVoice(String id) {
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

    private Voice getVoice(Set<Voice> voices, String id) {
        for (Voice voice : voices) {
            if (voice.getUID().endsWith(":" + id)) {
                return voice;
            }
        }
        return null;
    }

    public static AudioFormat getPreferredFormat(Set<AudioFormat> audioFormats) {
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

    public static AudioFormat getBestMatch(Set<AudioFormat> inputs, Set<AudioFormat> outputs) {
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
    public Voice getPreferredVoice(Set<Voice> voices) {
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
        if (preferredLocale == null) {
            preferredLocale = locales.iterator().next();
        }

        // Determine preferred voice
        Voice preferredVoice = null;
        for (Voice currentVoice : voices) {
            if (preferredLocale.equals(currentVoice.getLocale())) {
                preferredVoice = currentVoice;
            }
        }
        assert (preferredVoice != null);

        // Return preferred voice
        return preferredVoice;
    }

    @Override
    public void startDialog() {
        startDialog(null, null, null, null, null, null, null, this.keyword, this.listeningItem);
    }

    @Override
    public void startDialog(KSService ksService, STTService sttService, TTSService ttsService,
            HumanLanguageInterpreter interpreter, AudioSource audioSource, AudioSink audioSink, Locale locale,
            String keyword, String listeningItem) {
        // use defaults, if null
        KSService ks = (ksService == null) ? getKS() : ksService;
        STTService stt = (sttService == null) ? getSTT() : sttService;
        TTSService tts = (ttsService == null) ? getTTS() : ttsService;
        HumanLanguageInterpreter hli = (interpreter == null) ? getHLI() : interpreter;
        AudioSource source = (audioSource == null) ? audioManager.getSource() : audioSource;
        AudioSink sink = (audioSink == null) ? audioManager.getSink() : audioSink;
        Locale loc = (locale == null) ? localeProvider.getLocale() : locale;
        String kw = (keyword == null) ? this.keyword : keyword;
        String item = (listeningItem == null) ? this.listeningItem : listeningItem;

        if (ks != null && stt != null && tts != null && hli != null && source != null && sink != null && loc != null
                && kw != null) {
            DialogProcessor processor = new DialogProcessor(ks, stt, tts, hli, source, sink, loc, kw, item,
                    this.eventPublisher);
            processor.start();
        } else {
            String msg = "Cannot start dialog as services are missing.";
            logger.error(msg);
            throw new IllegalStateException(msg);
        }
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
    public TTSService getTTS() {
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
    public TTSService getTTS(String id) {
        return ttsServices.get(id);
    }

    private TTSService getTTS(Voice voice) {
        return getTTS(voice.getUID().split(":")[0]);
    }

    @Override
    public Collection<TTSService> getTTSs() {
        return new HashSet<>(ttsServices.values());
    }

    @Override
    public STTService getSTT() {
        STTService stt = null;
        if (defaultTTS != null) {
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
    public STTService getSTT(String id) {
        return sttServices.get(id);
    }

    @Override
    public Collection<STTService> getSTTs() {
        return new HashSet<>(sttServices.values());
    }

    @Override
    public KSService getKS() {
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
    public KSService getKS(String id) {
        return ksServices.get(id);
    }

    @Override
    public Collection<KSService> getKSs() {
        return new HashSet<>(ksServices.values());
    }

    @Override
    public HumanLanguageInterpreter getHLI() {
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
    public HumanLanguageInterpreter getHLI(String id) {
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
                .sorted(createVoiceComparator(locale))
                .collect(collectingAndThen(toCollection(LinkedHashSet::new), Collections::unmodifiableSet));
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
            return getTTS(v1).getLabel(locale).compareToIgnoreCase(getTTS(v2).getLabel(locale));
        };
        Comparator<Voice> byVoiceLocale = (Voice v1, Voice v2) -> {
            return v1.getLocale().getDisplayName(locale).compareToIgnoreCase(v2.getLocale().getDisplayName(locale));
        };
        return byTTSLabel.thenComparing(byVoiceLocale).thenComparing(Voice::getLabel);
    }

    @Override
    public @Nullable Voice getDefaultVoice() {
        return defaultVoice != null ? getVoice(defaultVoice) : null;
    }

    @Override
    public Collection<ParameterOption> getParameterOptions(URI uri, String param, Locale locale) {
        if (CONFIG_URI.equals(uri.toString())) {
            if (CONFIG_DEFAULT_HLI.equals(param)) {
                return humanLanguageInterpreters.values().stream()
                        .sorted((hli1, hli2) -> hli1.getLabel(locale).compareToIgnoreCase(hli2.getLabel(locale)))
                        .map(hli -> new ParameterOption(hli.getId(), hli.getLabel(locale))).collect(toList());
            } else if (CONFIG_DEFAULT_KS.equals(param)) {
                return ksServices.values().stream()
                        .sorted((ks1, ks2) -> ks1.getLabel(locale).compareToIgnoreCase(ks2.getLabel(locale)))
                        .map(ks -> new ParameterOption(ks.getId(), ks.getLabel(locale))).collect(toList());
            } else if (CONFIG_DEFAULT_STT.equals(param)) {
                return sttServices.values().stream()
                        .sorted((stt1, stt2) -> stt1.getLabel(locale).compareToIgnoreCase(stt2.getLabel(locale)))
                        .map(stt -> new ParameterOption(stt.getId(), stt.getLabel(locale))).collect(toList());
            } else if (CONFIG_DEFAULT_TTS.equals(param)) {
                return ttsServices.values().stream()
                        .sorted((tts1, tts2) -> tts1.getLabel(locale).compareToIgnoreCase(tts2.getLabel(locale)))
                        .map(tts -> new ParameterOption(tts.getId(), tts.getLabel(locale))).collect(toList());
            } else if (CONFIG_DEFAULT_VOICE.equals(param)) {
                Locale nullSafeLocale = locale != null ? locale : localeProvider.getLocale();
                return getAllVoicesSorted(nullSafeLocale)
                        .stream().filter(v -> getTTS(v) != null).map(
                                v -> new ParameterOption(v.getUID(),
                                        String.format("%s - %s - %s", getTTS(v).getLabel(nullSafeLocale),
                                                v.getLocale().getDisplayName(nullSafeLocale), v.getLabel())))
                        .collect(toList());
            }
        }
        return null;
    }
}
