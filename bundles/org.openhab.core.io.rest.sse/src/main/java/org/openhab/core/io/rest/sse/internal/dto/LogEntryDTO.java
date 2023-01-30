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
package org.openhab.core.io.rest.sse.internal.dto;

import java.util.Date;

import org.ops4j.pax.logging.spi.PaxLoggingEvent;

/**
 * A DTO class holding info from a Pax logging event.
 *
 * @author Yannick Schaus - Initial contribution
 */
public class LogEntryDTO {
    public Date timestamp;
    public String loggerName;
    public String level;
    public String message;

    public LogEntryDTO(PaxLoggingEvent loggingEvent) {
        this.timestamp = new Date(loggingEvent.getTimeStamp());
        this.loggerName = loggingEvent.getLoggerName();
        this.level = loggingEvent.getLevel().toString();
        this.message = loggingEvent.getRenderedMessage();
    }
}
