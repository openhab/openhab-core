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
package org.openhab.core.voice;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The listener interface for receiving {@link DTEvent} events.
 *
 * A class interested in processing {@link DTEvent} events implements this interface.
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
