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

/**
 * By implementing this interface, a {@link DiscoveryService} implementation may indicate that it requires extended
 * access to the core framework.
 *
 * The {@link DiscoveryService} will get a {@link DiscoveryServiceCallback}, which provides the extended framework
 * capabilities.
 *
 * @deprecated The implementation of {@link DiscoveryServiceCallback} caused a cyclic dependency and will be removed in
 *             future versions. Please see the deprecation documentation on {@link DiscoveryServiceCallback} for more
 *             details.
 *
 * @author Simon Kaufmann - initial contribution and API.
 * @author Henning Treu - deprecation.
 *
 */
@Deprecated
@NonNullByDefault
public interface ExtendedDiscoveryService {

    /**
     * Provides the callback, which a {@link DiscoveryService} may use in order to access core features.
     *
     * @param discoveryServiceCallback
     */
    public void setDiscoveryServiceCallback(DiscoveryServiceCallback discoveryServiceCallback);

}
