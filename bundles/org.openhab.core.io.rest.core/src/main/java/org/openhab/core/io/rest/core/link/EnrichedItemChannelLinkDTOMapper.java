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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.link.ItemChannelLink;

/**
 * The {@link EnrichedItemChannelLinkDTOMapper} is a utility class to map item channel links into enriched item channel
 * link data transform objects (DTOs).
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class EnrichedItemChannelLinkDTOMapper {

    /**
     * Maps an item channel link into an enriched item channel link DTO object.
     *
     * @param link the item channel link
     * @return item channel link DTO object
     */
    public static EnrichedItemChannelLinkDTO map(ItemChannelLink link, boolean editable) {
        return new EnrichedItemChannelLinkDTO(link.getItemName(), link.getLinkedUID().toString(),
                link.getConfiguration().getProperties(), editable);
    }
}
