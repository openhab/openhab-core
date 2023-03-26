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
package org.openhab.core.audio.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioHTTPServer;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.FixedLengthAudioStream;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
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

    private static final long serialVersionUID = -3364664035854567854L;

    private static final List<String> WAV_MIME_TYPES = List.of("audio/wav", "audio/x-wav", "audio/vnd.wave");

    static final String SERVLET_PATH = "/audio";

    private final Logger logger = LoggerFactory.getLogger(AudioServlet.class);

    private final Map<String, AudioStream> oneTimeStreams = new ConcurrentHashMap<>();
    private final Map<String, FixedLengthAudioStream> multiTimeStreams = new ConcurrentHashMap<>();

    private final Map<String, Long> streamTimeouts = new ConcurrentHashMap<>();

    @Deactivate
    protected synchronized void deactivate() {
        multiTimeStreams.values().forEach(this::tryClose);
        multiTimeStreams.clear();
        streamTimeouts.clear();

        oneTimeStreams.values().forEach(this::tryClose);
        oneTimeStreams.clear();
    }

    private void tryClose(@Nullable AudioStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
        }
    }

    private @Nullable InputStream prepareInputStream(final String streamId, final HttpServletResponse resp,
            List<String> acceptedMimeTypes) throws AudioException {
        final AudioStream stream;
        final boolean multiAccess;
        if (oneTimeStreams.containsKey(streamId)) {
            stream = oneTimeStreams.remove(streamId);
            multiAccess = false;
        } else if (multiTimeStreams.containsKey(streamId)) {
            stream = multiTimeStreams.get(streamId);
            multiAccess = true;
        } else {
            return null;
        }

        logger.debug("Stream to serve is {}", streamId);

        // try to set the content-type, if possible
        final String mimeType;
        if (AudioFormat.CODEC_MP3.equals(stream.getFormat().getCodec())) {
            mimeType = "audio/mpeg";
        } else if (AudioFormat.CONTAINER_WAVE.equals(stream.getFormat().getContainer())) {
            mimeType = WAV_MIME_TYPES.stream().filter(acceptedMimeTypes::contains).findFirst().orElse("audio/wav");
        } else if (AudioFormat.CONTAINER_OGG.equals(stream.getFormat().getContainer())) {
            mimeType = "audio/ogg";
        } else {
            mimeType = null;
        }
        if (mimeType != null) {
            resp.setContentType(mimeType);
        }

        // try to set the content-length, if possible
        if (stream instanceof FixedLengthAudioStream) {
            final long size = ((FixedLengthAudioStream) stream).length();
            resp.setContentLength((int) size);
        }

        if (multiAccess) {
            // we need to care about concurrent access and have a separate stream for each thread
            return ((FixedLengthAudioStream) stream).getClonedStream();
        } else {
            return stream;
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
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        removeTimedOutStreams();

        String requestURI = req.getRequestURI();
        if (requestURI == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "requestURI is null");
            return;
        }

        final String streamId = substringBefore(substringAfterLast(requestURI, "/"), ".");

        List<String> acceptedMimeTypes = Stream.of(Objects.requireNonNullElse(req.getHeader("Accept"), "").split(","))
                .map(String::trim).collect(Collectors.toList());

        try (final InputStream stream = prepareInputStream(streamId, resp, acceptedMimeTypes)) {
            if (stream == null) {
                logger.debug("Received request for invalid stream id at {}", requestURI);
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                stream.transferTo(resp.getOutputStream());
                resp.flushBuffer();
            }
        } catch (final AudioException ex) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private synchronized void removeTimedOutStreams() {
        // Build list of expired streams.
        long now = System.nanoTime();
        final List<String> toRemove = streamTimeouts.entrySet().stream().filter(e -> e.getValue() < now)
                .map(Entry::getKey).collect(Collectors.toList());

        toRemove.forEach(streamId -> {
            // the stream has expired, we need to remove it!
            final FixedLengthAudioStream stream = multiTimeStreams.remove(streamId);
            streamTimeouts.remove(streamId);
            tryClose(stream);
            logger.debug("Removed timed out stream {}", streamId);
        });
    }

    @Override
    public String serve(AudioStream stream) {
        String streamId = UUID.randomUUID().toString();
        oneTimeStreams.put(streamId, stream);
        return getRelativeURL(streamId);
    }

    @Override
    public String serve(FixedLengthAudioStream stream, int seconds) {
        String streamId = UUID.randomUUID().toString();
        multiTimeStreams.put(streamId, stream);
        streamTimeouts.put(streamId, System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds));
        return getRelativeURL(streamId);
    }

    Map<String, FixedLengthAudioStream> getMultiTimeStreams() {
        return Collections.unmodifiableMap(multiTimeStreams);
    }

    Map<String, AudioStream> getOneTimeStreams() {
        return Collections.unmodifiableMap(oneTimeStreams);
    }

    private String getRelativeURL(String streamId) {
        return SERVLET_PATH + "/" + streamId;
    }
}
