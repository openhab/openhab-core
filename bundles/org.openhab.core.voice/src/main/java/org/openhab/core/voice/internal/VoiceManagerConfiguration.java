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

import static org.openhab.core.voice.internal.VoiceConfigurationConstants.CONFIGURATION_PID;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.config.core.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link VoiceManagerConfiguration} class holds the configuration for the {@link VoiceManagerImpl}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class VoiceManagerConfiguration {
    private final Logger logger = LoggerFactory.getLogger(VoiceManagerConfiguration.class);
    private final ConfigDescriptionRegistry configDescriptionRegistry;

    private ConfigurationDTO configurationDTO = new ConfigurationDTO();
    private final Map<String, String> defaultVoices = new HashMap<>();

    public VoiceManagerConfiguration(final ConfigDescriptionRegistry configDescriptionRegistry) {
        this.configDescriptionRegistry = configDescriptionRegistry;
    }

    public void update(Map<String, Object> config) {
        Map<String, @Nullable Object> properties = new HashMap<>(config);

        var configDescription = configDescriptionRegistry
                .getConfigDescription(URI.create(VoiceConfigurationConstants.CONFIG_URI));
        if (configDescription == null) {
            logger.warn("No configuration description found for {}, unable to apply defaults!",
                    VoiceConfigurationConstants.CONFIG_URI);
        } else {
            ConfigUtil.applyDefaultConfiguration(properties, configDescription);
        }

        ConfigurationDTO configDTO = ConfigParser.configurationAs(properties, ConfigurationDTO.class);
        if (configDTO == null) {
            logger.error("Unable to parse configuration for {}!", CONFIGURATION_PID);
            return;
        }
        configurationDTO = configDTO;

        defaultVoices.clear();
        for (Entry<String, Object> entry : config.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(VoiceConfigurationConstants.CONFIG_PREFIX_DEFAULT_VOICE)) {
                String tts = key.substring(VoiceConfigurationConstants.CONFIG_PREFIX_DEFAULT_VOICE.length());
                defaultVoices.put(tts, entry.getValue().toString());
            }
        }
    }

    public String getKeyword() {
        return configurationDTO.keyword;
    }

    public @Nullable String getListeningItem() {
        return configurationDTO.listeningItem;
    }

    public @Nullable String getListeningMelody() {
        return configurationDTO.listeningMelody;
    }

    public @Nullable String getDefaultTTS() {
        return configurationDTO.defaultTTS;
    }

    public @Nullable String getDefaultSTT() {
        return configurationDTO.defaultSTT;
    }

    public @Nullable String getDefaultKS() {
        return configurationDTO.defaultKS;
    }

    public @Nullable String getDefaultHLI() {
        return configurationDTO.defaultHLI;
    }

    public @Nullable String getDefaultVoice() {
        return configurationDTO.defaultVoice;
    }

    public String getSystemPrompt() {
        return configurationDTO.systemPrompt;
    }

    public Map<String, String> getDefaultVoices() {
        return Map.copyOf(defaultVoices);
    }

    // Note: Default values are set here, because due to initialization order and missing listeners support in
    // ConfigDescriptionRegistry, default values aren't applied on initial activation.
    public static class ConfigurationDTO {
        public @Nullable String defaultTTS;
        public @Nullable String defaultSTT;
        public @Nullable String defaultVoice;
        public @Nullable String defaultHLI;
        public @Nullable String defaultKS;
        public String keyword = "Wakeup";
        public @Nullable String listeningItem;
        public @Nullable String listeningMelody;
        public String systemPrompt = """
                You are a smart home assistant managing an openHAB installation. Your job is to help users control devices, report their status, and answer general questions conversationally.
                - For device control or status requests, use only the tools provided. Never infer or simulate a device action — if the right tool or device isn't available, say so briefly.
                - If a request is ambiguous (e.g. the room or device isn't clear), ask one short clarifying question. Do not list all available devices or parameters.
                - For non-smart-home questions, answer concisely in plain, everyday language — 2–3 sentences maximum.
                - Always respond in the same language the user used.
                - Keep all responses short and natural, as they may be read aloud by a voice assistant.
                """;
    }
}
