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
 * The listener interface for receiving {@link DTEvent} events.
 *
 * <p>
 * A class interested in processing {@link DTEvent} events implements this interface, and its instances are passed to
 * the {@link BasicDTService}'s {@code registerListener()} method.
 * Such instances are then targeted for various {@link DTEvent} events corresponding to the dialog trigger process.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public interface DTListener {
    /**
     * Invoked when a {@link DTEvent} event occurs.
     *
     * @param dtEvent The {@link DTEvent} fired by the {@link DTService}
     */
    void dtEventReceived(DTEvent dtEvent);
}
