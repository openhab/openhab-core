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

import java.util.List;

/**
 * The {@link LogFilterDTO} is used for serialization and deserialization of log filters
 *
 * @author Chris Jackson - Initial contribution
 */
public class LogFilterDTO {

    public Long timeStart;
    public Long timeStop;
    public List<String> loggerNames;
    public Long sequenceStart;
}
