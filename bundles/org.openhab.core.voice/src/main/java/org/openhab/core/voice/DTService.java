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
package org.openhab.core.voice;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This is the interface that a dialog trigger service has to implement.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public interface DTService {
    /**
     * Returns a simple string that uniquely identifies this service
     *
     * @return an id that identifies this service
     */
    String getId();

    /**
     * Returns a localized human-readable label that can be used within UIs.
     *
     * @param locale the locale to provide the label for
     * @return a localized string to be used in UIs
     */
    String getLabel(@Nullable Locale locale);
}
