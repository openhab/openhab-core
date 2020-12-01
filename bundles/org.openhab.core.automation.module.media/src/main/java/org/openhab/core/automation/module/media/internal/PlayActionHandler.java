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
import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.handler.BaseActionModuleHandler;
import org.openhab.core.library.types.PercentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an ModuleHandler implementation for Actions that play a sound file from the file system.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Christoph Weitkamp - Added parameter volume
 */
@NonNullByDefault
public class PlayActionHandler extends BaseActionModuleHandler {

    public static final String TYPE_ID = "media.PlayAction";
    public static final String PARAM_SOUND = "sound";
    public static final String PARAM_SINK = "sink";
    public static final String PARAM_VOLUME = "volume";

    private final Logger logger = LoggerFactory.getLogger(PlayActionHandler.class);

    private final AudioManager audioManager;

    private final String sound;
    private final String sink;
    private final @Nullable PercentType volume;

    public PlayActionHandler(Action module, AudioManager audioManager) {
        super(module);
        this.audioManager = audioManager;

        this.sound = module.getConfiguration().get(PARAM_SOUND).toString();
        this.sink = module.getConfiguration().get(PARAM_SINK).toString();

        Object volumeParam = module.getConfiguration().get(PARAM_VOLUME);
        this.volume = volumeParam instanceof BigDecimal ? new PercentType((BigDecimal) volumeParam) : null;
    }

    @Override
    public @Nullable Map<String, Object> execute(Map<String, Object> context) {
        try {
            audioManager.playFile(sound, sink, volume);
        } catch (AudioException e) {
            logger.error("Error playing sound '{}': {}", sound, e.getMessage());
        }
        return null;
    }
}
