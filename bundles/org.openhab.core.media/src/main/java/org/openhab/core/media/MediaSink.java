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
package org.openhab.core.media;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This is an interface that is
 *
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
public interface MediaSink {
    String getId();

    String getName();

    @Nullable
    String getLabel(@Nullable Locale locale);

    String getType();

    String getBinding();

}
