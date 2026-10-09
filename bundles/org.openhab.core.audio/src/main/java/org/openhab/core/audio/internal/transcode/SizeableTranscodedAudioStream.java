/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.audio.internal.transcode;

import java.io.InputStream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.SizeableAudioStream;

/**
 * A {@link TranscodedAudioStream} with a known length in bytes.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class SizeableTranscodedAudioStream extends TranscodedAudioStream implements SizeableAudioStream {
    private final long length;

    public SizeableTranscodedAudioStream(AudioFormat format, @Nullable String id, InputStream input, long length) {
        super(format, id, input);
        this.length = length;
    }

    @Override
    public long length() {
        return length;
    }
}
