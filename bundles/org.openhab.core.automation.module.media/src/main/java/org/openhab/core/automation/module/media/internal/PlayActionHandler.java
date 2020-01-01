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

import java.util.Map;

import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.handler.BaseActionModuleHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an ModuleHandler implementation for Actions that play a sound file from the file system.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class PlayActionHandler extends BaseActionModuleHandler {

    public static final String TYPE_ID = "media.PlayAction";
    public static final String PARAM_SOUND = "sound";
    public static final String PARAM_SINK = "sink";

    private final Logger logger = LoggerFactory.getLogger(PlayActionHandler.class);

    private final AudioManager audioManager;

    public PlayActionHandler(Action module, AudioManager audioManager) {
        super(module);
        this.audioManager = audioManager;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> context) {
        String sound = module.getConfiguration().get(PARAM_SOUND).toString();
        String sink = (String) module.getConfiguration().get(PARAM_SINK);
        try {
            audioManager.playFile(sound, sink);
        } catch (AudioException e) {
            logger.error("Error playing sound '{}': {}", sound, e.getMessage());
        }
        return null;
    }

}
