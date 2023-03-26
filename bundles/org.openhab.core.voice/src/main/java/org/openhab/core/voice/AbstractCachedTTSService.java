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
package org.openhab.core.voice;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.voice.internal.cache.CachedTTSService;
import org.osgi.service.component.annotations.Reference;

/**
 * Implements cache functionality for the TTS service extending this class.
 *
 * @author Gwendal Roulleau - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractCachedTTSService implements CachedTTSService {

    private final TTSCache ttsCache;

    public AbstractCachedTTSService(final @Reference TTSCache ttsCache) {
        this.ttsCache = ttsCache;
    }

    @Override
    public AudioStream synthesize(String text, Voice voice, AudioFormat requestedFormat) throws TTSException {
        return ttsCache.get(this, text, voice, requestedFormat);
    }

    @Override
    public String getCacheKey(String text, Voice voice, AudioFormat requestedFormat) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return "nomd5algorithm";
        }
        byte[] binaryKey = ((text + voice.getUID() + requestedFormat.toString()).getBytes());
        return String.format("%032x", new BigInteger(1, md.digest(binaryKey)));
    }
}
