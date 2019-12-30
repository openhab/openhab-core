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
package org.openhab.core.config.discovery.inbox;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.discovery.DiscoveryResult;

/**
 * The {@link InboxListener} interface for receiving {@link Inbox} events.
 * <p>
 * A class that is interested in processing {@link Inbox} events fired synchronously by the {@link Inbox} service has to
 * implement this interface.
 *
 * @author Michael Grammling - Initial contribution
 *
 * @see Inbox
 */
@NonNullByDefault
public interface InboxListener {

    /**
     * Invoked synchronously when a <i>NEW</i> {@link DiscoveryResult} has been added
     * to the {@link Inbox}.
     *
     * @param source the inbox which is the source of this event (not null)
     * @param result the discovery result which has been added to the inbox (not null)
     */
    void thingAdded(Inbox source, DiscoveryResult result);

    /**
     * Invoked synchronously when an <i>EXISTING</i> {@link DiscoveryResult} has been
     * updated in the {@link Inbox}.
     *
     * @param source the inbox which is the source of this event (not null)
     * @param result the discovery result which has been updated in the inbox (not null)
     */
    void thingUpdated(Inbox source, DiscoveryResult result);

    /**
     * Invoked synchronously when an <i>EXISTING</i> {@link DiscoveryResult} has been
     * removed from the {@link Inbox}.
     *
     * @param source the inbox which is the source of this event (not null)
     * @param result the discovery result which has been removed from the inbox (not null)
     */
    void thingRemoved(Inbox source, DiscoveryResult result);

}
