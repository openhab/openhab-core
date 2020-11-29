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
package org.openhab.core.automation.module.media.internal;

import java.math.BigDecimal;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.handler.BaseActionModuleHandler;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.voice.VoiceManager;

/**
 * This is an ModuleHandler implementation for Actions that trigger a TTS output through "say".
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Christoph Weitkamp - Added parameter volume
 */
@NonNullByDefault
public class SayActionHandler extends BaseActionModuleHandler {

    public static final String TYPE_ID = "media.SayAction";
    public static final String PARAM_TEXT = "text";
    public static final String PARAM_SINK = "sink";
    public static final String PARAM_VOLUME = "volume";

    private final VoiceManager voiceManager;

    private final String text;
    private final String sink;
    private final @Nullable PercentType volume;

    public SayActionHandler(Action module, VoiceManager voiceManager) {
        super(module);
        this.voiceManager = voiceManager;

        text = module.getConfiguration().get(PARAM_TEXT).toString();
        sink = module.getConfiguration().get(PARAM_SINK).toString();

        Object volumeParam = module.getConfiguration().get(PARAM_VOLUME);
        this.volume = volumeParam instanceof BigDecimal ? new PercentType((BigDecimal) volumeParam) : null;
    }

    @Override
    public @Nullable Map<String, Object> execute(Map<String, Object> context) {
        voiceManager.say(text, null, sink, volume);
        return null;
    }
}
