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
package org.openhab.core.audio.utils;

import java.io.ByteArrayInputStream;
import java.io.FilterOutputStream;
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
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some utility methods for sink
 *
 * @author Gwendal Roulleau - Initial contribution
 *
 */
@NonNullByDefault
@Component
public class AudioSinkUtilsImpl implements AudioSinkUtils {

    private final Logger logger = LoggerFactory.getLogger(AudioSinkUtilsImpl.class);

    @Override
    public @Nullable Long transferAndAnalyzeLength(InputStream in, OutputStream out, AudioFormat audioFormat,
            boolean flush) throws IOException {
        // take some data from the stream beginning
        byte[] dataBytes = in.readNBytes(8192);

        // beginning sound timestamp :
        long startTime = System.nanoTime();
        // copy already read data to the output stream :
        if (dataBytes.length > 0) {
            out.write(dataBytes);
            if (flush) {
                out.flush();
            }
        }
        // transfer everything else
        long transferred;
        if (flush) {
            transferred = in.transferTo(new FilterOutputStream(out) {
                @Override
                public void write(byte @Nullable [] b, int off, int len) throws IOException {
                    if (b != null && len > 0) {
                        out.write(b, off, len);
                        out.flush();
                    }
                }

                @Override
                public void write(int b) throws IOException {
                    out.write(b);
                    out.flush();
                }

                @Override
                public void flush() throws IOException {
                    out.flush();
                }
            });
        } else {
            transferred = in.transferTo(out);
        }
        long dataTransferredLength = dataBytes.length + transferred;

        if (dataTransferredLength > 0) {
            if (AudioFormat.CODEC_PCM_SIGNED.equals(audioFormat.getCodec())) {
                try (AudioInputStream audioInputStream = AudioSystem
                        .getAudioInputStream(new ByteArrayInputStream(dataBytes))) {
                    int frameSize = audioInputStream.getFormat().getFrameSize();
                    float frameRate = audioInputStream.getFormat().getFrameRate();
                    long computedDuration = Float
                            .valueOf((dataTransferredLength / (frameSize * frameRate)) * 1000000000).longValue();
                    return startTime + computedDuration;
                } catch (IOException | UnsupportedAudioFileException e) {
                    logger.debug("Cannot compute the duration of input stream with method java stream sound analysis",
                            e);
                    Integer bitRate = audioFormat.getBitRate();
                    if (bitRate != null && bitRate != 0) {
                        long computedDuration = Float.valueOf((8f * dataTransferredLength / bitRate) * 1000000000)
                                .longValue();
                        return startTime + computedDuration;
                    } else {
                        logger.debug("Cannot compute the duration of input stream by using audio format information");
                    }
                    return null;
                }
            } else if (AudioFormat.CODEC_MP3.equals(audioFormat.getCodec())) {
                // not accurate, no VBR support, but better than nothing
                Bitstream bitstream = new Bitstream(new ByteArrayInputStream(dataBytes));
                try {
                    Header h = bitstream.readFrame();
                    if (h != null) {
                        long computedDuration = Float.valueOf(h.total_ms((int) dataTransferredLength) * 1000000)
                                .longValue();
                        return startTime + computedDuration;
                    }
                } catch (BitstreamException ex) {
                    logger.debug("Cannot compute the duration of input stream", ex);
                    return null;
                }
            }
        }

        return null;
    }
}
