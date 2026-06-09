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

import org.openhab.core.voice.security.ItemPermission;

/**
 * Constants related to <code>org.openhab.voice</code> configuration.
 *
 * @author Florian Hotze - Initial contribution
 */
public class VoiceConfigurationConstants {
    public static final String CONFIGURATION_PID = "org.openhab.voice";

    // configuration properties
    public static final String CONFIG_URI = "system:voice";
    public static final String CONFIG_DEFAULT_HLI = "defaultHLI";
    public static final String CONFIG_DEFAULT_KS = "defaultKS";
    public static final String CONFIG_DEFAULT_STT = "defaultSTT";
    public static final String CONFIG_DEFAULT_TTS = "defaultTTS";
    public static final String CONFIG_DEFAULT_VOICE = "defaultVoice";
    public static final String CONFIG_PREFIX_DEFAULT_VOICE = "defaultVoice.";
    public static final String CONFIG_CONVERSATION_HISTORY_LIMIT = "conversationHistoryLimit";
    public static final String CONFIG_IMPLICIT_ITEM_PERMISSION = "implicitItemPermission";

    // configuration defaults
    public static final int DEFAULT_CONVERSATION_HISTORY_LIMIT = 50;
    public static final ItemPermission DEFAULT_IMPLICIT_ITEM_ACCESS = ItemPermission.READ_WRITE;
}
