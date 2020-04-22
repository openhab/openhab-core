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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.utils.AudioStreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an AudioStream from an URL. Note that some sinks, like Sonos, can directly handle URL
 * based streams, and therefore can/should call getURL() to get an direct reference to the URL.
 *
 * @author Karel Goderis - Initial contribution
 * @author Kai Kreuzer - Refactored to not require a source
 * @author Christoph Weitkamp - Refactored use of filename extension
 */
@NonNullByDefault
public class URLAudioStream extends AudioStream {

    private static final Pattern PLS_STREAM_PATTERN = Pattern.compile("^File[0-9]=(.+)$");

    public static final String M3U_EXTENSION = "m3u";
    public static final String PLS_EXTENSION = "pls";

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

    private InputStream createInputStream() throws AudioException {
        final String filename = url.toLowerCase();
        final String extension = AudioStreamUtils.getExtension(filename);
        try {
            switch (extension) {
                case M3U_EXTENSION:
                    for (final String line : Files.readAllLines(Paths.get(URI.create(url)))) {
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            url = line;
                            break;
                        }
                    }
                    break;
                case PLS_EXTENSION:
                    for (final String line : Files.readAllLines(Paths.get(URI.create(url)))) {
                        if (!line.isEmpty() && line.startsWith("File")) {
                            final Matcher matcher = PLS_STREAM_PATTERN.matcher(line);
                            if (matcher.find()) {
                                url = matcher.group(1);
                                break;
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
            URL streamUrl = new URL(url);
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
        } catch (MalformedURLException e) {
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

    @Override
    public void close() throws IOException {
        super.close();
        if (shoutCastSocket != null) {
            shoutCastSocket.close();
        }
    }

    @Override
    public String toString() {
        return url;
    }
}
