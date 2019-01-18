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
package org.eclipse.smarthome.core.audio;

import org.eclipse.smarthome.core.audio.internal.AudioServlet;

/**
 * This is an interface that is implemented by {@link AudioServlet} and which allows exposing audio streams through
 * HTTP.
 * Streams are only served a single time and then discarded.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
public interface AudioHTTPServer {

    /**
     * Creates a relative url for a given {@link AudioStream} where it can be requested a single time.
     * Note that the HTTP header only contains "Content-length", if the passed stream is an instance of
     * {@link FixedLengthAudioStream}.
     * If the client that requests the url expects this header field to be present, make sure to pass such an instance.
     * Streams are closed after having been served.
     *
     * @param stream the stream to serve on HTTP
     * @return the relative URL to access the stream starting with a '/'
     */
    String serve(AudioStream stream);

    /**
     * Creates a relative url for a given {@link AudioStream} where it can be requested multiple times within the given
     * time frame.
     * This method only accepts {@link FixedLengthAudioStream}s, since it needs to be able to create multiple concurrent
     * streams from it, which isn't possible with a regular {@link AudioStream}.
     * Streams are closed, once they expire.
     *
     * @param stream the stream to serve on HTTP
     * @param seconds number of seconds for which the stream is available through HTTP
     * @return the relative URL to access the stream starting with a '/'
     */
    String serve(FixedLengthAudioStream stream, int seconds);

}
