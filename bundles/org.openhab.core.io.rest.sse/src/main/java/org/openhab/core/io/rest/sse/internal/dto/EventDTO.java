/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import org.osgi.dto.DTO;

/**
 * Event bean for broadcasted events.
 *
 * @author Ivan Iliev - Initial contribution
 * @author Dennis Nobel - Added event type and renamed object to payload
 * @author Markus Rathgeb - Follow the Data Transfer Objects Specification
 */
public class EventDTO extends DTO {

    public String topic;

    public String payload;

    public String type;
}
