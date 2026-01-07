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

/**
 * An {@link AudioSink} fake used for the tests.
 *
 * @author Petar Valchev - Initial contribution
 * @author Christoph Weitkamp - Added examples for getSupportedFormats() and getSupportedStreams()
 * @author Christoph Weitkamp - Added parameter to adjust the volume
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
@NonNullByDefault
public class VideoSinkFake implements VideoSink {

    @Override
    public String getId() {
        return "testSinkId";
    }

    @Override
    public String getName() {
        return "testSinkId";
    }

    @Override
    public String getBinding() {
        return "core.video";
    }

    @Override
    public String getType() {
        return "";
    }

    @Override
    public @Nullable String getLabel(@Nullable Locale locale) {
        return "testSinkLabel";
    }
}
