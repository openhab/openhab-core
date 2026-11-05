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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigOptionProvider;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.voice.dialog.trigger.ks.KSService;
import org.openhab.core.voice.dialog.trigger.ks.KSServiceRegistry;
import org.openhab.core.voice.stt.STTService;
import org.openhab.core.voice.stt.STTServiceRegistry;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.HumanLanguageInterpreterRegistry;
import org.openhab.core.voice.tts.TTSService;
import org.openhab.core.voice.tts.TTSServiceRegistry;
import org.openhab.core.voice.tts.Voice;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for resolving configured default and identifier-based voice services (TTS, STT, KS, HLI, Voice)
 * from their respective registries.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(service = VoiceServiceResolver.class, configurationPid = VoiceConfigurationConstants.CONFIGURATION_PID)
@NonNullByDefault
public class VoiceServiceResolver implements ConfigOptionProvider {

    private final Logger logger = LoggerFactory.getLogger(VoiceServiceResolver.class);

    private final TTSServiceRegistry ttsRegistry;
    private final STTServiceRegistry sttRegistry;
    private final KSServiceRegistry ksRegistry;
    private final HumanLanguageInterpreterRegistry hliRegistry;
    private final LocaleProvider localeProvider;
    private final VoiceManagerConfiguration configuration;

    @Activate
    public VoiceServiceResolver(final @Reference TTSServiceRegistry ttsRegistry,
            final @Reference STTServiceRegistry sttRegistry, final @Reference KSServiceRegistry ksRegistry,
            final @Reference HumanLanguageInterpreterRegistry hliRegistry,
            final @Reference LocaleProvider localeProvider,
            final @Reference ConfigDescriptionRegistry configDescriptionRegistry) {
        this.ttsRegistry = ttsRegistry;
        this.sttRegistry = sttRegistry;
        this.ksRegistry = ksRegistry;
        this.hliRegistry = hliRegistry;
        this.localeProvider = localeProvider;
        this.configuration = new VoiceManagerConfiguration(configDescriptionRegistry);
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        modified(config);
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        synchronized (this) {
            configuration.update(config);
        }
    }

    public VoiceManagerConfiguration getConfiguration() {
        return configuration;
    }

    public @Nullable TTSService getTTS() {
        String defaultTTS = configuration.getDefaultTTS();
        if (defaultTTS != null) {
            TTSService tts = ttsRegistry.get(defaultTTS);
            if (tts == null) {
                logger.warn("Default TTS service '{}' not available!", defaultTTS);
            }
            return tts;
        }
        TTSService tts = ttsRegistry.stream().findFirst().orElse(null);
        if (tts == null) {
            logger.debug("No TTS service available!");
        }
        return tts;
    }

    public @Nullable TTSService getTTS(@Nullable String id) {
        return id == null ? null : ttsRegistry.get(id);
    }

    public @Nullable TTSService getTTS(Voice voice) {
        return getTTS(voice.getUID().split(":")[0]);
    }

    public Collection<TTSService> getTTSs() {
        return ttsRegistry.getAll();
    }

    public @Nullable STTService getSTT() {
        String defaultSTT = configuration.getDefaultSTT();
        if (defaultSTT != null) {
            STTService stt = sttRegistry.get(defaultSTT);
            if (stt == null) {
                logger.warn("Default STT service '{}' not available!", defaultSTT);
            }
            return stt;
        }
        STTService stt = sttRegistry.stream().findFirst().orElse(null);
        if (stt == null) {
            logger.debug("No STT service available!");
        }
        return stt;
    }

    public @Nullable STTService getSTT(@Nullable String id) {
        return id == null ? null : sttRegistry.get(id);
    }

    public Collection<STTService> getSTTs() {
        return sttRegistry.getAll();
    }

    public @Nullable KSService getKS() {
        String defaultKS = configuration.getDefaultKS();
        if (defaultKS != null) {
            KSService ks = ksRegistry.get(defaultKS);
            if (ks == null) {
                logger.warn("Default KS service '{}' not available!", defaultKS);
            }
            return ks;
        }
        KSService ks = ksRegistry.stream().findFirst().orElse(null);
        if (ks == null) {
            logger.debug("No KS service available!");
        }
        return ks;
    }

    public @Nullable KSService getKS(@Nullable String id) {
        return id == null ? null : ksRegistry.get(id);
    }

    public Collection<KSService> getKSs() {
        return ksRegistry.getAll();
    }

    public @Nullable HumanLanguageInterpreter getHLI() {
        String defaultHLI = configuration.getDefaultHLI();
        if (defaultHLI != null) {
            HumanLanguageInterpreter hli = hliRegistry.get(defaultHLI);
            if (hli == null) {
                logger.warn("Default HumanLanguageInterpreter '{}' not available!", defaultHLI);
            }
            return hli;
        }
        HumanLanguageInterpreter hli = hliRegistry.stream().findFirst().orElse(null);
        if (hli == null) {
            logger.debug("No HumanLanguageInterpreter available!");
        }
        return hli;
    }

    public @Nullable HumanLanguageInterpreter getHLI(@Nullable String id) {
        return id == null ? null : hliRegistry.get(id);
    }

    public List<HumanLanguageInterpreter> getHLIsByIds(@Nullable String ids) {
        return hliRegistry.getByIds(ids);
    }

    public List<HumanLanguageInterpreter> getHLIsByIds(List<String> ids) {
        return hliRegistry.getByIds(ids);
    }

    public Collection<HumanLanguageInterpreter> getHLIs() {
        return hliRegistry.getAll();
    }

    public @Nullable Voice getDefaultVoice() {
        String localDefaultVoice = configuration.getDefaultVoice();
        return localDefaultVoice != null ? getVoice(localDefaultVoice) : null;
    }

    public @Nullable Voice getVoice(@Nullable String id) {
        if (id == null) {
            return null;
        } else if (id.contains(":")) {
            // id is a fully qualified unique id
            String[] segments = id.split(":");
            TTSService tts = getTTS(segments[0]);
            if (tts != null) {
                return getVoice(tts.getAvailableVoices(), segments[1]);
            }
        } else {
            // id is not fully qualified
            TTSService tts = getTTS();
            if (tts != null) {
                return getVoice(tts.getAvailableVoices(), id);
            }
        }
        return null;
    }

    public @Nullable Voice getVoice(Set<Voice> voices, String id) {
        for (Voice voice : voices) {
            if (voice.getUID().endsWith(":" + id)) {
                return voice;
            }
        }
        return null;
    }

    public Set<Voice> getAllVoices() {
        return getAllVoices(localeProvider.getLocale());
    }

    public Set<Voice> getAllVoices(Locale locale) {
        return ttsRegistry.stream().map(TTSService::getAvailableVoices).flatMap(Collection::stream)
                .sorted(createVoiceComparator(locale)).collect(Collectors
                        .collectingAndThen(Collectors.toCollection(LinkedHashSet::new), Collections::unmodifiableSet));
    }

    public @Nullable Voice getPreferredVoice(Set<Voice> voices) {
        Locale locale = localeProvider.getLocale();
        Collection<Locale> locales = new ArrayList<>();
        for (Voice currentVoice : voices) {
            locales.add(currentVoice.getLocale());
        }

        String ranges = locale.toLanguageTag();
        List<Locale.LanguageRange> languageRanges = Locale.LanguageRange.parse(ranges + "-*");
        Locale preferredLocale = Locale.lookup(languageRanges, locales);

        if (preferredLocale == null && !voices.isEmpty()) {
            preferredLocale = locales.iterator().next();
        }
        if (preferredLocale == null) {
            return null;
        }

        Voice preferredVoice = null;
        for (Voice currentVoice : voices) {
            if (currentVoice.getLocale().equals(preferredLocale)) {
                preferredVoice = currentVoice;
                break;
            }
        }
        return preferredVoice;
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
                    return getAllVoices(nullSafeLocale)
                            .stream().filter(v -> getTTS(v) != null).map(
                                    v -> new ParameterOption(v.getUID(),
                                            String.format("%s - %s - %s", getTTS(v).getLabel(nullSafeLocale),
                                                    v.getLocale().getDisplayName(nullSafeLocale), v.getLabel())))
                            .toList();
            }
        }
        return null;
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
}
