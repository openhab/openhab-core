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

import java.util.List;

import org.openhab.core.items.dto.ItemDTO;
import org.openhab.core.thing.dto.ThingDTO;
import org.openhab.core.thing.link.dto.ItemChannelLinkDTO;

/**
 * This is a data transfer object that is used to serialize different components that can be embedded
 * inside a file format, like items, metadata, channel links, things, ...
 *
 * @author Laurent Garnier - Initial contribution
 */
public class FileFormatDTO {

    public List<ItemDTO> items;
    public List<MetadataDTO> metadata;
    public List<ItemChannelLinkDTO> channelLinks;
    public List<ThingDTO> things;
}
