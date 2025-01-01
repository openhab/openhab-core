/**
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

import java.io.InputStream;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This is for an {@link AudioStream}, that can be cloned
 *
 * @author Gwendal Roulleau - Initial contribution, separation from {@link FixedLengthAudioStream}
 */
@NonNullByDefault
public interface ClonableAudioStream {

    /**
     * Returns a new, fully independent stream instance, which can be read and closed without impacting the original
     * instance.
     *
     * @return a new input stream that can be consumed by the caller
     * @throws AudioException if stream cannot be created
     */
    InputStream getClonedStream() throws AudioException;
}
