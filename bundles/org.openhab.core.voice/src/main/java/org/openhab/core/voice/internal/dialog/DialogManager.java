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
package org.openhab.core.voice.internal.dialog;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.voice.dialog.DialogContext;
import org.openhab.core.voice.dialog.DialogRegistration;
import org.openhab.core.voice.dialog.trigger.DTServiceHandle;

/**
 * Manages dialog persistence, lifecycle, service reactivity, and concurrency locking.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public interface DialogManager {

    /**
     * Returns a dialog context builder with default configured services.
     *
     * @return a {@link DialogContext.Builder}
     */
    DialogContext.Builder getDialogContextBuilder();

    /**
     * Returns the context of the last executed dialog interpretation if any.
     *
     * @return the last {@link DialogContext} or null
     * @deprecated Use {@link #getDialogsContexts()} instead to get all the dialog contexts.
     */
    @Deprecated
    @Nullable
    DialogContext getLastDialogContext();

    /**
     * Returns a list with the contexts of all currently running dialogs.
     *
     * @return list of active dialog contexts
     */
    List<DialogContext> getDialogsContexts();

    /**
     * Starts a persistent dialog sequence.
     *
     * @param context dialog context
     * @return service handle or null
     * @throws IllegalStateException if the dialog cannot be started
     */
    @Nullable
    DTServiceHandle startDialog(DialogContext context) throws IllegalStateException;

    /**
     * Stops the dialog associated with the given audio source.
     *
     * @param source the audio source or null to use the default source
     * @throws IllegalStateException if no dialog is started for the source
     */
    void stopDialog(@Nullable AudioSource source) throws IllegalStateException;

    /**
     * Stops the dialog associated with the given dialog context.
     *
     * @param context dialog context
     * @throws IllegalStateException if no dialog is started for the source
     */
    void stopDialog(DialogContext context) throws IllegalStateException;

    /**
     * Executes a simple dialog sequence without keyword spotting.
     *
     * @param context dialog context
     * @throws IllegalStateException if the simple dialog cannot be executed
     */
    void listenAndAnswer(DialogContext context) throws IllegalStateException;

    /**
     * Returns the list of registered dialogs.
     *
     * @return list of dialog registrations
     */
    List<DialogRegistration> getDialogRegistrations();

    /**
     * Registers a persistent dialog for an audio source.
     *
     * @param registration dialog registration
     * @throws IllegalStateException if the registration already exists
     */
    void registerDialog(DialogRegistration registration) throws IllegalStateException;

    /**
     * Unregisters a persistent dialog registration.
     *
     * @param registration dialog registration
     * @throws IllegalStateException if the registration does not exist
     */
    void unregisterDialog(DialogRegistration registration) throws IllegalStateException;

    /**
     * Unregisters a persistent dialog registration associated with the given audio source.
     *
     * @param sourceId the ID of the audio source
     */
    void unregisterDialog(String sourceId);
}
