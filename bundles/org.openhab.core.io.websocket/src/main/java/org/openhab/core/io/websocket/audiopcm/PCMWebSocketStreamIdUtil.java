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
package org.openhab.core.io.websocket.audiopcm;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Utils to read/write the audio packets send though the websocket binary protocol.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public class PCMWebSocketStreamIdUtil {
    /**
     * Packet header length in bytes:
     * 2 for id
     * 4 for sample rate as int little-endian
     * 1 for bitDepth
     * 1 for channels
     */
    public static int packetHeaderByteLength = 2 + 4 + 1 + 1;

    public static AudioPacketData parseAudioPacket(byte[] bytes) {
        assert (bytes.length >= packetHeaderByteLength);
        var byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        byte[] idBytes = new byte[] { byteBuffer.get(), byteBuffer.get() };
        int sampleRate = byteBuffer.getInt();
        byte bitDepth = byteBuffer.get();
        byte channels = byteBuffer.get();
        byte[] data = new byte[byteBuffer.remaining()];
        byteBuffer.get(data);
        return new AudioPacketData(idBytes, sampleRate, bitDepth, channels, data);
    }

    public static ByteBuffer generateAudioPacketHeader(int sampleRate, byte bitDepth, byte channels) {
        var byteBuffer = ByteBuffer.allocate(packetHeaderByteLength).order(ByteOrder.LITTLE_ENDIAN);
        SecureRandom sr = new SecureRandom();
        byte[] rndBytes = new byte[2];
        sr.nextBytes(rndBytes);
        byteBuffer.put(rndBytes[0]);
        byteBuffer.put(rndBytes[1]);
        byteBuffer.putInt(sampleRate);
        byteBuffer.put(bitDepth);
        byteBuffer.put(channels);
        return byteBuffer;
    }

    public record AudioPacketData(byte[] id, int sampleRate, byte bitDepth, byte channels, byte[] audioData) {
    }
}
