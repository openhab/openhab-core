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

    /**
     * Starts a dialog and returns a runnable that triggers it or null if the dialog initialization fails. The default
     * implementation ignores {@code onIdle}.
     *
     * @param audioSink the audio sink to play sound
     * @param audioSource the audio source to capture sound
     * @param locationItem an optional Item name to scope dialog commands
     * @param listeningItem an optional Item name to toggle while dialog is listening, overwrites default
     * @param onAbort an optional {@link Runnable} instance to call on abort.
     * @param onIdle an optional {@link Runnable} instance to call each time the dialog finishes an interaction, even
     *            one without a reply. The reply may still be playing. It may run on the trigger's thread, so it must
     *            return quickly and must not trigger the dialog. It may still run just after {@code onAbort}.
     * @return a {@link Runnable} instance to trigger dialog processing or null if the dialog initialization fails
     */
    default @Nullable Runnable startDialog(AudioSink audioSink, AudioSource audioSource, @Nullable String locationItem,
            @Nullable String listeningItem, @Nullable Runnable onAbort, @Nullable Runnable onIdle) {
        return startDialog(audioSink, audioSource, locationItem, listeningItem, onAbort);
    }

    /**
     * Starts a dialog that is triggered when the default keyword spotter spots the default magic word.
     *
     * @param audioSink the audio sink to play sound
     * @param audioSource the audio source to capture sound
     * @param locationItem an optional Item name to scope dialog commands
     * @param listeningItem an optional Item name to toggle while dialog is listening, overwrites default
     * @param onAbort an optional {@link Runnable} instance to call when the started dialog is stopped
     * @return true if the dialog was started, false if it failed to start or is not supported by this provider
     * @throws IllegalStateException if the voice settings don't allow starting the dialog
     */
    default boolean startKeywordSpottingDialog(AudioSink audioSink, AudioSource audioSource,
            @Nullable String locationItem, @Nullable String listeningItem, @Nullable Runnable onAbort) {
        return false;
    }

    /**
     * Starts a dialog that is triggered when the default keyword spotter spots the default magic word. The default
     * implementation ignores {@code onIdle}.
     *
     * @param audioSink the audio sink to play sound
     * @param audioSource the audio source to capture sound
     * @param locationItem an optional Item name to scope dialog commands
     * @param listeningItem an optional Item name to toggle while dialog is listening, overwrites default
     * @param onAbort an optional {@link Runnable} instance to call when the started dialog is stopped
     * @param onIdle an optional {@link Runnable} instance to call each time the dialog finishes an interaction, even
     *            one without a reply. The reply may still be playing. It must return quickly, and may still run just
     *            after {@code onAbort}.
     * @return true if the dialog was started, false if it failed to start or is not supported by this provider
     * @throws IllegalStateException if the voice settings don't allow starting the dialog
     */
    default boolean startKeywordSpottingDialog(AudioSink audioSink, AudioSource audioSource,
            @Nullable String locationItem, @Nullable String listeningItem, @Nullable Runnable onAbort,
            @Nullable Runnable onIdle) {
        return startKeywordSpottingDialog(audioSink, audioSource, locationItem, listeningItem, onAbort);
    }
}
