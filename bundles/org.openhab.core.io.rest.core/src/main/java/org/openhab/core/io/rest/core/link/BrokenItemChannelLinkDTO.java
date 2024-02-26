/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

import org.openhab.core.thing.link.ItemChannelLink;

/**
 * Transfer object for broken item channel links.
 *
 * @author Arne Seime - Initial contribution
 */
public class BrokenItemChannelLinkDTO {
    public ItemChannelLink itemChannelLink;

    public ItemChannelLinkProblem problem;

    public boolean isEditable;

    public BrokenItemChannelLinkDTO(ItemChannelLink itemChannelLink, ItemChannelLinkProblem problem,
            boolean isEditable) {
        this.itemChannelLink = itemChannelLink;
        this.problem = problem;
        this.isEditable = isEditable;
    }

    public enum ItemChannelLinkProblem {
        THING_CHANNEL_MISSING,
        ITEM_MISSING,
        ITEM_AND_THING_CHANNEL_MISSING;
    }
}
