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

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.FileAudioStream;
import org.openhab.core.audio.PipedAudioStream;
import org.openhab.core.audio.SizeableAudioStream;
import org.openhab.core.audio.internal.transcode.SizeableTranscodedAudioStream;
import org.openhab.core.audio.internal.transcode.TranscodedAudioStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for transcoding {@link AudioStream}s using the Java Sound API.
 *
 * <p>
 * Supports transcoding between {@link AudioFormat#WAV}, {@link AudioFormat#PCM_SIGNED},
 * {@link AudioFormat#FLAC}, and all PCM variations supported by the Java Sound API
 * (signed/unsigned PCM, A-law, μ-law), including sample rate resampling, bit-depth
 * conversion, and endianness changes.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public final class AudioTranscoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioTranscoder.class);

    public static final Set<AudioFormat> SUPPORTED_SOURCE_FORMATS = Set.of(AudioFormat.WAV, AudioFormat.PCM_SIGNED,
            AudioFormat.FLAC, AudioFormat.PCM_UNSIGNED, AudioFormat.PCM_ALAW, AudioFormat.PCM_ULAW);

    public static final Set<AudioFormat> SUPPORTED_TARGET_FORMATS = Set.of(AudioFormat.WAV, AudioFormat.PCM_SIGNED,
            AudioFormat.FLAC, AudioFormat.PCM_UNSIGNED, AudioFormat.PCM_ALAW, AudioFormat.PCM_ULAW);

    private AudioTranscoder() {
        // Utility class
    }

    /**
     * Gets the set of general audio formats supported for decoding (reading as source stream).
     *
     * @return supported source audio formats
     */
    public static Set<AudioFormat> getSupportedSourceFormats() {
        return SUPPORTED_SOURCE_FORMATS;
    }

    /**
     * Gets the set of general audio formats supported for encoding (writing as target stream).
     *
     * @return supported target audio formats
     */
    public static Set<AudioFormat> getSupportedTargetFormats() {
        return SUPPORTED_TARGET_FORMATS;
    }

    /**
     * Checks if the given audio format is supported by this transcoder (either for decoding or encoding).
     *
     * @param format the format to check
     * @return true if the format is supported, false otherwise
     */
    public static boolean isSupported(@Nullable AudioFormat format) {
        return isSourceSupported(format) || isTargetSupported(format);
    }

    /**
     * Checks if the given audio format can be decoded from a source stream.
     *
     * @param format the format to check
     * @return true if the format can be decoded, false otherwise
     */
    public static boolean isSourceSupported(@Nullable AudioFormat format) {
        if (format == null) {
            return false;
        }
        String container = format.getContainer();
        String codec = format.getCodec();

        if (AudioFormat.CONTAINER_WAVE.equals(container)) {
            return codec == null || isPcmCodec(codec);
        }
        if (AudioFormat.CONTAINER_FLAC.equals(container)) {
            return codec == null || AudioFormat.CODEC_FLAC.equals(codec);
        }
        if (AudioFormat.CONTAINER_NONE.equals(container) || container == null) {
            return codec == null || isPcmCodec(codec) || AudioFormat.CODEC_FLAC.equals(codec);
        }
        return false;
    }

    /**
     * Checks if the given audio format can be encoded to a target stream.
     *
     * @param format the format to check
     * @return true if the format can be encoded, false otherwise
     */
    public static boolean isTargetSupported(@Nullable AudioFormat format) {
        if (format == null) {
            return false;
        }
        String container = format.getContainer();
        String codec = format.getCodec();

        if (AudioFormat.CONTAINER_WAVE.equals(container)) {
            return codec == null || isPcmCodec(codec);
        }
        if (AudioFormat.CONTAINER_FLAC.equals(container)) {
            return codec == null || AudioFormat.CODEC_FLAC.equals(codec);
        }
        if (AudioFormat.CONTAINER_NONE.equals(container) || container == null) {
            return codec == null || isPcmCodec(codec);
        }
        return false;
    }

    /**
     * Checks whether this transcoder can transcode from the source format to the target format.
     *
     * @param sourceFormat the source format
     * @param targetFormat the target format
     * @return true if transcoding is possible, false otherwise
     */
    public static boolean canTranscode(@Nullable AudioFormat sourceFormat, @Nullable AudioFormat targetFormat) {
        if (sourceFormat == null || targetFormat == null) {
            return false;
        }

        if (!isSourceSupported(sourceFormat) || !isTargetSupported(targetFormat)) {
            return false;
        }

        // remixing is not supported:
        Integer sourceChannels = sourceFormat.getChannels();
        Integer targetChannels = targetFormat.getChannels();
        if (sourceChannels != null && targetChannels != null && !sourceChannels.equals(targetChannels)) {
            return false;
        }

        // only specific bit depths are supported:
        Integer sourceBitDepth = sourceFormat.getBitDepth();
        if (sourceBitDepth != null && sourceBitDepth != 8 && sourceBitDepth != 16 && sourceBitDepth != 24
                && sourceBitDepth != 32) {
            return false;
        }
        Integer targetBitDepth = targetFormat.getBitDepth();
        if (targetBitDepth != null && targetBitDepth != 8 && targetBitDepth != 16 && targetBitDepth != 24
                && targetBitDepth != 32) {
            return false;
        }

        return true;
    }

    /**
     * Transcodes the given audio stream to the requested target format.
     *
     * <p>
     * Favors streaming via {@link PipedAudioStream} for lower latency without disk buffering.
     *
     * @param source the audio stream to transcode
     * @param targetFormat the desired target format
     * @return a new audio stream in the requested target format
     * @throws AudioTranscodingException if transcoding fails or formats are unsupported
     */
    public static AudioStream transcode(AudioStream source, AudioFormat targetFormat) throws AudioTranscodingException {
        return transcode(source, targetFormat, false);
    }

    /**
     * Transcodes the given audio stream to the requested target format, taking into account supported stream types.
     *
     * @param source the audio stream to transcode
     * @param targetFormat the desired target format
     * @param supportedStreams the set of stream classes supported by the consumer/sink
     * @return a new audio stream in the requested target format
     * @throws AudioTranscodingException if transcoding fails or formats are unsupported
     */
    public static AudioStream transcode(AudioStream source, AudioFormat targetFormat,
            Set<Class<? extends AudioStream>> supportedStreams) throws AudioTranscodingException {
        boolean requireSizeable = supportedStreams.stream()
                .noneMatch(clazz -> clazz.isAssignableFrom(PipedAudioStream.class));
        return transcode(source, targetFormat, requireSizeable);
    }

    /**
     * Transcodes the given audio stream to the requested target format.
     *
     * @param source the audio stream to transcode
     * @param targetFormat the desired target format
     * @param requireSizeable true to buffer to disk and return a {@link SizeableAudioStream}, false to stream via pipe
     * @return a new audio stream in the requested target format
     * @throws AudioTranscodingException if transcoding fails or formats are unsupported
     */
    public static AudioStream transcode(AudioStream source, AudioFormat targetFormat, boolean requireSizeable)
            throws AudioTranscodingException {
        Objects.requireNonNull(source, "source audio stream must not be null");
        Objects.requireNonNull(targetFormat, "targetFormat must not be null");

        if (targetFormat.isCompatible(source.getFormat())) {
            if (!requireSizeable || source instanceof SizeableAudioStream) {
                return source;
            }
        }

        if (!isSourceSupported(source.getFormat())) {
            closeQuietly(source);
            throw new AudioTranscodingException(
                    "Source format " + source.getFormat() + " is not supported for decoding");
        }
        if (!isTargetSupported(targetFormat)) {
            closeQuietly(source);
            throw new AudioTranscodingException("Target format " + targetFormat + " is not supported for encoding");
        }
        if (!canTranscode(source.getFormat(), targetFormat)) {
            closeQuietly(source);
            throw new AudioTranscodingException(
                    "Transcoding from " + source.getFormat() + " to " + targetFormat + " is not supported");
        }

        AudioFormat sourceFormat = source.getFormat();
        AudioInputStream inAis;

        try {
            if (AudioFormat.CONTAINER_NONE.equals(sourceFormat.getContainer())) {
                javax.sound.sampled.AudioFormat sourceJFormat = toJavaSoundFormat(sourceFormat);
                long frameLength = AudioSystem.NOT_SPECIFIED;
                if (source instanceof SizeableAudioStream sizeable && sourceJFormat.getFrameSize() > 0) {
                    frameLength = sizeable.length() / sourceJFormat.getFrameSize();
                }
                inAis = new AudioInputStream(source, sourceJFormat, frameLength);
            } else {
                // Java Sound API requires markable input streams to read container headers
                InputStream markable = source.markSupported() ? source : new BufferedInputStream(source);
                inAis = AudioSystem.getAudioInputStream(markable);
            }
        } catch (UnsupportedAudioFileException | IOException | IllegalArgumentException e) {
            closeQuietly(source);
            throw new AudioTranscodingException("Failed to read source audio stream: " + e.getMessage(), e);
        }

        try {
            javax.sound.sampled.AudioFormat sourceJFormat = inAis.getFormat();

            float targetSampleRate = targetFormat.getFrequency() != null ? targetFormat.getFrequency().floatValue()
                    : sourceJFormat.getSampleRate();

            int targetBitDepth = targetFormat.getBitDepth() != null ? targetFormat.getBitDepth()
                    : (sourceJFormat.getSampleSizeInBits() > 0 ? sourceJFormat.getSampleSizeInBits() : 16);

            int targetChannels = targetFormat.getChannels() != null ? targetFormat.getChannels()
                    : (sourceJFormat.getChannels() > 0 ? sourceJFormat.getChannels() : 1);

            boolean targetBigEndian;
            if (targetFormat.isBigEndian() != null) {
                targetBigEndian = Boolean.TRUE.equals(targetFormat.isBigEndian());
            } else if (AudioFormat.CONTAINER_WAVE.equals(targetFormat.getContainer())) {
                targetBigEndian = false;
            } else {
                targetBigEndian = sourceJFormat.isBigEndian();
            }

            Encoding targetEncoding = toJavaSoundEncoding(targetFormat.getCodec());
            if (targetEncoding.equals(Encoding.ALAW) || targetEncoding.equals(Encoding.ULAW)) {
                targetBitDepth = 8;
            }

            int targetFrameSize = ((targetBitDepth + 7) / 8) * targetChannels; // rounds up to the next byte before
                                                                               // multiply by channels
            float targetFrameRate = targetSampleRate;

            javax.sound.sampled.AudioFormat targetJFormat = new javax.sound.sampled.AudioFormat(targetEncoding,
                    targetSampleRate, targetBitDepth, targetChannels, targetFrameSize, targetFrameRate,
                    targetBigEndian);

            AudioInputStream convertedAis = inAis;
            if (!sourceJFormat.matches(targetJFormat)) {
                convertedAis = convertAudioInputStream(inAis, sourceJFormat, targetJFormat);
            }

            String targetContainer = targetFormat.getContainer();
            String resultCodec = toOpenHabCodec(targetEncoding, targetContainer);
            int bitRate = Math.round(targetSampleRate * targetBitDepth * targetChannels);
            AudioFormat resultFormat = new AudioFormat(
                    targetContainer != null ? targetContainer : AudioFormat.CONTAINER_NONE, resultCodec,
                    targetBigEndian, targetBitDepth, bitRate, (long) targetSampleRate, targetChannels);

            if (AudioFormat.CONTAINER_FLAC.equals(targetContainer)) {
                AudioFileFormat.Type flacFileType = getFlacFileType(convertedAis);
                if (requireSizeable) {
                    return writeToTempFile(source, convertedAis, flacFileType, resultFormat);
                } else {
                    return writeToPipedStream(source, convertedAis, flacFileType, resultFormat);
                }
            } else if (AudioFormat.CONTAINER_WAVE.equals(targetContainer)) {
                return writeToTempFile(source, convertedAis, AudioFileFormat.Type.WAVE, resultFormat);
            } else {
                // Raw PCM stream
                long frameLength = convertedAis.getFrameLength();
                if (frameLength >= 0) {
                    return new SizeableTranscodedAudioStream(resultFormat, source.getId(), convertedAis,
                            frameLength * targetFrameSize);
                }
                if (requireSizeable) {
                    return writeToTempFile(source, convertedAis, AudioFileFormat.Type.WAVE, resultFormat);
                }
                return new TranscodedAudioStream(resultFormat, source.getId(), convertedAis);
            }
        } catch (AudioTranscodingException ate) {
            closeQuietly(convertedAisOrSource(inAis, source));
            throw ate;
        } catch (RuntimeException re) {
            closeQuietly(convertedAisOrSource(inAis, source));
            throw new AudioTranscodingException("Transcoding failed: " + re.getMessage(), re);
        }
    }

    /**
     * Transcodes the audio stream to a format supported by candidateFormats (e.g. from an AudioSink).
     *
     * @param source the audio stream to transcode
     * @param candidateFormats the acceptable target formats
     * @return an audio stream compatible with one of the candidateFormats
     * @throws AudioTranscodingException if no route to any candidate format exists or transcoding fails
     */
    public static AudioStream transcodeToSupported(AudioStream source, Set<AudioFormat> candidateFormats)
            throws AudioTranscodingException {
        return transcodeToSupported(source, candidateFormats, false);
    }

    /**
     * Transcodes the audio stream to a format and stream type supported by candidateFormats and supportedStreams (e.g.
     * from an AudioSink).
     *
     * @param source the audio stream to transcode
     * @param candidateFormats the acceptable target formats
     * @param supportedStreams the set of stream classes supported by the consumer/sink
     * @return an audio stream compatible with one of the candidateFormats
     * @throws AudioTranscodingException if no route to any candidate format exists or transcoding fails
     */
    public static AudioStream transcodeToSupported(AudioStream source, Set<AudioFormat> candidateFormats,
            Set<Class<? extends AudioStream>> supportedStreams) throws AudioTranscodingException {
        boolean requireSizeable = supportedStreams.stream()
                .noneMatch(clazz -> clazz.isAssignableFrom(PipedAudioStream.class));
        return transcodeToSupported(source, candidateFormats, requireSizeable);
    }

    /**
     * Transcodes the audio stream to a format supported by candidateFormats (e.g. from an AudioSink).
     *
     * @param source the audio stream to transcode
     * @param candidateFormats the acceptable target formats
     * @param requireSizeable true to buffer to disk and return a {@link SizeableAudioStream}, false to stream via pipe
     * @return an audio stream compatible with one of the candidateFormats
     * @throws AudioTranscodingException if no route to any candidate format exists or transcoding fails
     */
    public static AudioStream transcodeToSupported(AudioStream source, Set<AudioFormat> candidateFormats,
            boolean requireSizeable) throws AudioTranscodingException {
        Objects.requireNonNull(source, "source audio stream must not be null");
        Objects.requireNonNull(candidateFormats, "candidateFormats must not be null");

        AudioFormat sourceFormat = source.getFormat();
        for (AudioFormat candidate : candidateFormats) {
            if (candidate.isCompatible(sourceFormat)) {
                if (!requireSizeable || source instanceof SizeableAudioStream) {
                    return source;
                }
            }
        }

        if (!isSourceSupported(sourceFormat)) {
            throw new AudioTranscodingException("Source format " + sourceFormat + " is not supported for decoding");
        }

        AudioFormat target = null;
        for (AudioFormat candidate : candidateFormats) {
            if (isTargetSupported(candidate) && canTranscode(sourceFormat, candidate)) {
                if (AudioFormat.CONTAINER_WAVE.equals(candidate.getContainer())) {
                    target = candidate;
                    break;
                } else if (AudioFormat.CONTAINER_FLAC.equals(candidate.getContainer())) {
                    if (target == null || !AudioFormat.CONTAINER_WAVE.equals(target.getContainer())) {
                        target = candidate;
                    }
                } else if (target == null) {
                    target = candidate;
                }
            }
        }

        if (target == null) {
            throw new AudioTranscodingException("No supported transcoding target found for source format "
                    + sourceFormat + " among candidates " + candidateFormats);
        }

        return transcode(source, target, requireSizeable);
    }

    /**
     * Converts a Java {@link AudioInputStream} to from and to a Java {@link javax.sound.sampled.AudioFormat} using the
     * Java Sound API.
     *
     * <p>
     * If direct conversion to the target format is not possible, conversion via intermediate signed PCM format is
     * attempted.
     *
     * @param inAis the audio input stream to convert
     * @param sourceJFormat the format of the audio input stream
     * @param targetJFormat the target format to convert to
     * @return the converted audio input stream
     * @throws AudioTranscodingException when conversion is not supported or fails
     */
    private static AudioInputStream convertAudioInputStream(AudioInputStream inAis,
            javax.sound.sampled.AudioFormat sourceJFormat, javax.sound.sampled.AudioFormat targetJFormat)
            throws AudioTranscodingException {
        try {
            if (AudioSystem.isConversionSupported(targetJFormat, sourceJFormat)) {
                return AudioSystem.getAudioInputStream(targetJFormat, inAis);
            }

            // Attempt 2-step conversion via intermediate PCM_SIGNED at source sample rate
            javax.sound.sampled.AudioFormat intermediate = new javax.sound.sampled.AudioFormat(Encoding.PCM_SIGNED,
                    sourceJFormat.getSampleRate(), 16, sourceJFormat.getChannels(), 2 * sourceJFormat.getChannels(),
                    sourceJFormat.getSampleRate(), false);
            if (AudioSystem.isConversionSupported(intermediate, sourceJFormat)
                    && AudioSystem.isConversionSupported(targetJFormat, intermediate)) {
                AudioInputStream intermediateAis = AudioSystem.getAudioInputStream(intermediate, inAis);
                return AudioSystem.getAudioInputStream(targetJFormat, intermediateAis);
            }

            throw new AudioTranscodingException(
                    "Java Sound conversion not supported: " + sourceJFormat + " -> " + targetJFormat);
        } catch (IllegalArgumentException e) {
            throw new AudioTranscodingException("Failed to convert audio stream: " + e.getMessage(), e);
        }
    }

    /**
     * Asynchronously transcodes an audio stream into a {@link PipedAudioStream} for low-latency,
     * forward-only pipeline streaming without disk I/O.
     *
     * <p>
     * Encoding runs in a background thread via {@link CompletableFuture#runAsync(Runnable)}, writing directly
     * into a {@link PipedAudioStream.Group} while consumers immediately start reading the resulting stream.
     * Because the destination is a non-seekable {@link java.io.OutputStream}, the underlying encoder cannot
     * seek back to update header metadata once encoding completes (e.g. retroactive MD5 checksums, or file sizes
     * that were not known ahead of time).
     *
     * <p>
     * Both the {@code source} stream and the converted {@code ais} are closed automatically in the background
     * thread upon completion or failure.
     *
     * @param source the source openHAB {@link AudioStream} to be closed after processing
     * @param ais the converted Java Sound {@link AudioInputStream} to encode
     * @param fileType the target {@link AudioFileFormat.Type} container format
     * @param resultFormat the target openHAB {@link AudioFormat} metadata for the resulting stream
     * @return an {@link AudioStream} connected to the write end of the piped group
     * @throws AudioTranscodingException if the piped output stream fails to initialize
     */
    private static AudioStream writeToPipedStream(AudioStream source, AudioInputStream ais,
            AudioFileFormat.Type fileType, AudioFormat resultFormat) throws AudioTranscodingException {
        PipedAudioStream.Group group = PipedAudioStream.newGroup(resultFormat);
        PipedAudioStream outputStream;
        try {
            outputStream = group.getAudioStreamInGroup();
        } catch (IOException e) {
            closeQuietly(ais);
            closeQuietly(source);
            throw new AudioTranscodingException("Failed to initialize piped output stream: " + e.getMessage(), e);
        }

        CompletableFuture.runAsync(() -> {
            try (source; ais; group) {
                AudioSystem.write(ais, fileType, group);
            } catch (Exception e) {
                LOGGER.warn("Transcoding write finished with exception: {}", e.getMessage(), e);
            }
        });

        return outputStream;
    }

    /**
     * Transcodes an audio input stream into a concrete audio file container by buffering the output in a temporary
     * file.
     *
     * <p>
     * Writing directly to a {@link java.io.File} via
     * {@link AudioSystem#write(AudioInputStream, AudioFileFormat.Type, java.io.File)}
     * is required for audio container formats whose header metadata cannot be fully populated in a single forward-only
     * pass
     * (e.g., FLAC {@code STREAMINFO} total sample count and MD5 audio checksums, or WAV RIFF chunk sizes when the input
     * frame
     * length is unknown upfront). A seekable file target allows SPI encoders to rewind to the beginning and backfill
     * missing header fields once encoding completes.
     *
     * <p>
     * The returned {@link FileAudioStream} is configured to delete the underlying temporary file when closed.
     * Both the {@code source} stream and the converted {@code ais} are closed upon completing the write operation
     * or if an error occurs.
     *
     * @param source the source openHAB {@link AudioStream} to be closed after processing
     * @param ais the converted Java Sound {@link AudioInputStream} to encode
     * @param fileType the target {@link AudioFileFormat.Type} container format (e.g., WAVE, FLAC)
     * @param resultFormat the target openHAB {@link AudioFormat} metadata for the resulting stream
     * @return a {@link FileAudioStream} backed by the generated temporary file, scheduled for deletion on close
     * @throws AudioTranscodingException if creating the temp file, encoding, or wrapping the result fails
     */
    private static AudioStream writeToTempFile(AudioStream source, AudioInputStream ais, AudioFileFormat.Type fileType,
            AudioFormat resultFormat) throws AudioTranscodingException {
        Path tempFile;
        try {
            tempFile = Files.createTempFile("transcoded-", "." + fileType.getExtension());
            tempFile.toFile().deleteOnExit();
            try (source; ais) {
                AudioSystem.write(ais, fileType, tempFile.toFile());
            }
            return new FileAudioStream(tempFile.toFile(), resultFormat, true);
        } catch (IOException | AudioException e) {
            closeQuietly(ais);
            closeQuietly(source);
            throw new AudioTranscodingException("Failed to transcode audio stream to temporary file: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Returns the Java Sound {@link AudioFileFormat.Type} of the given FLAC audio stream.
     *
     * <p>
     * As FLAC is no Java Sound built-in, there is no static member for it on {@link AudioFileFormat.Type}.
     *
     * @param ais the Java Sound {@link AudioInputStream} to check
     * @return the FLAC Java Sound {@link AudioFileFormat.Type}
     */
    private static AudioFileFormat.Type getFlacFileType(AudioInputStream ais) {
        for (AudioFileFormat.Type type : AudioSystem.getAudioFileTypes(ais)) {
            if ("FLAC".equalsIgnoreCase(type.toString()) || "FLAC".equalsIgnoreCase(type.getExtension())) {
                return type;
            }
        }
        return new AudioFileFormat.Type("FLAC", "flac");
    }

    /**
     * Converts an openHAB {@link AudioFormat} to a Java Sound {@link javax.sound.sampled.AudioFormat}.
     *
     * @param format the openHAB {@link AudioFormat} to convert
     * @return the Java Sound {@link javax.sound.sampled.AudioFormat}
     * @throws AudioTranscodingException if Java Sound does not support the codec
     */
    private static javax.sound.sampled.AudioFormat toJavaSoundFormat(AudioFormat format)
            throws AudioTranscodingException {
        float sampleRate = format.getFrequency() != null ? format.getFrequency().floatValue() : 44100.0f;
        int bitDepth = format.getBitDepth() != null ? format.getBitDepth() : 16;
        int channels = format.getChannels() != null ? format.getChannels() : 1;
        boolean bigEndian = Boolean.TRUE.equals(format.isBigEndian());

        Encoding encoding = toJavaSoundEncoding(format.getCodec());
        int frameSize;
        if (encoding.equals(Encoding.ALAW) || encoding.equals(Encoding.ULAW)) {
            frameSize = channels;
            bitDepth = 8;
        } else {
            frameSize = ((bitDepth + 7) / 8) * channels; // rounds up to the next byte before multiply by channels
        }
        return new javax.sound.sampled.AudioFormat(encoding, sampleRate, bitDepth, channels, frameSize, sampleRate,
                bigEndian);
    }

    /**
     * Converts an openHAB {@link AudioFormat#getCodec()} codec to a Java Sound {@link Encoding}.
     *
     * @param codec an {@link AudioFormat#getCodec()} codec
     * @return the Java Sound {@link Encoding}
     * @throws AudioTranscodingException if Java Sound does not support the codec
     */
    private static Encoding toJavaSoundEncoding(@Nullable String codec) throws AudioTranscodingException {
        if (AudioFormat.CODEC_PCM_SIGNED.equals(codec)) {
            return Encoding.PCM_SIGNED;
        } else if (AudioFormat.CODEC_PCM_UNSIGNED.equals(codec)) {
            return Encoding.PCM_UNSIGNED;
        } else if (AudioFormat.CODEC_PCM_ALAW.equals(codec)) {
            return Encoding.ALAW;
        } else if (AudioFormat.CODEC_PCM_ULAW.equals(codec)) {
            return Encoding.ULAW;
        } else if (AudioFormat.CODEC_FLAC.equals(codec)) {
            return Encoding.PCM_SIGNED; // Tested to work with FLAC
        }
        throw new AudioTranscodingException("No Java Sound encoding available for '" + codec + "'!");
    }

    /***
     * Converts a Java Sound {@link Encoding} to an openHAB {@link AudioFormat#getCodec()} codec.
     *
     * @param encoding a Java Sound {@link Encoding}
     * @param container the container of the audio stream, e.g. {@link AudioFormat#CONTAINER_WAVE} or
     *            {@link AudioFormat#CONTAINER_FLAC}
     * @return the openHAB {@link AudioFormat#getCodec()} codec
     */
    private static String toOpenHabCodec(Encoding encoding, @Nullable String container) {
        if (AudioFormat.CONTAINER_FLAC.equals(container)) {
            return AudioFormat.CODEC_FLAC;
        }
        if (Encoding.PCM_SIGNED.equals(encoding)) {
            return AudioFormat.CODEC_PCM_SIGNED;
        } else if (Encoding.PCM_UNSIGNED.equals(encoding)) {
            return AudioFormat.CODEC_PCM_UNSIGNED;
        } else if (Encoding.ALAW.equals(encoding)) {
            return AudioFormat.CODEC_PCM_ALAW;
        } else if (Encoding.ULAW.equals(encoding)) {
            return AudioFormat.CODEC_PCM_ULAW;
        }
        return encoding.toString();
    }

    /**
     * Returns whether the given codec is a PCM codec.
     *
     * @param codec a openHAB {@link AudioFormat#getCodec()} codec
     * @return true if the codec is a PCM codec, false otherwise
     */
    private static boolean isPcmCodec(String codec) {
        return AudioFormat.CODEC_PCM_SIGNED.equals(codec) || AudioFormat.CODEC_PCM_UNSIGNED.equals(codec)
                || AudioFormat.CODEC_PCM_ALAW.equals(codec) || AudioFormat.CODEC_PCM_ULAW.equals(codec);
    }

    private static Closeable convertedAisOrSource(@Nullable AudioInputStream ais, AudioStream source) {
        return ais != null ? ais : source;
    }

    private static void closeQuietly(@Nullable Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
            }
        }
    }
}
