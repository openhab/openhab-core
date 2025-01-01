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
package org.openhab.core.io.websocket.log;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.websocket.event.EventDTO;
import org.osgi.service.log.LogLevel;

/**
 * The {@link EventDTO} is used for serialization and deserialization of events
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class LogDTO {
    public @Nullable String loggerName;
    public @Nullable LogLevel level;
    public @Nullable Date timestamp;
    public long unixtime;
    public @Nullable String message;
}
