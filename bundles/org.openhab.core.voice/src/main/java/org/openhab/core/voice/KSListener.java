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
 * The listener interface for receiving {@link KSEvent} events.
 *
 * A class interested in processing {@link KSEvent} events implements this interface,
 * and its instances are passed to the {@code KSService}'s {@code spot()} method.
 * Such instances are then targeted for various {@link KSEvent} events corresponding
 * to the keyword spotting process.
 *
 * @author Kelly Davis - Initial contribution
 */
public interface KSListener {
    /**
     * Invoked when a {@link KSEvent} event occurs during keyword spotting.
     *
     * @param ksEvent The {@link KSEvent} fired by the {@link KSService}
     */
    public void ksEventReceived(KSEvent ksEvent);
}
