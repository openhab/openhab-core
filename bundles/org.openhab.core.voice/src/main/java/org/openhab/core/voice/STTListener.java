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
package org.openhab.core.voice;

/**
 * The listener interface for receiving {@link STTEvent} events.
 *
 * A class interested in processing {@link STTEvent} events implements this interface,
 * and its instances are passed to the {@code STTService}'s {@code recognize()} method.
 * Such instances are then targeted for various {@link STTEvent} events corresponding
 * to the speech recognition process.
 *
 * @author Kelly Davis - Initial contribution
 */
public interface STTListener {

    /**
     * Invoked when a {@link STTEvent} event occurs during speech recognition.
     *
     * @param sttEvent The {@link STTEvent} fired by the {@link STTService}
     */
    public void sttEventReceived(STTEvent sttEvent);
}
