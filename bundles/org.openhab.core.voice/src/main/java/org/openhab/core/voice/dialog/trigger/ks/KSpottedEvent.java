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
package org.openhab.core.voice.dialog.trigger.ks;

import java.time.Duration;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.voice.dialog.trigger.DTTriggeredEvent;

/**
 * A {@link KSEvent} fired when the {@link KSService} spots a keyword.
 *
 * @author Kelly Davis - Initial contribution
 * @author Yannick Schaus - Removed AudioSource information
 * @author Dan Cunningham - Added the audio read after the keyword
 */
@NonNullByDefault
public class KSpottedEvent extends DTTriggeredEvent implements KSEvent {
    private final @Nullable Duration audioAfterKeyword;

    /**
     * Creates an event without saying where the keyword ended. Speech recognition starts with whatever audio
     * comes next.
     */
    public KSpottedEvent() {
        this.audioAfterKeyword = null;
    }

    /**
     * Creates an event that says where the keyword ended. Speech recognition starts here, prevents users from have
     * to pause after the keyword.
     *
     * @param audioAfterKeyword how much audio the spotter has read since the keyword ended
     */
    public KSpottedEvent(Duration audioAfterKeyword) {
        this.audioAfterKeyword = audioAfterKeyword;
    }

    /**
     * How much audio the spotter had read past the end of the keyword when it sent this event.
     *
     * @return the length of that audio, or null when the spotter doesn't tell
     */
    public @Nullable Duration getAudioAfterKeyword() {
        return audioAfterKeyword;
    }
}
