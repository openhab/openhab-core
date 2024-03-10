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
package org.openhab.core.thing.link;

/**
 * Transfer object for broken item channel links.
 *
 * @author Arne Seime - Initial contribution
 */
public class BrokenItemChannelLinkDTO {
    public EnrichedItemChannelLinkDTO itemChannelLink;

    public ItemChannelLinkProblem problem;

    public BrokenItemChannelLinkDTO(EnrichedItemChannelLinkDTO itemChannelLink, ItemChannelLinkProblem problem) {
        this.itemChannelLink = itemChannelLink;
        this.problem = problem;
    }

    public enum ItemChannelLinkProblem {
        THING_CHANNEL_MISSING,
        ITEM_MISSING,
        ITEM_AND_THING_CHANNEL_MISSING;
    }
}
