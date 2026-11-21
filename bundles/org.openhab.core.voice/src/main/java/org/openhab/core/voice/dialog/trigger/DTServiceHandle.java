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
package org.openhab.core.voice.dialog.trigger;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A handle to a {@link DTService}.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public interface DTServiceHandle {
    /**
     * Aborts execution in the associated {@link DTServiceHandle}
     */
    void abort();

    /**
     * Called each time the dialog finishes an interaction, even one without a reply, and when it ignores a trigger
     * because its dialog group is busy. The reply may still be playing. It may run on the trigger's thread, so it must
     * return quickly and must not trigger the dialog. It may still be called just after {@link #abort()}.
     */
    default void onDialogIdle() {
    }
}
