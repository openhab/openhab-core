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
package org.openhab.core.audio;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This is a {@link ClonableAudioStream}, which can also provide information about its absolute length.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Gwendal Roulleau - Separate getClonedStream into its own class
 */
@NonNullByDefault
public abstract class FixedLengthAudioStream extends ClonableAudioStream {

    /**
     * Provides the length of the stream in bytes.
     *
     * @return absolute length in bytes
     */
    public abstract long length();
}
