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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This is an AudioStream from a URL. Note that some sinks, like Sonos, can directly handle URL
 * based streams, and therefore can/should call getURL() to get a direct reference to the URL.
 *
 * @author Karel Goderis - Initial contribution
 * @author Kai Kreuzer - Refactored to not require a source
 * @author Christoph Weitkamp - Refactored use of filename extension
 */
public class LazzyLoadingAudioStream extends InputStream {
    private final Iterator<URL> urls;
    private InputStream current;
    private boolean first = true;
    private String artist;
    private String title;

    public LazzyLoadingAudioStream(List<URL> urls, String artist, String title) {
        this.urls = urls.iterator();
        this.artist = artist;
        this.title = title;
    }

    private InputStream downloadAndInject(URL url) throws IOException {
        // 1) Télécharge dans un buffer
        byte[] rawInit = url.openStream().readAllBytes();

        // 2) Modifie le MP4 (init segment)
        ByteArrayInputStream in = new ByteArrayInputStream(rawInit);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Appel à ton injecteur :
        Map<String, String> tags = new HashMap<>();
        tags.put("title", title);
        tags.put("artist", artist);
        tags.put("album", "Greatest Hits");
        tags.put("genre", "Rock");
        tags.put("year", "2023");
        tags.put("track", "1");
        tags.put("trackTotal", "12");
        tags.put("albumArtist", "John Doe");

        byte[] cover = Files.readAllBytes(
                Paths.get("C:/eclipse/openhab-main/git/openhab-distro/launch/app/runtime/conf/html/Deezer.png"));

        Mp4UdtaInjector.inject(in, out, tags, cover);

        byte[] modified = out.toByteArray();

        // 3) Retourne un InputStream lisant le MP4 modifié
        return new ByteArrayInputStream(modified);
    }

    private InputStream startDownload(URL url) throws IOException {
        if (first) {
            try {
                first = false;
                return downloadAndInject(url);
            } catch (Exception ex) {
                first = false;
            }
        }

        PipedInputStream in = new PipedInputStream(64 * 1024);
        PipedOutputStream out = new PipedOutputStream(in);

        new Thread(() -> {
            try (InputStream src = url.openStream()) {
                System.out.println("Download URL:" + url.toString());

                src.transferTo(out);
            } catch (IOException ignored) {
            } finally {
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }
        }, "preload-" + url).start();

        return in;
    }

    private void ensureCurrent() throws IOException {
        if (current == null && urls.hasNext()) {
            current = startDownload(urls.next());
        }
    }

    @Override
    public int read() throws IOException {
        ensureCurrent();
        if (current == null) {
            return -1;
        }

        int b = current.read();
        if (b == -1) {
            current.close();
            current = null;
            return read();
        }
        return b;
    }
}
