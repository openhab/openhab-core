/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.voice.internal.cache;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;

/**
 * Serializable AudioFormat storage class
 * We cannot use a record yet (requires Gson v2.10)
 *
 * @author Gwendal Roulleau - Initial contribution
 */
@NonNullByDefault
public class AudioFormatInfo {
    public final @Nullable Boolean bigEndian;
    public final @Nullable Integer bitDepth;
    public final @Nullable Integer bitRate;
    public final @Nullable Long frequency;
    public final @Nullable Integer channels;
    public final @Nullable String codec;
    public final @Nullable String container;

    public AudioFormatInfo(String text, @Nullable Boolean bigEndian, @Nullable Integer bitDepth,
            @Nullable Integer bitRate, @Nullable Long frequency, @Nullable Integer channels, @Nullable String codec,
            @Nullable String container) {
        this.bigEndian = bigEndian;
        this.bitDepth = bitDepth;
        this.bitRate = bitRate;
        this.frequency = frequency;
        this.channels = channels;
        this.codec = codec;
        this.container = container;
    }

    public AudioFormatInfo(AudioFormat audioFormat) {
        this.bigEndian = audioFormat.isBigEndian();
        this.bitDepth = audioFormat.getBitDepth();
        this.bitRate = audioFormat.getBitRate();
        this.frequency = audioFormat.getFrequency();
        this.channels = audioFormat.getChannels();
        this.codec = audioFormat.getCodec();
        this.container = audioFormat.getContainer();
    }

    public AudioFormat toAudioFormat() {
        return new AudioFormat(container, codec, bigEndian, bitDepth, bitRate, frequency, channels);
    }
}
