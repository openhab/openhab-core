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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an AudioStream from a URL. Note that some sinks, like Sonos, can directly handle URL
 * based streams, and therefore can/should call getURL() to get a direct reference to the URL.
 *
 * @author Karel Goderis - Initial contribution
 * @author Kai Kreuzer - Refactored to not require a source
 * @author Christoph Weitkamp - Refactored use of filename extension
 */
@NonNullByDefault
public class LazzyLoadingAudioStream extends InputStream {
    private final Logger logger = LoggerFactory.getLogger(LazzyLoadingAudioStream.class);

    private final Iterator<URL> urls;
    @Nullable
    private InputStream current;

    public LazzyLoadingAudioStream(List<URL> urls) {
        this.urls = urls.iterator();
    }

    private InputStream startDownload(URL url) throws IOException {
        PipedInputStream in = new PipedInputStream(64 * 1024);
        PipedOutputStream out = new PipedOutputStream(in);

        new Thread(() -> {
            try (InputStream src = url.openStream()) {
                logger.info("Download URL:" + url.toString());

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
