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
package org.openhab.core.video;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.media.MediaSink;

/**
 * Definition of an audio output like headphones, a speaker or for writing to
 * a file / clip.
 *
 * @author Harald Kuhn - Initial contribution
 * @author Kelly Davis - Modified to match discussion in #584
 * @author Christoph Weitkamp - Added getSupportedStreams() and UnsupportedAudioStreamException
 */
@NonNullByDefault
public interface VideoSink extends MediaSink {

    /**
     * Returns a simple string that uniquely identifies this service
     *
     * @return an id that identifies this service
     */
    @Override
    String getId();

    /**
     * Returns a localized human readable label that can be used within UIs.
     *
     * @param locale the locale to provide the label for
     * @return a localized string to be used in UIs
     */
    @Override
    @Nullable
    String getLabel(@Nullable Locale locale);

}
