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
package org.openhab.core.audio.transcode;

import java.io.Serial;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioException;

/**
 * Exception thrown when audio transcoding fails or cannot be performed.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class AudioTranscodingException extends AudioException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AudioTranscodingException(String message) {
        super(message);
    }

    public AudioTranscodingException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
