/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.thing.link.events;

import org.eclipse.smarthome.core.events.AbstractEvent;
import org.eclipse.smarthome.core.thing.link.dto.ItemChannelLinkDTO;

/**
 * {@link AbstractItemChannelLinkRegistryEvent} is an abstract class for item channel link events.
 *
 * @author Dennis Nobel - Initial contribution
 *
 */
public abstract class AbstractItemChannelLinkRegistryEvent extends AbstractEvent {

    private final ItemChannelLinkDTO link;

    public AbstractItemChannelLinkRegistryEvent(String topic, String payload, ItemChannelLinkDTO link) {
        super(topic, payload, null);
        this.link = link;
    }

    public ItemChannelLinkDTO getLink() {
        return link;
    }

}
