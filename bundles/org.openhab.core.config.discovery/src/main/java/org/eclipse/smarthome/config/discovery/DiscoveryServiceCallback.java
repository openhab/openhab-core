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
package org.eclipse.smarthome.config.discovery;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.inbox.Inbox;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingUID;

/**
 * This interface provides helper methods for {@link DiscoveryService}s in order to access core framework capabilities.
 * <p>
 * This interface must not be implemented by bindings.
 *
 * @deprecated The use of this callback is deprecated. The {@link Inbox} is able to deal with updates.
 *             Incremental discovery should be handled internally by each {@link DiscoveryService}.
 *
 * @author Simon Kaufmann - initial contribution and API.
 * @author Henning Treu - deprecation.
 */
@Deprecated
@NonNullByDefault
public interface DiscoveryServiceCallback {

    /**
     * @deprecated Will always return null.
     */
    @Deprecated
    default @Nullable Thing getExistingThing(ThingUID thingUID) {
        return null;
    }

    /**
     * @deprecated Will always return null.
     */
    @Deprecated
    default @Nullable DiscoveryResult getExistingDiscoveryResult(ThingUID thingUID) {
        return null;
    }

}
