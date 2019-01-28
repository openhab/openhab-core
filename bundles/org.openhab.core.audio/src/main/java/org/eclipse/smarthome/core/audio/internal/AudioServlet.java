/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.audio.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.core.audio.AudioException;
import org.eclipse.smarthome.core.audio.AudioFormat;
import org.eclipse.smarthome.core.audio.AudioHTTPServer;
import org.eclipse.smarthome.core.audio.AudioStream;
import org.eclipse.smarthome.core.audio.FixedLengthAudioStream;
import org.eclipse.smarthome.io.http.servlet.SmartHomeServlet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;

/**
 * A servlet that serves audio streams via HTTP.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
@Component
public class AudioServlet extends SmartHomeServlet implements AudioHTTPServer {

    private static final long serialVersionUID = -3364664035854567854L;

    private static final String SERVLET_NAME = "/audio";

    private final Map<String, AudioStream> oneTimeStreams = new ConcurrentHashMap<>();
    private final Map<String, FixedLengthAudioStream> multiTimeStreams = new ConcurrentHashMap<>();

    private final Map<String, Long> streamTimeouts = new ConcurrentHashMap<>();

    @Activate
    protected void activate() {
        super.activate(SERVLET_NAME);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate(SERVLET_NAME);
    }

    @Override
    @Reference
    protected void setHttpService(HttpService httpService) {
        super.setHttpService(httpService);
    }

    @Override
    public void unsetHttpService(HttpService httpService) {
        super.unsetHttpService(httpService);
    }

    private InputStream prepareInputStream(final String streamId, final HttpServletResponse resp)
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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        removeTimedOutStreams();

        final String streamId = StringUtils.substringBefore(StringUtils.substringAfterLast(req.getRequestURI(), "/"),
                ".");

        try (final InputStream stream = prepareInputStream(streamId, resp)) {
            if (stream == null) {
                logger.debug("Received request for invalid stream id at {}", req.getRequestURI());
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                IOUtils.copy(stream, resp.getOutputStream());
                resp.flushBuffer();
            }
        } catch (final AudioException ex) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private synchronized void removeTimedOutStreams() {
        for (String streamId : multiTimeStreams.keySet()) {
            if (streamTimeouts.get(streamId) < System.nanoTime()) {
                // the stream has expired, we need to remove it!
                FixedLengthAudioStream stream = multiTimeStreams.remove(streamId);
                streamTimeouts.remove(streamId);
                IOUtils.closeQuietly(stream);
                stream = null;
                logger.debug("Removed timed out stream {}", streamId);
            }
        }
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
