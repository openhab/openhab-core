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

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.audio.AudioStream;

/**
 * This is the interface that an edge keyword spotting service has to implement.
 * Used to register a keyword spotting service that is running on a remote device.
 *
 * @author Miguel Álvarez - Initial contribution
 */
@NonNullByDefault
public interface KSEdgeService extends KSService {

    /**
     * This method links the remote keyword spotting process to a consumer.
     *
     * The method is supposed to return fast.
     *
     * @param ksListener Non-null {@link KSListener} that {@link KSEvent} events target
     * @throws KSException if any parameter is invalid or a problem occurs
     */
    KSServiceHandle spot(KSListener ksListener) throws KSException;

    @Override
    default KSServiceHandle spot(KSListener ksListener, AudioStream audioStream, Locale locale, String keyword)
            throws KSException {
        throw new KSException("An edge keyword spotter is not meant to process audio in the server");
    }
}
