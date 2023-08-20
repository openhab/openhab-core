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
package org.openhab.core.audio.internal.webaudio;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioHTTPServer;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSinkAsync;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.StreamServed;
import org.openhab.core.audio.UnsupportedAudioFormatException;
import org.openhab.core.audio.UnsupportedAudioStreamException;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.library.types.PercentType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an audio sink that publishes an event through SSE and temporarily serves the stream via HTTP for web players
 * to pick it up.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Christoph Weitkamp - Added getSupportedStreams() and UnsupportedAudioStreamException
 *
 */
@NonNullByDefault
@Component(service = AudioSink.class, immediate = true)
public class WebAudioAudioSink extends AudioSinkAsync {

    private final Logger logger = LoggerFactory.getLogger(WebAudioAudioSink.class);

    private static final Set<AudioFormat> SUPPORTED_AUDIO_FORMATS = Set.of(AudioFormat.MP3, AudioFormat.WAV);
    private static final Set<Class<? extends AudioStream>> SUPPORTED_AUDIO_STREAMS = Set.of(AudioStream.class);

    private AudioHTTPServer audioHTTPServer;
    private EventPublisher eventPublisher;

    @Activate
    public WebAudioAudioSink(@Reference AudioHTTPServer audioHTTPServer, @Reference EventPublisher eventPublisher) {
        this.audioHTTPServer = audioHTTPServer;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void processAsynchronously(@Nullable AudioStream audioStream)
            throws UnsupportedAudioFormatException, UnsupportedAudioStreamException {
        if (audioStream == null) {
            // in case the audioStream is null, this should be interpreted as a request to end any currently playing
            // stream.
            sendEvent("");
            return;
        }
        logger.debug("Received audio stream of format {}", audioStream.getFormat());

        // we need to serve it for a while and make it available to multiple clients
        try {
            StreamServed servedStream = audioHTTPServer.serve(audioStream, 10, true);
            // we will let the HTTP servlet run the delayed task when finished with the stream
            servedStream.playEnd().thenRun(() -> this.playbackFinished(audioStream));
            sendEvent(servedStream.url());
        } catch (IOException e) {
            logger.warn("Cannot precache the audio stream to serve it", e);
        }
    }

    private void sendEvent(String url) {
        PlayURLEvent event = WebAudioEventFactory.createPlayURLEvent(url);
        eventPublisher.post(event);
    }

    @Override
    public Set<AudioFormat> getSupportedFormats() {
        return SUPPORTED_AUDIO_FORMATS;
    }

    @Override
    public Set<Class<? extends AudioStream>> getSupportedStreams() {
        return SUPPORTED_AUDIO_STREAMS;
    }

    @Override
    public String getId() {
        return "webaudio";
    }

    @Override
    public @Nullable String getLabel(@Nullable Locale locale) {
        return "Web Audio";
    }

    @Override
    public PercentType getVolume() throws IOException {
        return PercentType.HUNDRED;
    }

    @Override
    public void setVolume(final PercentType volume) throws IOException {
        throw new IOException("Web Audio sink does not support volume level changes.");
    }
}
