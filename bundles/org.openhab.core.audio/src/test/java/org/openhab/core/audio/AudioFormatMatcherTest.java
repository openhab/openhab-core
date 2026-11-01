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
package org.openhab.core.audio;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.audio.utils.AudioFormatMatcher;

/**
 * Test class for {@link AudioFormatMatcher}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class AudioFormatMatcherTest {

    private static final AudioFormat CONCRETE_PCM_44100 = new AudioFormat(AudioFormat.CONTAINER_NONE,
            AudioFormat.CODEC_PCM_SIGNED, true, 16, 705600, 44100L, 1);
    private static final AudioFormat CONCRETE_WAV_44100 = new AudioFormat(AudioFormat.CONTAINER_WAVE,
            AudioFormat.CODEC_PCM_SIGNED, true, 16, 705600, 44100L, 1);
    private static final AudioFormat CONCRETE_WAV_16000 = new AudioFormat(AudioFormat.CONTAINER_WAVE,
            AudioFormat.CODEC_PCM_SIGNED, true, 16, 256000, 16000L, 1);
    private static final AudioFormat CONCRETE_MP3 = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_MP3,
            true, 16, 128000, 44100L, 2);
    private static final AudioFormat ABSTRACT_PCM = new AudioFormat(AudioFormat.CONTAINER_NONE,
            AudioFormat.CODEC_PCM_SIGNED, null, null, null, null, 1);
    private static final AudioFormat ABSTRACT_WAV = new AudioFormat(AudioFormat.CONTAINER_WAVE,
            AudioFormat.CODEC_PCM_SIGNED, null, null, null, null, 1);

    @Test
    public void getPreferredFormatReturnsNullForEmptySet() {
        assertNull(AudioFormatMatcher.getPreferredFormat(Set.of()));
    }

    @Test
    public void getPreferredFormatPrefersConcretePcmOverConcreteWav() {
        AudioFormat preferred = AudioFormatMatcher.getPreferredFormat(Set.of(CONCRETE_WAV_44100, CONCRETE_PCM_44100));
        assertEquals(CONCRETE_PCM_44100, preferred);
    }

    @Test
    public void getPreferredFormatPrefersAbstractPcmOverAbstractWav() {
        AudioFormat preferred = AudioFormatMatcher.getPreferredFormat(Set.of(ABSTRACT_WAV, ABSTRACT_PCM));
        assertNotNull(preferred);
        assertEquals(AudioFormat.CONTAINER_NONE, preferred.getContainer());
        assertEquals(AudioFormat.CODEC_PCM_SIGNED, preferred.getCodec());
        assertEquals(Boolean.TRUE, preferred.isBigEndian());
        assertEquals(16, preferred.getBitDepth());
        assertEquals(16000L, preferred.getFrequency());
        assertEquals(16 * 16000, preferred.getBitRate());
    }

    @Test
    public void getPreferredFormatReturnsConcreteWavWhenNoPcm() {
        AudioFormat preferred = AudioFormatMatcher.getPreferredFormat(Set.of(CONCRETE_WAV_44100, CONCRETE_MP3));
        assertEquals(CONCRETE_WAV_44100, preferred);
    }

    @Test
    public void getPreferredFormatFillsDefaultsForAbstractWavWhenNoPcm() {
        AudioFormat preferred = AudioFormatMatcher.getPreferredFormat(Set.of(ABSTRACT_WAV));
        assertNotNull(preferred);
        assertEquals(AudioFormat.CONTAINER_WAVE, preferred.getContainer());
        assertEquals(AudioFormat.CODEC_PCM_SIGNED, preferred.getCodec());
        assertEquals(Boolean.TRUE, preferred.isBigEndian());
        assertEquals(16, preferred.getBitDepth());
        assertEquals(16000L, preferred.getFrequency());
        assertEquals(16 * 16000, preferred.getBitRate());
    }

    @Test
    public void getBestMatchFindsCompatibleFormat() {
        AudioFormat bestMatch = AudioFormatMatcher.getBestMatch(Set.of(CONCRETE_WAV_44100),
                Set.of(CONCRETE_WAV_44100, CONCRETE_MP3));
        assertEquals(CONCRETE_WAV_44100, bestMatch);
    }

    @Test
    public void getBestMatchReturnsNullWhenIncompatible() {
        AudioFormat bestMatch = AudioFormatMatcher.getBestMatch(Set.of(CONCRETE_MP3), Set.of(CONCRETE_WAV_44100));
        assertNull(bestMatch);
    }

    @Test
    public void getBestMatchWithTranscodingReturnsDirectMatchWhenAvailable() {
        AudioFormat match = AudioFormatMatcher.getBestMatchWithTranscoding(Set.of(CONCRETE_WAV_44100),
                Set.of(CONCRETE_WAV_44100));
        assertEquals(CONCRETE_WAV_44100, match);
    }

    @Test
    public void getBestMatchWithTranscodingFindsTranscodeableFormat() {
        // WAV to WAV with different sample rate or MP3 to WAV can be transcoded
        AudioFormat match = AudioFormatMatcher.getBestMatchWithTranscoding(Set.of(CONCRETE_WAV_44100),
                Set.of(CONCRETE_WAV_16000));
        assertNotNull(match);
    }
}
