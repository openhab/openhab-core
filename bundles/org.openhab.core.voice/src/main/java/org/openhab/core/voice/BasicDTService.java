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
 * This is the interface for a dialog trigger service that only allows to register a {@link DTEvent} listener.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public interface BasicDTService extends DTService {
    /**
     * Used to register the dialog trigger events listener
     * 
     * @param dtListener Non-null {@link DTListener} that {@link DTEvent} events target
     * @throws DTException if the listener cannot be registered due to an internal error or invalid state
     */
    DTServiceHandle registerListener(DTListener dtListener) throws DTException;
}
