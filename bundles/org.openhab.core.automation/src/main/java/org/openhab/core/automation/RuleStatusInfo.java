/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.automation;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This class is used to present status of a rule. The status consists of three parts:
 * The main status, a status detail and a string description.
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Kai Kreuzer - Refactored to match ThingStatusInfo implementation
 */
@NonNullByDefault
public class RuleStatusInfo {

    private @NonNullByDefault({}) RuleStatus status;
    private @NonNullByDefault({}) RuleStatusDetail statusDetail;
    private @Nullable String description;

    /**
     * Default constructor for deserialization e.g. by Gson.
     */
    protected RuleStatusInfo() {
    }

    /**
     * Constructs a status info.
     *
     * @param status the status
     */
    public RuleStatusInfo(RuleStatus status) {
        this(status, RuleStatusDetail.NONE);
    }

    /**
     * Constructs a status info.
     *
     * @param status the status
     * @param statusDetail the detail of the status
     */
    public RuleStatusInfo(RuleStatus status, RuleStatusDetail statusDetail) {
        this(status, statusDetail, null);
    }

    /**
     * Constructs a status info.
     *
     * @param status the status
     * @param statusDetail the detail of the status
     * @param description the description of the status
     */
    public RuleStatusInfo(RuleStatus status, RuleStatusDetail statusDetail, @Nullable String description) {
        this.status = status;
        this.statusDetail = statusDetail;
        this.description = description;
    }

    /**
     * Gets the status itself.
     *
     * @return the status
     */
    public RuleStatus getStatus() {
        return status;
    }

    /**
     * Gets the detail of the status.
     *
     * @return the status detail
     */
    public RuleStatusDetail getStatusDetail() {
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
        boolean hasDescription = getDescription() != null && !getDescription().isEmpty();
        return getStatus() + (getStatusDetail() == RuleStatusDetail.NONE ? "" : " (" + getStatusDetail() + ")")
                + (hasDescription ? ": " + getDescription() : "");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        result = prime * result + ((statusDetail == null) ? 0 : statusDetail.hashCode());
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
        RuleStatusInfo other = (RuleStatusInfo) obj;
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
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
