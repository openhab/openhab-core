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
package org.openhab.core.thing;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A {@link ThingStatusInfo} represents status information of a thing which consists of
 * <ul>
 * <li>the status itself </il>
 * <li>detail of the status</il>
 * <li>and a description of the status</il>
 * </ul>
 *
 * @author Stefan Bußweiler - Initial contribution
 * @author Dennis Nobel - Added null checks
 */
@NonNullByDefault
public class ThingStatusInfo {

    private final ThingStatus status;

    private final ThingStatusDetail statusDetail;

    private @Nullable String description;

    /**
     * Default constructor for deserialization e.g. by Gson.
     */
    protected ThingStatusInfo() {
        status = ThingStatus.UNKNOWN;
        statusDetail = ThingStatusDetail.NONE;
    }

    /**
     * Constructs a status info.
     *
     * @param status the status (must not be null)
     * @param statusDetail the detail of the status (must not be null)
     * @param description the description of the status
     */
    public ThingStatusInfo(ThingStatus status, ThingStatusDetail statusDetail, @Nullable String description) {
        this.status = status;
        this.statusDetail = statusDetail;
        this.description = description;
    }

    /**
     * Gets the status itself.
     *
     * @return the status (not null)
     */
    public ThingStatus getStatus() {
        return status;
    }

    /**
     * Gets the detail of the status.
     *
     * @return the status detail (not null)
     */
    public ThingStatusDetail getStatusDetail() {
        return statusDetail;
    }

    /**
     * Gets the description of the status.
     *
     * @return the description
     */
    public @Nullable String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        String description = getDescription();
        return getStatus() + (getStatusDetail() == ThingStatusDetail.NONE ? "" : " (" + getStatusDetail() + ")")
                + (description == null || description.isBlank() ? "" : ": " + description);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        String description = this.description; // prevent NPE in case the class variable is changed between the two
                                               // calls in the next line
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + status.hashCode();
        result = prime * result + statusDetail.hashCode();
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ThingStatusInfo other = (ThingStatusInfo) obj;
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!Objects.equals(description, other.description)) {
            return false;
        }
        if (status != other.status) {
            return false;
        }
        if (statusDetail != other.statusDetail) {
            return false;
        }
        return true;
    }
}
