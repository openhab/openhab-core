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
package org.openhab.core.io.net.http;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This exception is thrown, if an unexpected error occurs during initialization of the Jetty client
 *
 * @author Michael Bock - Initial contribution
 */
@NonNullByDefault
public class HttpClientInitializationException extends RuntimeException {

    private static final long serialVersionUID = -3187938868560212413L;

    public HttpClientInitializationException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
