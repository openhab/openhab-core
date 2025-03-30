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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This is an interface that is implemented by {@link org.openhab.core.audio.internal.AudioServlet} and which allows
 * exposing audio streams through HTTP.
 * Streams are only served a single time and then discarded.
 *
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
public interface MediaServiceFactory {

}
