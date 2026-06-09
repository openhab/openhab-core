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

import static org.openhab.core.voice.internal.VoiceConfiguration.CONFIGURATION_PID;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.voice.security.ItemPermission;
import org.openhab.core.voice.text.conversation.Conversation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link VoiceConfiguration} class holds the configuration for the {@link VoiceManagerImpl}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class VoiceConfiguration {
    public static final String CONFIGURATION_PID = "org.openhab.voice";

    // constants for the configuration properties
    public static final String CONFIG_URI = "system:voice";
    public static final String CONFIG_DEFAULT_HLI = "defaultHLI";
    public static final String CONFIG_DEFAULT_KS = "defaultKS";
    public static final String CONFIG_DEFAULT_STT = "defaultSTT";
    public static final String CONFIG_DEFAULT_TTS = "defaultTTS";
    public static final String CONFIG_DEFAULT_VOICE = "defaultVoice";
    public static final String CONFIG_PREFIX_DEFAULT_VOICE = "defaultVoice.";
    public static final String CONFIG_CONVERSATION_HISTORY_LIMIT = "conversationHistoryLimit";
    public static final String CONFIG_IMPLICIT_ITEM_PERMISSION = "implicitItemPermission";

    // default configuration which type cannot be stored in config XML
    public static final ItemPermission DEFAULT_IMPLICIT_ITEM_ACCESS = ItemPermission.READ_WRITE;

    private final Logger logger = LoggerFactory.getLogger(VoiceConfiguration.class);
    private final ConfigDescriptionRegistry configDescriptionRegistry;

    private ConfigurationDTO configurationDTO = new ConfigurationDTO();
    private ItemPermission implicitItemPermission = DEFAULT_IMPLICIT_ITEM_ACCESS;
    private final Map<String, String> defaultVoices = new HashMap<>();

    public VoiceConfiguration(final ConfigDescriptionRegistry configDescriptionRegistry) {
        this.configDescriptionRegistry = configDescriptionRegistry;
    }

    public void update(Map<String, Object> config) {
        Map<String, @Nullable Object> properties = new HashMap<>(config);

        var configDescription = configDescriptionRegistry.getConfigDescription(URI.create(CONFIG_URI));
        if (configDescription == null) {
            logger.warn("No configuration description found for {}, unable to apply defaults!", CONFIG_URI);
        } else {
            ConfigUtil.applyDefaultConfiguration(properties, configDescription);
        }

        ConfigurationDTO configDTO = ConfigParser.configurationAs(properties, ConfigurationDTO.class);
        if (configDTO == null) {
            logger.error("Unable to parse configuration for {}!", CONFIGURATION_PID);
            return;
        }
        configurationDTO = configDTO;

        try {
            this.implicitItemPermission = ItemPermission.valueOf(configDTO.implicitItemPermission.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid implicitItemPermission value '{}', using {}",
                    configDTO.implicitItemPermission.toUpperCase(), DEFAULT_IMPLICIT_ITEM_ACCESS);
            this.implicitItemPermission = DEFAULT_IMPLICIT_ITEM_ACCESS;
        }

        defaultVoices.clear();
        for (Entry<String, Object> entry : config.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(CONFIG_PREFIX_DEFAULT_VOICE)) {
                String tts = key.substring(CONFIG_PREFIX_DEFAULT_VOICE.length());
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

    public int getConversationHistoryLimit() {
        return configurationDTO.conversationHistoryLimit;
    }

    public ItemPermission getImplicitItemPermission() {
        return implicitItemPermission;
    }

    public String getSystemPrompt() {
        return configurationDTO.systemPrompt;
    }

    public Map<String, String> getDefaultVoices() {
        return Map.copyOf(defaultVoices);
    }

    public static class ConfigurationDTO {
        public String defaultTTS = "";
        public String defaultSTT = "";
        public String defaultVoice = "";
        public String defaultHLI = "";
        public String defaultKS = "";
        public String keyword = "";
        public String listeningItem = "";
        public String listeningMelody = "";
        public boolean enableCacheTTS = true;
        public int cacheSizeTTS = 10240;
        public int maxTextLengthCacheTTS = 150;
        public int conversationHistoryLimit = Conversation.DEFAULT_MAX_MESSAGES;
        public String implicitItemPermission = DEFAULT_IMPLICIT_ITEM_ACCESS.name();
        public String systemPrompt = "";
    }
}
