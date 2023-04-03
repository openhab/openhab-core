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
package org.openhab.core.audio.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Header;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;

/**
 * Some general filename and extension utilities.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class AudioStreamUtils {

    public static final String EXTENSION_SEPARATOR = ".";

    /**
     * Gets the base name of a filename.
     *
     * @param filename the filename to query
     * @return the base name of the file or an empty string if none exists or {@code null} if the filename is
     *         {@code null}
     */
    public static String getBaseName(String filename) {
        final int index = filename.lastIndexOf(EXTENSION_SEPARATOR);
        if (index == -1) {
            return filename;
        } else {
            return filename.substring(0, index);
        }
    }

    /**
     * Gets the extension of a filename.
     *
     * @param filename the filename to retrieve the extension of
     * @return the extension of the file or an empty string if none exists or {@code null} if the filename is
     *         {@code null}
     */
    public static String getExtension(String filename) {
        final int index = filename.lastIndexOf(EXTENSION_SEPARATOR);
        if (index == -1) {
            return "";
        } else {
            return filename.substring(index + 1);
        }
    }

    /**
     * Checks if the extension of a filename matches the given.
     *
     * @param filename the filename to check the extension of
     * @param extension the extension to check for
     * @return {@code true} if the filename has the specified extension
     */
    public static boolean isExtension(String filename, String extension) {
        return !extension.isEmpty() && getExtension(filename).equals(extension);
    }

    /**
     * Transfers data from an input stream to an output stream and computes on the fly its duration
     *
     * @param in the input stream giving audio data ta play
     * @param out the output stream receiving data to play
     * @return the timestamp (from System.nanoTime) when the sound should be fully played. Returns null if computing
     *         time fails.
     * @throws IOException if reading from the stream or writing to the stream failed
     */
    public static @Nullable Long transferAndAnalyzeLength(InputStream in, OutputStream out, AudioFormat audioFormat)
            throws IOException {
        // take some data from the stream beginning
        byte[] dataBytes = in.readNBytes(8192);

        // beginning sound timestamp :
        long startTime = System.nanoTime();
        // copy already read data to the output stream :
        out.write(dataBytes);
        // transfer everything else
        Long dataTransferedLength = dataBytes.length + in.transferTo(out);

        if (dataTransferedLength > 0) {
            if (AudioFormat.CODEC_PCM_SIGNED.equals(audioFormat.getCodec())) {
                try (AudioInputStream audioInputStream = AudioSystem
                        .getAudioInputStream(new ByteArrayInputStream(dataBytes))) {
                    int frameSize = audioInputStream.getFormat().getFrameSize();
                    float frameRate = audioInputStream.getFormat().getFrameRate();
                    long computedDuration = Float.valueOf((dataTransferedLength / (frameSize * frameRate)) * 1000000000)
                            .longValue();
                    return startTime + computedDuration;
                } catch (IOException | UnsupportedAudioFileException e) {
                    return null;
                }
            } else if (AudioFormat.CODEC_MP3.equals(audioFormat.getCodec())) {
                // not precise, no VBR, but better than nothing
                Bitstream bitstream = new Bitstream(new ByteArrayInputStream(dataBytes));
                try {
                    Header h = bitstream.readFrame();
                    if (h != null) {
                        long computedDuration = Float.valueOf(h.total_ms(dataTransferedLength.intValue()) * 1000000)
                                .longValue();
                        return startTime + computedDuration;
                    }
                } catch (BitstreamException ex) {
                    return null;
                }
            }
        }

        return null;
    }
}
