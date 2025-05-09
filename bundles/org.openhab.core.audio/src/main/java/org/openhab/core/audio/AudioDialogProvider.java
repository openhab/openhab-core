/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.audio;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This interface is designed so the voice bundle can inject the start dialog functionality for audio bundle to consume,
 * which a programmatic way of trigger the dialog execution.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public interface AudioDialogProvider {
    /**
     * Starts a dialog and returns a runnable that triggers it or null if the dialog initialization fails
     *
     * @param audioSink the audio sink to play sound
     * @param audioSource the audio source to capture sound
     * @param locationItem an optional Item name to scope dialog commands
     * @param listeningItem an optional Item name to toggle while dialog is listening, overwrites default
     * @param onAbort an optional {@link Runnable} instance to call on abort.
     * @return a {@link Runnable} instance to trigger dialog processing or null if the dialog initialization fails
     */
    @Nullable
    Runnable startDialog(AudioSink audioSink, AudioSource audioSource, @Nullable String locationItem,
            @Nullable String listeningItem, @Nullable Runnable onAbort);
}
