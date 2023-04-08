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
package org.openhab.core.audio.utils;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.library.types.PercentType;
import org.slf4j.Logger;

/**
 * Some utility methods for sink
 *
 * @author Gwendal Roulleau - Initial contribution
 *
 */
@NonNullByDefault
public class AudioSinkUtils {

    /**
     * Handle a volume command change and returns a Runnable to restore it.
     *
     * @param volume The volume to set
     * @param sink The sink to set the volume to
     * @param logger to log error to
     * @return A runnable to restore the volume to its previous value, or null if no change is required
     */
    public static @Nullable Runnable handleVolumeCommand(@Nullable PercentType volume, AudioSink sink, Logger logger) {
        boolean volumeChanged = false;
        PercentType oldVolume = null;

        if (volume == null) {
            return null;
        }

        // set notification sound volume
        try {
            // get current volume
            oldVolume = sink.getVolume();
        } catch (IOException | UnsupportedOperationException e) {
            logger.debug("An exception occurred while getting the volume of sink '{}' : {}", sink.getId(),
                    e.getMessage(), e);
        }

        if (!volume.equals(oldVolume) || oldVolume == null) {
            try {
                sink.setVolume(volume);
                volumeChanged = true;
            } catch (IOException | UnsupportedOperationException e) {
                logger.debug("An exception occurred while setting the volume of sink '{}' : {}", sink.getId(),
                        e.getMessage(), e);
            }
        }

        final PercentType oldVolumeFinal = oldVolume;
        Runnable toRunWhenProcessFinished = null;
        // restore volume only if it was set before
        if (volumeChanged && oldVolumeFinal != null) {
            toRunWhenProcessFinished = () -> {
                try {
                    sink.setVolume(oldVolumeFinal);
                } catch (IOException | UnsupportedOperationException e) {
                    logger.debug("An exception occurred while setting the volume of sink '{}' : {}", sink.getId(),
                            e.getMessage(), e);
                }
            };
        }

        return toRunWhenProcessFinished;
    }
}
