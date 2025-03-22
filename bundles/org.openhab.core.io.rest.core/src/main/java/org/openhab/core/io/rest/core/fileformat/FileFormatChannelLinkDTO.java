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
package org.openhab.core.io.rest.core.fileformat;

import java.util.Map;

/**
 * This is a data transfer object to serialize a channel link for an item contained in a file format.
 *
 * @author Laurent Garnier - Initial contribution
 */
public class FileFormatChannelLinkDTO {

    public String channelUID;
    public Map<String, Object> configuration;

    public FileFormatChannelLinkDTO(String channelUID, Map<String, Object> configuration) {
        this.channelUID = channelUID;
        this.configuration = configuration;
    }
}
