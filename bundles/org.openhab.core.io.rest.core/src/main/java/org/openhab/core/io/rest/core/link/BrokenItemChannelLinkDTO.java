/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
import org.openhab.core.thing.link.ItemChannelLinkRegistry.ItemChannelLinkProblem;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Transfer object for broken item channel links.
 *
 * @author Arne Seime - Initial contribution
 */
@Schema(name = "BrokenItemChannelLink")
@NonNullByDefault
public class BrokenItemChannelLinkDTO {
    public EnrichedItemChannelLinkDTO itemChannelLink;

    public ItemChannelLinkProblem problem;

    public BrokenItemChannelLinkDTO(EnrichedItemChannelLinkDTO itemChannelLink, ItemChannelLinkProblem problem) {
        this.itemChannelLink = itemChannelLink;
        this.problem = problem;
    }
}
