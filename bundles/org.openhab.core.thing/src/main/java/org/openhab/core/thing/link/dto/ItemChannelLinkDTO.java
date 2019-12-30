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
package org.openhab.core.thing.link.dto;

import java.util.Map;

/**
 * This is a data transfer object that is used to serialize links.
 *
 * @author Dennis Nobel - Initial contribution
 */
public class ItemChannelLinkDTO extends AbstractLinkDTO {

    public String channelUID;
    public Map<String, Object> configuration;

    /**
     * Default constructor for deserialization e.g. by Gson.
     */
    protected ItemChannelLinkDTO() {
    }

    public ItemChannelLinkDTO(String itemName, String channelUID, Map<String, Object> configuration) {
        super(itemName);
        this.channelUID = channelUID;
        this.configuration = configuration;
    }

}
