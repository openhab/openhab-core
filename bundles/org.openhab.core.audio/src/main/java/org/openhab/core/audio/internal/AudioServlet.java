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
package org.openhab.core.audio.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serial;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioHTTPServer;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.ByteArrayAudioStream;
import org.openhab.core.audio.ClonableAudioStream;
import org.openhab.core.audio.FileAudioStream;
import org.openhab.core.audio.PipedAudioStream;
import org.openhab.core.audio.SizeableAudioStream;
import org.openhab.core.audio.StreamServed;
import org.openhab.core.audio.utils.AudioSinkUtils;
import org.openhab.core.common.Disposable;
import org.openhab.core.common.ThreadPoolManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletName;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A servlet that serves audio streams via HTTP.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component(service = { AudioHTTPServer.class, Servlet.class })
@HttpWhiteboardServletName(AudioServlet.SERVLET_PATH)
@HttpWhiteboardServletPattern(AudioServlet.SERVLET_PATH + "/*")
@NonNullByDefault
public class AudioServlet extends HttpServlet implements AudioHTTPServer {

    @Serial
    private static final long serialVersionUID = -3364664035854567854L;

    private static final List<String> WAV_MIME_TYPES = List.of("audio/wav", "audio/x-wav", "audio/vnd.wave");

    // A 1MB in memory buffer will help playing multiple times an AudioStream, if the sink cannot do otherwise
    private static final int ONETIME_STREAM_BUFFER_MAX_SIZE = 1048576;
    // 5MB max for a file buffer
    private static final int ONETIME_STREAM_FILE_MAX_SIZE = 5242880;

    static final String SERVLET_PATH = "/audio";

    private final Logger logger = LoggerFactory.getLogger(AudioServlet.class);

    private final Map<String, StreamServed> servedStreams = new ConcurrentHashMap<>();

    private final ScheduledExecutorService threadPool = ThreadPoolManager
            .getScheduledPool(ThreadPoolManager.THREAD_POOL_NAME_COMMON);
    @Nullable
    ScheduledFuture<?> periodicCleaner;

    private final AudioSinkUtils audioSinkUtils;

    @Activate
    public AudioServlet(@Reference AudioSinkUtils audioSinkUtils) {
        this.audioSinkUtils = audioSinkUtils;
    }

    @Deactivate
    protected synchronized void deactivate() {
        servedStreams.values().stream().map(StreamServed::audioStream).forEach(this::tryClose);
        servedStreams.clear();
    }

    private void tryClose(@Nullable AudioStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
            if (stream instanceof Disposable disposableStream) {
                try {
                    disposableStream.dispose();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private InputStream prepareInputStream(final StreamServed streamServed, final HttpServletResponse resp,
            List<String> acceptedMimeTypes) throws AudioException {
        logger.debug("Stream to serve is {}", streamServed.url());

        AudioStream audioStream = streamServed.audioStream();

        // try to set the content-type, if possible
        final String mimeType;
        if (AudioFormat.CODEC_MP3.equals(audioStream.getFormat().getCodec())) {
            mimeType = "audio/mpeg";
        } else if (AudioFormat.CONTAINER_WAVE.equals(audioStream.getFormat().getContainer())) {
            mimeType = WAV_MIME_TYPES.stream().filter(acceptedMimeTypes::contains).findFirst().orElse("audio/wav");
        } else if (AudioFormat.CONTAINER_OGG.equals(audioStream.getFormat().getContainer())) {
            mimeType = "audio/ogg";
        } else if (AudioFormat.CODEC_FLAC.equals(audioStream.getFormat().getCodec())
                || AudioFormat.CONTAINER_FLAC.equals(audioStream.getFormat().getContainer())) {
            mimeType = "audio/flac";
        } else {
            mimeType = null;
        }
        if (mimeType != null) {
            resp.setContentType(mimeType);
        }

        // Determine whether to serve with fixed Content-Length or via HTTP chunked transfer:
        // - SizeableAudioStream (e.g., FileAudioStream): full file with known length => set Content-Length
        // - non-sizeable (e.g., PipedAudioStream): live/on-the-fly chunked streaming => Transfer-Encoding: chunked
        if (audioStream instanceof SizeableAudioStream sizeableServedStream) {
            final long size = sizeableServedStream.length();
            resp.setContentLength((int) size);
        } else {
            // prevent proxies and browsers from caching dynamic stream chunks
            resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        }

        if (streamServed.multiTimeStream() && audioStream instanceof ClonableAudioStream clonableAudioStream) {
            // we need to care about concurrent access and have a separate stream for each thread
            return clonableAudioStream.getClonedStream();
        } else {
            return audioStream;
        }
    }

    private String substringAfterLast(String str, String separator) {
        int index = str.lastIndexOf(separator);
        return index == -1 || index == str.length() - separator.length() ? ""
                : str.substring(index + separator.length());
    }

    private String substringBefore(String str, String separator) {
        int index = str.indexOf(separator);
        return index == -1 ? str : str.substring(0, index);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String requestURI = req.getRequestURI();
        if (requestURI == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "requestURI is null");
            return;
        }

        final String streamId = substringBefore(substringAfterLast(requestURI, "/"), ".");

        List<String> acceptedMimeTypes = Stream.of(Objects.requireNonNullElse(req.getHeader("Accept"), "").split(","))
                .map(String::trim).toList();

        StreamServed servedStream = servedStreams.get(streamId);
        if (servedStream == null) {
            logger.debug("Received request for invalid stream id at {}", requestURI);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // we count the number of active process using the input stream
        AtomicInteger currentlyServedStream = servedStream.currentlyServedStream();
        if (currentlyServedStream.incrementAndGet() == 1 || servedStream.multiTimeStream()) {
            try (final InputStream stream = prepareInputStream(servedStream, resp, acceptedMimeTypes)) {
                // Protect active piped/chunked streams from timeout eviction during long transfers
                OutputStream out = new StreamTransferOutputStream(resp.getOutputStream(), servedStream.timeout(),
                        servedStream.multiTimeStream());
                // Only flush explicitly per chunk for non-sizeable piped streams for minimal latency
                // Static/sizeable streams use standard servlet container buffering for maximum throughput
                boolean flush = !(servedStream.audioStream() instanceof SizeableAudioStream);
                Long endOfPlayTimestamp = audioSinkUtils.transferAndAnalyzeLength(stream, out,
                        servedStream.audioStream().getFormat(), flush);
                // update timeout with the sound duration :
                if (endOfPlayTimestamp != null) {
                    servedStream.timeout().set(Math.max(servedStream.timeout().get(), endOfPlayTimestamp));
                    logger.debug(
                            "doGet endOfPlayTimestamp {} (delay from now {} nanoseconds) => new timeout timestamp {} nanoseconds",
                            endOfPlayTimestamp, endOfPlayTimestamp - System.nanoTime(), servedStream.timeout().get());
                }
                resp.flushBuffer();
                // try-with-resources automatically closes stream on exit of try_block
            } catch (final AudioException ex) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
            } finally {
                currentlyServedStream.decrementAndGet();
                if (!servedStream.multiTimeStream()) {
                    servedStreams.remove(streamId);
                    if (servedStream.audioStream() instanceof Disposable disposableStream) {
                        try {
                            disposableStream.dispose();
                        } catch (IOException ignored) {
                        }
                    }
                    servedStream.playEnd().complete(null);
                    logger.debug("Removed served stream {}", streamId);
                }
            }
        } else {
            logger.debug("Received request for already consumed stream id at {}", requestURI);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private synchronized void removeTimedOutStreams() {
        // Build list of expired streams.
        long now = System.nanoTime();
        final List<String> toRemove = servedStreams.entrySet().stream()
                .filter(e -> e.getValue().timeout().get() < now && e.getValue().currentlyServedStream().get() <= 0)
                .map(Entry::getKey).toList();

        toRemove.forEach(streamId -> {
            // the stream has expired and no one is using it, we need to remove it!
            StreamServed streamServed = servedStreams.remove(streamId);
            if (streamServed != null) {
                tryClose(streamServed.audioStream());
                // we can notify the caller of the stream consumption
                streamServed.playEnd().complete(null);
                logger.debug("Removed timed out stream {}", streamId);
            }
        });

        // Because the callback should be executed as soon as possible,
        // we cannot wait for the next doGet to perform a clean. So we have to schedule a periodic cleaner.
        ScheduledFuture<?> periodicCleanerLocal = periodicCleaner;
        if (!servedStreams.isEmpty()) {
            if (periodicCleanerLocal == null || periodicCleanerLocal.isDone()) {
                // reschedule a clean
                periodicCleaner = threadPool.scheduleWithFixedDelay(this::removeTimedOutStreams, 2, 2,
                        TimeUnit.SECONDS);
            }
        } else if (periodicCleanerLocal != null) { // no more stream to serve, shut the periodic cleaning thread:
            periodicCleanerLocal.cancel(true);
            periodicCleaner = null;
        }
    }

    @Override
    public String serve(AudioStream stream) {
        try {
            // In case the stream is never played, we cannot wait indefinitely before executing the callback.
            // so we set a timeout (even if this is a one time stream).
            return serve(stream, 10, false).url();
        } catch (IOException e) {
            logger.warn("Cannot precache the audio stream to serve it", e);
            return getRelativeURL("error");
        }
    }

    @Override
    public String serve(AudioStream stream, int seconds) {
        try {
            return serve(stream, seconds, true).url();
        } catch (IOException e) {
            logger.warn("Cannot precache the audio stream to serve it", e);
            return getRelativeURL("error");
        }
    }

    @Override
    public StreamServed serve(AudioStream originalStream, int seconds, boolean multiTimeStream) throws IOException {
        String streamId = UUID.randomUUID().toString();
        AudioStream audioStream = originalStream;
        // PipedAudioStream represents a forward-only live stream that cannot be rewound or cloned into a temp
        // file without destroying low-latency on-the-fly streaming. Only static/bufferable streams are cached
        // as ClonableAudioStream when multiTimeStream is requested.
        boolean canMultiTime = multiTimeStream && !(originalStream instanceof PipedAudioStream);
        if (!(originalStream instanceof ClonableAudioStream) && canMultiTime) {
            audioStream = createClonableInputStream(originalStream, streamId);
        }
        long timeOut = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        logger.debug("timeout {} seconds => timestamp {} nanoseconds", seconds, timeOut);
        CompletableFuture<@Nullable Void> playEnd = new CompletableFuture<@Nullable Void>();
        StreamServed streamToServe = new StreamServed(getRelativeURL(streamId), audioStream, new AtomicInteger(),
                new AtomicLong(timeOut), canMultiTime, playEnd);
        servedStreams.put(streamId, streamToServe);

        // try to clean, or a least launch the periodic cleanse:
        removeTimedOutStreams();

        return streamToServe;
    }

    private AudioStream createClonableInputStream(AudioStream stream, String streamId) throws IOException {
        byte[] dataBytes = stream.readNBytes(ONETIME_STREAM_BUFFER_MAX_SIZE + 1);
        AudioStream clonableAudioStreamResult;
        if (dataBytes.length <= ONETIME_STREAM_BUFFER_MAX_SIZE) {
            // we will use an in memory buffer to avoid disk operation
            clonableAudioStreamResult = new ByteArrayAudioStream(dataBytes, stream.getFormat());
        } else {
            // in memory max size exceeded, sound is too long, we will use a file
            File tempFile = Files.createTempFile(streamId, ".snd").toFile();
            tempFile.deleteOnExit();
            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                // copy already read data to file :
                outputStream.write(dataBytes);
                // copy the remaining stream data to a file.
                byte[] buf = new byte[8192];
                int length;
                // but with a limit
                int fileSize = ONETIME_STREAM_BUFFER_MAX_SIZE + 1;
                while ((length = stream.read(buf)) != -1 && fileSize < ONETIME_STREAM_FILE_MAX_SIZE) {
                    int lengthToWrite = Math.min(length, ONETIME_STREAM_FILE_MAX_SIZE - fileSize);
                    outputStream.write(buf, 0, lengthToWrite);
                    fileSize += lengthToWrite;
                }
            }
            try {
                clonableAudioStreamResult = new FileAudioStream(tempFile, stream.getFormat(), true);
            } catch (AudioException e) { // this is in fact a FileNotFoundException and should not happen
                throw new IOException("Cannot find the cache file we just created.", e);
            }
        }
        tryClose(stream);
        return clonableAudioStreamResult;
    }

    Map<String, StreamServed> getServedStreams() {
        return Collections.unmodifiableMap(servedStreams);
    }

    private String getRelativeURL(String streamId) {
        return SERVLET_PATH + "/" + streamId;
    }

    /**
     * An {@link OutputStream} wrapper that continuously extends the stream expiration timeout as bytes flow.
     * For on-the-fly piped audio streams, total duration cannot be predicted upfront; this ensures active
     * transfers are not prematurely evicted by {@link #removeTimedOutStreams()}.
     */
    private static class StreamTransferOutputStream extends FilterOutputStream {
        private final AtomicLong timeout;
        private final boolean multiTime;

        public StreamTransferOutputStream(OutputStream out, AtomicLong timeout, boolean multiTime) {
            super(out);
            this.timeout = timeout;
            this.multiTime = multiTime;
        }

        @Override
        public void write(byte @Nullable [] b, int off, int len) throws IOException {
            if (b != null && len > 0) {
                out.write(b, off, len);
                if (!multiTime) {
                    timeout.set(Math.max(timeout.get(), System.nanoTime() + TimeUnit.SECONDS.toNanos(10)));
                }
            }
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            if (!multiTime) {
                timeout.set(Math.max(timeout.get(), System.nanoTime() + TimeUnit.SECONDS.toNanos(10)));
            }
        }
    }
}
