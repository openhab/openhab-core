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
package org.openhab.core.voice.internal.tts.cache;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;

/**
 * Serializable {@link AudioFormat} storage class to store {@link AudioFormat} with
 * {@link org.openhab.core.storage.StorageService}.
 *
 * @author Gwendal Roulleau - Initial contribution
 */
@NonNullByDefault
public record AudioFormatInfo(@Nullable Boolean bigEndian, @Nullable Integer bitDepth, @Nullable Integer bitRate,
        @Nullable Long frequency, @Nullable Integer channels, @Nullable String codec, @Nullable String container) {

    public AudioFormatInfo(AudioFormat audioFormat) {
        this(audioFormat.isBigEndian(), audioFormat.getBitDepth(), audioFormat.getBitRate(), audioFormat.getFrequency(),
                audioFormat.getChannels(), audioFormat.getCodec(), audioFormat.getContainer());
    }

    public AudioFormat toAudioFormat() {
        return new AudioFormat(container, codec, bigEndian, bitDepth, bitRate, frequency, channels);
    }
}
