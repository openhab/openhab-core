/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.utils.AudioStreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lindstrom.mpd.MPDParser;
import io.lindstrom.mpd.data.AdaptationSet;
import io.lindstrom.mpd.data.MPD;
import io.lindstrom.mpd.data.Period;
import io.lindstrom.mpd.data.Representation;
import io.lindstrom.mpd.data.Segment;
import io.lindstrom.mpd.data.SegmentTemplate;

/**
 * This is an AudioStream from a URL. Note that some sinks, like Sonos, can directly handle URL
 * based streams, and therefore can/should call getURL() to get a direct reference to the URL.
 *
 * @author Karel Goderis - Initial contribution
 * @author Kai Kreuzer - Refactored to not require a source
 * @author Christoph Weitkamp - Refactored use of filename extension
 */
@NonNullByDefault
public class URLAudioStream extends AudioStream implements ClonableAudioStream {

    private static final Pattern PLS_STREAM_PATTERN = Pattern.compile("^File[0-9]=(.+)$");

    public static final String M3U_EXTENSION = "m3u";
    public static final String PLS_EXTENSION = "pls";
    public static final String MPD_EXTENSION = "mpd";

    private final Logger logger = LoggerFactory.getLogger(URLAudioStream.class);

    private final AudioFormat audioFormat;
    private final InputStream inputStream;
    private String url;

    private @Nullable Socket shoutCastSocket;

    public URLAudioStream(String url) throws AudioException {
        this.url = url;
        this.audioFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_MP3, false, 16, null, null);
        this.inputStream = createInputStream();
    }

    public URLAudioStream(InputStream inputStream, String artist, String title) throws AudioException {
        this.url = "";
        this.audioFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_MP3, false, 16, null, null);
        if (inputStream instanceof LazzyLoadingAudioStream) {
            this.inputStream = inputStream;
        } else {
            this.inputStream = createInputStream(inputStream, artist, title);
        }
    }

    private InputStream createInputStream(InputStream inputStream, String artist, String title) throws AudioException {
        try {
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            if (content.contains("urn:mpeg:dash:schema:mpd")) {
                MPDParser parser = new MPDParser();
                content = content.replace("<Label>FLAC_HIRES</Label>", "");
                MPD mpd = parser.parse(content);

                List<Period> periodList = mpd.getPeriods();
                if (periodList.size() > 1) {
                    logger.debug("We only support simple period mpd at this time");
                    throw new AudioException("NIY");
                }

                Period period = periodList.get(0);
                List<AdaptationSet> adaptationSetList = period.getAdaptationSets();
                AdaptationSet adaptationSet = adaptationSetList.get(0);

                List<Representation> representationList = adaptationSet.getRepresentations();
                Representation representation = representationList.get(0);

                SegmentTemplate segmentTemplate = representation.getSegmentTemplate();
                String initializationUri = segmentTemplate.getInitialization();
                String mediaUri = segmentTemplate.getMedia();

                List<Segment> segmentList = segmentTemplate.getSegmentTimeline();
                Segment segment = segmentList.get(0);
                long r = segment.getR();

                List<URL> urls = new ArrayList();
                urls.add(new URI(initializationUri).toURL());
                for (int i = 1; i < r; i++) {
                    logger.info("Segment:" + i);
                    String downloadUri = mediaUri.replace("$Number$", "" + i);
                    urls.add(new URI(downloadUri).toURL());
                }

                LazzyLoadingAudioStream stream = new LazzyLoadingAudioStream(urls, artist, title);
                return stream;
            }

            throw new AudioException("NIY");
        } catch (Exception ex) {
            throw new AudioException(ex);
        }
    }

    private InputStream createInputStream() throws AudioException {
        final String filename = url.toLowerCase();
        final String extension = AudioStreamUtils.getExtension(filename);

        try {
            URL streamUrl = new URI(url).toURL();
            switch (extension) {
                case M3U_EXTENSION:
                    try (Scanner scanner = new Scanner(streamUrl.openStream(), StandardCharsets.UTF_8.name())) {
                        while (true) {
                            String line = scanner.nextLine();
                            if (!line.isEmpty() && !line.startsWith("#")) {
                                url = line;
                                break;
                            }
                        }
                    } catch (NoSuchElementException e) {
                        // we reached the end of the file, this exception is thus expected
                    }
                    break;
                case PLS_EXTENSION:
                    try (Scanner scanner = new Scanner(streamUrl.openStream(), StandardCharsets.UTF_8.name())) {
                        while (true) {
                            String line = scanner.nextLine();
                            if (!line.isEmpty() && line.startsWith("File")) {
                                final Matcher matcher = PLS_STREAM_PATTERN.matcher(line);
                                if (matcher.find()) {
                                    url = matcher.group(1);
                                    break;
                                }
                            }
                        }
                    } catch (NoSuchElementException e) {
                        // we reached the end of the file, this exception is thus expected
                    }
                    break;
                case MPD_EXTENSION:
                    logger.info("=============================");
                    try {
                        MPDParser parser = new MPDParser();
                        MPD mpd = parser.parse(streamUrl.openStream());

                        List<Period> periodList = mpd.getPeriods();
                        if (periodList.size() > 1) {
                            logger.debug("We only support simple period mpd at this time");
                            throw new Exception("NIY");
                        }

                        Period period = periodList.get(0);
                        List<AdaptationSet> adaptationSetList = period.getAdaptationSets();
                        AdaptationSet adaptationSet = adaptationSetList.get(0);

                        List<Representation> representationList = adaptationSet.getRepresentations();
                        Representation representation = representationList.get(0);

                        SegmentTemplate segmentTemplate = representation.getSegmentTemplate();
                        String initializationUri = segmentTemplate.getInitialization();
                        String mediaUri = segmentTemplate.getMedia();

                        List<Segment> segmentList = segmentTemplate.getSegmentTimeline();
                        Segment segment = segmentList.get(0);
                        long r = segment.getR();

                        List<URL> urls = new ArrayList();
                        urls.add(new URI(initializationUri).toURL());
                        for (int i = 1; i < r; i++) {
                            logger.info("Segment:" + i);
                            String downloadUri = mediaUri.replace("$Number$", "" + i);
                            urls.add(new URI(downloadUri).toURL());
                        }

                        LazzyLoadingAudioStream stream = new LazzyLoadingAudioStream(urls, "", "");
                        return stream;
                    } catch (Exception ex) {
                        logger.info("bb");
                    }
                    break;
                default:
                    break;
            }
            streamUrl = new URI(url).toURL();
            URLConnection connection = streamUrl.openConnection();
            if ("unknown/unknown".equals(connection.getContentType())) {
                // Java does not parse non-standard headers used by SHOUTCast
                int port = streamUrl.getPort() > 0 ? streamUrl.getPort() : 80;
                // Manipulate User-Agent to receive a stream
                Socket socket = new Socket(streamUrl.getHost(), port);
                shoutCastSocket = socket;

                OutputStream os = socket.getOutputStream();
                String userAgent = "WinampMPEG/5.09";
                String req = "GET / HTTP/1.0\r\nuser-agent: " + userAgent
                        + "\r\nIcy-MetaData: 1\r\nConnection: keep-alive\r\n\r\n";
                os.write(req.getBytes());
                return socket.getInputStream();
            } else {
                // getInputStream() method is more error-proof than openStream(),
                // because openStream() does openConnection().getInputStream(),
                // which opens a new connection and does not reuse the old one.
                return connection.getInputStream();
            }
        } catch (MalformedURLException | URISyntaxException e) {
            logger.error("URL '{}' is not a valid url: {}", url, e.getMessage(), e);
            throw new AudioException("URL not valid");
        } catch (IOException e) {
            logger.error("Cannot set up stream '{}': {}", url, e.getMessage(), e);
            throw new AudioException("IO Error");
        }
    }

    @Override
    public AudioFormat getFormat() {
        return audioFormat;
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }

    public String getURL() {
        return url;
    }

    public boolean hasDirectURL() {
        return !url.isBlank();
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (shoutCastSocket instanceof Socket socket) {
            socket.close();
        }
    }

    @Override
    public String toString() {
        return url;
    }

    @Override
    public InputStream getClonedStream() throws AudioException {
        if (!hasDirectURL()) {
            return new URLAudioStream(inputStream, "", "");
        } else {
            return new URLAudioStream(url);
        }
    }
}
