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
package org.openhab.core.audio;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * An audio format definition
 *
 * @author Harald Kuhn - Initial contribution
 * @author Kelly Davis - Modified to match discussion in #584
 * @author Kai Kreuzer - Moved class, included constants, added toString
 */
@NonNullByDefault
public class AudioFormat {

    // generic mp3 format without any further constraints
    public static final AudioFormat MP3 = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_MP3, null, null,
            null, null);

    // generic wav format without any further constraints
    public static final AudioFormat WAV = new AudioFormat(AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_PCM_SIGNED,
            null, null, null, null);

    // generic OGG format without any further constraints
    public static final AudioFormat OGG = new AudioFormat(AudioFormat.CONTAINER_OGG, AudioFormat.CODEC_VORBIS, null,
            null, null, null);

    // generic AAC format without any further constraints
    public static final AudioFormat AAC = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_AAC, null, null,
            null, null);

    /**
     * {@link AudioCodec} encoded data without any container header or footer,
     * e.g. MP3 is a non-container format
     */
    public static final String CONTAINER_NONE = "NONE";

    /**
     * Microsofts wave container format
     *
     * @see <a href="http://bit.ly/1TUW93t">WAV Format</a>
     * @see <a href="http://bit.ly/1oRMKOt">Supported codecs</a>
     * @see <a href="http://bit.ly/1TUWSlk">RIFF container format</a>
     */
    public static final String CONTAINER_WAVE = "WAVE";

    /**
     * OGG container format
     *
     * @see <a href="http://bit.ly/1oRMWNE">OGG</a>
     */
    public static final String CONTAINER_OGG = "OGG";

    /**
     * PCM Signed
     *
     * @see <a href="http://wiki.multimedia.cx/?title=PCM#PCM_Types">PCM Types</a>
     */
    public static final String CODEC_PCM_SIGNED = "PCM_SIGNED";

    /**
     * PCM Unsigned
     *
     * @see <a href="http://wiki.multimedia.cx/?title=PCM#PCM_Types">PCM Types</a>
     */
    public static final String CODEC_PCM_UNSIGNED = "PCM_UNSIGNED";

    /**
     * PCM A-law
     *
     * @see <a href="http://wiki.multimedia.cx/?title=PCM#PCM_Types">PCM Types</a>
     */
    public static final String CODEC_PCM_ALAW = "ALAW";

    /**
     * PCM u-law
     *
     * @see <a href="http://wiki.multimedia.cx/?title=PCM#PCM_Types">PCM Types</a>
     */
    public static final String CODEC_PCM_ULAW = "ULAW";

    /**
     * MP3 Codec
     *
     * @see <a href="http://wiki.multimedia.cx/index.php?title=MP3">MP3 Codec</a>
     */
    public static final String CODEC_MP3 = "MP3";

    /**
     * Vorbis Codec
     *
     * @see <a href="http://xiph.org/vorbis/doc/">Vorbis</a>
     */
    public static final String CODEC_VORBIS = "VORBIS";

    /**
     * AAC Codec
     */
    public static final String CODEC_AAC = "AAC";

    /**
     * Codec
     */
    private final @Nullable String codec;

    /**
     * Container
     */
    private final @Nullable String container;

    /**
     * Big endian or little endian
     */
    private final @Nullable Boolean bigEndian;

    /**
     * Bit depth
     *
     * @see <a href="http://bit.ly/1OTydad">Bit Depth</a>
     */
    private final @Nullable Integer bitDepth;

    /**
     * Bit rate
     *
     * @see <a href="http://bit.ly/1OTy5rk">Bit Rate</a>
     */
    private final @Nullable Integer bitRate;

    /**
     * Sample frequency
     */
    private final @Nullable Long frequency;

    /**
     * Constructs an instance with the specified properties.
     *
     * Note that any properties that are null indicate that
     * the corresponding AudioFormat allows any value for
     * the property.
     *
     * Concretely this implies that if, for example, one
     * passed null for the value of frequency, this would
     * mean the created AudioFormat allowed for any valid
     * frequency.
     *
     * @param container The container for the audio
     * @param codec The audio codec
     * @param bigEndian If the audo data is big endian
     * @param bitDepth The bit depth of the audo data
     * @param bitRate The bit rate of the audio
     * @param frequency The frequency at which the audio was sampled
     */
    public AudioFormat(@Nullable String container, @Nullable String codec, @Nullable Boolean bigEndian,
            @Nullable Integer bitDepth, @Nullable Integer bitRate, @Nullable Long frequency) {
        this.container = container;
        this.codec = codec;
        this.bigEndian = bigEndian;
        this.bitDepth = bitDepth;
        this.bitRate = bitRate;
        this.frequency = frequency;
    }

    /**
     * Gets codec
     *
     * @return The codec
     */
    public @Nullable String getCodec() {
        return codec;
    }

    /**
     * Gets container
     *
     * @return The container
     */
    public @Nullable String getContainer() {
        return container;
    }

    /**
     * Is big endian?
     *
     * @return If format is big endian
     */
    public @Nullable Boolean isBigEndian() {
        return bigEndian;
    }

    /**
     * Gets bit depth
     *
     * @see <a href="http://bit.ly/1OTydad">Bit Depth</a>
     * @return Bit depth
     */
    public @Nullable Integer getBitDepth() {
        return bitDepth;
    }

    /**
     * Gets bit rate
     *
     * @see <a href="http://bit.ly/1OTy5rk">Bit Rate</a>
     * @return Bit rate
     */
    public @Nullable Integer getBitRate() {
        return bitRate;
    }

    /**
     * Gets frequency
     *
     * @return The frequency
     */
    public @Nullable Long getFrequency() {
        return frequency;
    }

    /**
     * Determines if the passed AudioFormat is compatible with this AudioFormat.
     *
     * This AudioFormat is compatible with the passed AudioFormat if both have
     * the same value for all non-null members of this instance.
     */
    public boolean isCompatible(@Nullable AudioFormat audioFormat) {
        if (audioFormat == null) {
            return false;
        }
        if ((null != getContainer()) && (!getContainer().equals(audioFormat.getContainer()))) {
            return false;
        }
        if ((null != getCodec()) && (!getCodec().equals(audioFormat.getCodec()))) {
            return false;
        }
        if ((null != isBigEndian()) && (!isBigEndian().equals(audioFormat.isBigEndian()))) {
            return false;
        }
        if ((null != getBitDepth()) && (!getBitDepth().equals(audioFormat.getBitDepth()))) {
            return false;
        }
        if ((null != getBitRate()) && (!getBitRate().equals(audioFormat.getBitRate()))) {
            return false;
        }
        if ((null != getFrequency()) && (!getFrequency().equals(audioFormat.getFrequency()))) {
            return false;
        }
        return true;
    }

    /**
     * Determines the best match between a list of audio formats supported by a source and a sink.
     *
     * @param inputs the supported audio formats of an audio source
     * @param outputs the supported audio formats of an audio sink
     * @return the best matching format or null, if source and sink are incompatible
     */
    public static @Nullable AudioFormat getBestMatch(Set<AudioFormat> inputs, Set<AudioFormat> outputs) {
        AudioFormat preferredFormat = getPreferredFormat(inputs);
        if (preferredFormat != null) {
            for (AudioFormat output : outputs) {
                if (output.isCompatible(preferredFormat)) {
                    return preferredFormat;
                } else {
                    for (AudioFormat input : inputs) {
                        if (output.isCompatible(input)) {
                            return input;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets the first concrete AudioFormat in the passed set or a preferred one
     * based on 16bit, 16KHz, big endian default
     *
     * @param audioFormats The AudioFormats from which to choose
     * @return The preferred AudioFormat or null if none could be determined. A passed concrete format is preferred
     *         adding default values to an abstract AudioFormat in the passed set.
     */
    public static @Nullable AudioFormat getPreferredFormat(Set<AudioFormat> audioFormats) {
        // Return the first concrete AudioFormat found
        for (AudioFormat currentAudioFormat : audioFormats) {
            // Check if currentAudioFormat is abstract
            if (null == currentAudioFormat.getCodec()) {
                continue;
            }
            if (null == currentAudioFormat.getContainer()) {
                continue;
            }
            if (null == currentAudioFormat.isBigEndian()) {
                continue;
            }
            if (null == currentAudioFormat.getBitDepth()) {
                continue;
            }
            if (null == currentAudioFormat.getBitRate()) {
                continue;
            }
            if (null == currentAudioFormat.getFrequency()) {
                continue;
            }

            // Prefer WAVE container
            if (!CONTAINER_WAVE.equals(currentAudioFormat.getContainer())) {
                continue;
            }

            // As currentAudioFormat is concrete, use it
            return currentAudioFormat;
        }

        // There's no concrete AudioFormat so we must create one
        for (AudioFormat currentAudioFormat : audioFormats) {
            // Define AudioFormat to return
            AudioFormat format = currentAudioFormat;

            // Not all Codecs and containers can be supported
            if (null == format.getCodec()) {
                continue;
            }
            if (null == format.getContainer()) {
                continue;
            }

            // Prefer WAVE container
            if (!CONTAINER_WAVE.equals(format.getContainer())) {
                continue;
            }

            // If required set BigEndian, BitDepth, BitRate, and Frequency to default values
            if (null == format.isBigEndian()) {
                format = new AudioFormat(format.getContainer(), format.getCodec(), new Boolean(true),
                        format.getBitDepth(), format.getBitRate(), format.getFrequency());
            }
            if (null == format.getBitDepth() || null == format.getBitRate() || null == format.getFrequency()) {
                // Define default values
                int defaultBitDepth = 16;
                long defaultFrequency = 16384;

                // Obtain current values
                Integer bitRate = format.getBitRate();
                Long frequency = format.getFrequency();
                Integer bitDepth = format.getBitDepth();

                // These values must be interdependent (bitRate = bitDepth * frequency)
                if (null == bitRate) {
                    if (null == bitDepth) {
                        bitDepth = new Integer(defaultBitDepth);
                    }
                    if (null == frequency) {
                        frequency = new Long(defaultFrequency);
                    }
                    bitRate = new Integer(bitDepth.intValue() * frequency.intValue());
                } else if (null == bitDepth) {
                    if (null == frequency) {
                        frequency = new Long(defaultFrequency);
                    }
                    bitDepth = new Integer(bitRate.intValue() / frequency.intValue());
                } else if (null == frequency) {
                    frequency = new Long(bitRate.longValue() / bitDepth.longValue());
                }

                format = new AudioFormat(format.getContainer(), format.getCodec(), format.isBigEndian(), bitDepth,
                        bitRate, frequency);
            }

            // Return preferred AudioFormat
            return format;
        }

        // Return null indicating failure
        return null;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof AudioFormat) {
            AudioFormat format = (AudioFormat) obj;
            if (!(null == getCodec() ? null == format.getCodec() : getCodec().equals(format.getCodec()))) {
                return false;
            }
            if (!(null == getContainer() ? null == format.getContainer()
                    : getContainer().equals(format.getContainer()))) {
                return false;
            }
            if (!(null == isBigEndian() ? null == format.isBigEndian() : isBigEndian().equals(format.isBigEndian()))) {
                return false;
            }
            if (!(null == getBitDepth() ? null == format.getBitDepth() : getBitDepth().equals(format.getBitDepth()))) {
                return false;
            }
            if (!(null == getBitRate() ? null == format.getBitRate() : getBitRate().equals(format.getBitRate()))) {
                return false;
            }
            if (!(null == getFrequency() ? null == format.getFrequency()
                    : getFrequency().equals(format.getFrequency()))) {
                return false;
            }
            return true;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bigEndian == null) ? 0 : bigEndian.hashCode());
        result = prime * result + ((bitDepth == null) ? 0 : bitDepth.hashCode());
        result = prime * result + ((bitRate == null) ? 0 : bitRate.hashCode());
        result = prime * result + ((codec == null) ? 0 : codec.hashCode());
        result = prime * result + ((container == null) ? 0 : container.hashCode());
        result = prime * result + ((frequency == null) ? 0 : frequency.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "AudioFormat [" + (codec != null ? "codec=" + codec + ", " : "")
                + (container != null ? "container=" + container + ", " : "")
                + (bigEndian != null ? "bigEndian=" + bigEndian + ", " : "")
                + (bitDepth != null ? "bitDepth=" + bitDepth + ", " : "")
                + (bitRate != null ? "bitRate=" + bitRate + ", " : "")
                + (frequency != null ? "frequency=" + frequency : "") + "]";
    }
}
