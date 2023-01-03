/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
import org.openhab.core.audio.AudioManager;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.handler.BaseActionModuleHandler;
import org.openhab.core.library.types.PercentType;

/**
 * This is an ModuleHandler implementation for Actions that synthesize a tone melody.
 *
 * @author Miguel Álvarez - Initial contribution
 */
@NonNullByDefault
public class SynthesizeActionHandler extends BaseActionModuleHandler {

    public static final String TYPE_ID = "media.SynthesizeAction";
    public static final String PARAM_MELODY = "melody";
    public static final String PARAM_SINK = "sink";
    public static final String PARAM_VOLUME = "volume";

    private final AudioManager audioManager;
    private final String melody;
    private final @Nullable String sink;
    private final @Nullable PercentType volume;

    public SynthesizeActionHandler(Action module, AudioManager audioManager) {
        super(module);
        this.audioManager = audioManager;

        this.melody = module.getConfiguration().get(PARAM_MELODY).toString();

        Object sinkParam = module.getConfiguration().get(PARAM_SINK);
        this.sink = sinkParam != null ? sinkParam.toString() : null;

        Object volumeParam = module.getConfiguration().get(PARAM_VOLUME);
        this.volume = volumeParam instanceof BigDecimal ? new PercentType((BigDecimal) volumeParam) : null;
    }

    @Override
    public @Nullable Map<String, Object> execute(Map<String, Object> context) {
        audioManager.playMelody(melody, sink, volume);
        return null;
    }
}
