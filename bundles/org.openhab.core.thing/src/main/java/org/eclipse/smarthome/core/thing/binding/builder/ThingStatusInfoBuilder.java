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
package org.eclipse.smarthome.core.thing.binding.builder;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;

/**
 * {@link ThingStatusInfoBuilder} is responsible for creating {@link ThingStatusInfo}s.
 *
 * @author Stefan Bußweiler - Initial contribution
 * @author Dennis Nobel - Added null checks
 */
@NonNullByDefault
public class ThingStatusInfoBuilder {

    private final ThingStatus status;

    private ThingStatusDetail statusDetail;

    private @Nullable String description;

    private ThingStatusInfoBuilder(ThingStatus status, ThingStatusDetail statusDetail, @Nullable String description) {
        this.status = status;
        this.statusDetail = statusDetail;
        this.description = description;
    }

    /**
     * Creates a status info builder for the given status and detail.
     *
     * @param status the status (must not be null)
     * @param statusDetail the detail of the status (must not be null)
     * @return status info builder
     */
    public static ThingStatusInfoBuilder create(ThingStatus status, ThingStatusDetail statusDetail) {
        return new ThingStatusInfoBuilder(status, statusDetail, null);
    }

    /**
     * Creates a status info builder for the given status.
     *
     * @param status the status (must not be null)
     * @return status info builder
     */
    public static ThingStatusInfoBuilder create(ThingStatus status) {
        return create(status, ThingStatusDetail.NONE);
    }

    /**
     * Appends a description to the status to build.
     *
     * @param description the description
     * @return status info builder
     */
    public ThingStatusInfoBuilder withDescription(@Nullable String description) {
        this.description = description;
        return this;
    }

    /**
     * Appends a status detail to the status to build.
     *
     * @param statusDetail the status detail (must not be null)
     * @return status info builder
     */
    public ThingStatusInfoBuilder withStatusDetail(ThingStatusDetail statusDetail) {
        this.statusDetail = statusDetail;
        return this;
    }

    /**
     * Builds and returns the status info.
     *
     * @return status info
     */
    public ThingStatusInfo build() {
        return new ThingStatusInfo(status, statusDetail, description);
    }
}
