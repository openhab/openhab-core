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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigParser;
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
    // the default keyword to use if no other is configured
    private static final String DEFAULT_KEYWORD = "Wakeup";
    private static final ItemPermission DEFAULT_IMPLICIT_ITEM_ACCESS = ItemPermission.READ_WRITE;

    // constants for the configuration properties
    public static final String CONFIG_URI = "system:voice";
    public static final String CONFIG_KEYWORD = "keyword";
    public static final String CONFIG_LISTENING_ITEM = "listeningItem";
    public static final String CONFIG_LISTENING_MELODY = "listeningMelody";
    public static final String CONFIG_DEFAULT_HLI = "defaultHLI";
    public static final String CONFIG_DEFAULT_KS = "defaultKS";
    public static final String CONFIG_DEFAULT_STT = "defaultSTT";
    public static final String CONFIG_DEFAULT_TTS = "defaultTTS";
    public static final String CONFIG_DEFAULT_VOICE = "defaultVoice";
    public static final String CONFIG_PREFIX_DEFAULT_VOICE = "defaultVoice.";
    public static final String CONFIG_CONVERSATION_HISTORY_LIMIT = "conversationHistoryLimit";
    public static final String CONFIG_IMPLICIT_ITEM_PERMISSION = "implicitItemPermission";

    private final Logger logger = LoggerFactory.getLogger(VoiceConfiguration.class);

    private String keyword = DEFAULT_KEYWORD;
    private @Nullable String listeningItem;
    private @Nullable String listeningMelody;
    private @Nullable String defaultTTS;
    private @Nullable String defaultSTT;
    private @Nullable String defaultKS;
    private @Nullable String defaultHLI;
    private @Nullable String defaultVoice;
    private int conversationHistoryLimit = Conversation.DEFAULT_MAX_MESSAGES;
    private ItemPermission implicitItemPermission = DEFAULT_IMPLICIT_ITEM_ACCESS;
    private final Map<String, String> defaultVoices = new HashMap<>();

    public void update(Map<String, Object> config) {
        this.keyword = ConfigParser.valueAsOrElse(config.get(CONFIG_KEYWORD), String.class, DEFAULT_KEYWORD);
        this.listeningItem = ConfigParser.valueAs(config.get(CONFIG_LISTENING_ITEM), String.class);
        this.listeningMelody = ConfigParser.valueAs(config.get(CONFIG_LISTENING_MELODY), String.class);
        this.defaultTTS = ConfigParser.valueAs(config.get(CONFIG_DEFAULT_TTS), String.class);
        this.defaultSTT = ConfigParser.valueAs(config.get(CONFIG_DEFAULT_STT), String.class);
        this.defaultKS = ConfigParser.valueAs(config.get(CONFIG_DEFAULT_KS), String.class);
        this.defaultHLI = ConfigParser.valueAs(config.get(CONFIG_DEFAULT_HLI), String.class);
        this.defaultVoice = ConfigParser.valueAs(config.get(CONFIG_DEFAULT_VOICE), String.class);
        this.conversationHistoryLimit = ConfigParser.valueAsOrElse(config.get(CONFIG_CONVERSATION_HISTORY_LIMIT),
                Integer.class, Conversation.DEFAULT_MAX_MESSAGES);
        String implicitItemPermissionStr = ConfigParser.valueAsOrElse(config.get(CONFIG_IMPLICIT_ITEM_PERMISSION),
                String.class, DEFAULT_IMPLICIT_ITEM_ACCESS.name());
        try {
            this.implicitItemPermission = ItemPermission.valueOf(implicitItemPermissionStr);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid implicitItemPermission value '{}', using {}", implicitItemPermissionStr,
                    DEFAULT_IMPLICIT_ITEM_ACCESS);
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
        return keyword;
    }

    public @Nullable String getListeningItem() {
        return listeningItem;
    }

    public @Nullable String getListeningMelody() {
        return listeningMelody;
    }

    public @Nullable String getDefaultTTS() {
        return defaultTTS;
    }

    public @Nullable String getDefaultSTT() {
        return defaultSTT;
    }

    public @Nullable String getDefaultKS() {
        return defaultKS;
    }

    public @Nullable String getDefaultHLI() {
        return defaultHLI;
    }

    public @Nullable String getDefaultVoice() {
        return defaultVoice;
    }

    public int getConversationHistoryLimit() {
        return conversationHistoryLimit;
    }

    public ItemPermission getImplicitItemPermission() {
        return implicitItemPermission;
    }

    public Map<String, String> getDefaultVoices() {
        return Map.copyOf(defaultVoices);
    }
}
