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
package org.openhab.core.io.websocket.log;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.osgi.service.log.LogLevel;

/**
 * The {@link LogDTO} is used for serialization and deserialization of log messages
 *
 * @author Jan N. Klug - Initial contribution
 * @author Chris Jackson - Add sequence and make Comparable based on sequence
 */
@NonNullByDefault
public class LogDTO implements Comparable<LogDTO> {
    public String loggerName;
    public LogLevel level;
    public Date timestamp;
    public long unixtime;
    public String message;
    public String stackTrace;
    public long sequence;

    public LogDTO(long sequence, String loggerName, LogLevel level, long unixtime, String message, String stackTrace) {
        this.sequence = sequence;
        this.loggerName = loggerName;
        this.level = level;
        this.timestamp = new Date(unixtime);
        this.unixtime = unixtime;
        this.message = message;
        this.stackTrace = stackTrace;
    }

    @Override
    public int compareTo(LogDTO o) {
        return (int) (sequence - o.sequence);
    }
}
