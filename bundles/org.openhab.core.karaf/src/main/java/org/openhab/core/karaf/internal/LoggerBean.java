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
package org.openhab.core.karaf.internal;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This is a java bean that is used to define logger settings for the REST interface.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class LoggerBean {

    public final List<LoggerInfo> loggers;

    public static class LoggerInfo {
        public final String loggerName;
        public final String level;

        public LoggerInfo(String loggerName, String level) {
            this.loggerName = loggerName;
            this.level = level;
        }
    }

    public LoggerBean(Map<String, String> logLevels) {
        loggers = logLevels.entrySet().stream().map(l -> new LoggerInfo(l.getKey(), l.getValue()))
                .collect(Collectors.toList());
    }
}
