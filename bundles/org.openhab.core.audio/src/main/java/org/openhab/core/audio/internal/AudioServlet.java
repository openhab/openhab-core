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
package org.openhab.core.audio.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioHTTPServer;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.FixedLengthAudioStream;
import org.openhab.core.io.http.servlet.SmartHomeServlet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * A servlet that serves audio streams via HTTP.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
@Component
public class AudioServlet extends SmartHomeServlet implements AudioHTTPServer {

    private static final long serialVersionUID = -3364664035854567854L;

    private static final String SERVLET_NAME = "/audio";

    private final Map<String, AudioStream> oneTimeStreams = new ConcurrentHashMap<>();
    private final Map<String, FixedLengthAudioStream> multiTimeStreams = new ConcurrentHashMap<>();

    private final Map<String, Long> streamTimeouts = new ConcurrentHashMap<>();

    @Activate
    public AudioServlet(final @Reference HttpService httpService, final @Reference HttpContext httpContext) {
        super(httpService, httpContext);
    }

    @Activate
    protected void activate() {
        super.activate(SERVLET_NAME);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate(SERVLET_NAME);
    }

    private @Nullable InputStream prepareInputStream(final String streamId, final HttpServletResponse resp)
            throws AudioException {
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
        if (stream.getFormat().getCodec() == AudioFormat.CODEC_MP3) {
            mimeType = "audio/mpeg";
        } else if (stream.getFormat().getContainer() == AudioFormat.CONTAINER_WAVE) {
            mimeType = "audio/wav";
        } else if (stream.getFormat().getContainer() == AudioFormat.CONTAINER_OGG) {
            mimeType = "audio/ogg";
        } else {
            mimeType = null;
        }
        if (mimeType != null) {
            resp.setContentType(mimeType);
        }

        // try to set the content-length, if possible
        if (stream instanceof FixedLengthAudioStream) {
            final Long size = ((FixedLengthAudioStream) stream).length();
            resp.setContentLength(size.intValue());
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
    protected void doGet(@NonNullByDefault({}) HttpServletRequest req, @NonNullByDefault({}) HttpServletResponse resp)
            throws ServletException, IOException {
        removeTimedOutStreams();

        final String streamId = substringBefore(substringAfterLast(req.getRequestURI(), "/"), ".");

        try (final InputStream stream = prepareInputStream(streamId, resp)) {
            if (stream == null) {
                logger.debug("Received request for invalid stream id at {}", req.getRequestURI());
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
        final List<String> toRemove = new LinkedList<>();
        for (Entry<String, Long> entry : streamTimeouts.entrySet()) {
            if (entry.getValue() < System.nanoTime()) {
                toRemove.add(entry.getKey());
            }
        }
        toRemove.forEach(streamId -> {
            // the stream has expired, we need to remove it!
            final FixedLengthAudioStream stream = multiTimeStreams.remove(streamId);
            streamTimeouts.remove(streamId);
            try {
                stream.close();
            } catch (IOException e) {
            }
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

    private String getRelativeURL(String streamId) {
        return SERVLET_NAME + "/" + streamId;
    }
}
