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

import java.io.InputStream;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This is an {@link AudioStream}, which can provide information about its absolute length and is able to provide
 * cloned streams.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public abstract class FixedLengthAudioStream extends AudioStream {

    /**
     * Provides the length of the stream in bytes.
     *
     * @return absolute length in bytes
     */
    public abstract long length();

    /**
     * Returns a new, fully independent stream instance, which can be read and closed without impacting the original
     * instance.
     *
     * @return a new input stream that can be consumed by the caller
     * @throws AudioException if stream cannot be created
     */
    public abstract InputStream getClonedStream() throws AudioException;
}
