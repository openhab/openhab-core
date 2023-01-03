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
package org.openhab.core.i18n;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Provides an exception class for openHAB to be used in case of connection issues with a device
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class ConnectionException extends CommunicationException {

    private static final long serialVersionUID = 1L;

    public ConnectionException(String message, @Nullable Object @Nullable... msgParams) {
        super(message, msgParams);
    }

    public ConnectionException(String message, @Nullable Throwable cause, @Nullable Object @Nullable... msgParams) {
        super(message, cause, msgParams);
    }

    public ConnectionException(Throwable cause) {
        super(cause);
    }
}
