/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.io.rest.core.link;

import java.util.Map;

import org.openhab.core.thing.link.dto.ItemChannelLinkDTO;

/**
 * This is an enriched data transfer object that is used to serialize items channel links with dynamic data like the
 * editable flag.
 *
 * @author Christoph Weitkamp- Initial contribution
 */
public class EnrichedItemChannelLinkDTO extends ItemChannelLinkDTO {

    public Boolean editable;

    public EnrichedItemChannelLinkDTO(String itemName, String channelUID, Map<String, Object> configuration,
            boolean editable) {
        super(itemName, channelUID, configuration);
        this.editable = editable;
    }
}
