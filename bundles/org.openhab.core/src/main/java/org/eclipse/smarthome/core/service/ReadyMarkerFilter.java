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
package org.eclipse.smarthome.core.service;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Filter for {@link ReadyMarker}s which a ReadyTracker is interested in.
 *
 * By default, this filter will match any {@link ReadyMarker}.
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
@NonNullByDefault
public final class ReadyMarkerFilter {

    private final @Nullable String identifier;
    private final @Nullable String type;

    public ReadyMarkerFilter() {
        this(null, null);
    }

    private ReadyMarkerFilter(@Nullable String type, @Nullable String identifier) {
        this.type = type;
        this.identifier = identifier;
    }

    public boolean apply(ReadyMarker readyMarker) {
        return isTracked(type, readyMarker.getType()) && isTracked(identifier, readyMarker.getIdentifier());
    }

    private boolean isTracked(@Nullable String trackingSpec, String realValue) {
        return trackingSpec == null || trackingSpec.equals(realValue);
    }

    /**
     * Returns a {@link ReadyMarkerFilter} restricted to the given type.
     *
     * @param type
     * @return
     */
    public ReadyMarkerFilter withType(@Nullable String type) {
        return new ReadyMarkerFilter(type, identifier);
    }

    /**
     * Returns a {@link ReadyMarkerFilter} restricted to the given identifier.
     *
     * @param type
     * @return
     */
    public ReadyMarkerFilter withIdentifier(@Nullable String identifier) {
        return new ReadyMarkerFilter(type, identifier);
    }

}
