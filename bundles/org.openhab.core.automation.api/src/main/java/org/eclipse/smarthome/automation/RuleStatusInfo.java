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
package org.eclipse.smarthome.automation;

/**
 * This class is used to present status of a rule. The status consists of three parts:
 * The main status, a status detail and a string description.
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Kai Kreuzer - Refactored to match ThingStatusInfo implementation
 */
public class RuleStatusInfo {

    private RuleStatus status;
    private RuleStatusDetail statusDetail;
    private String description;

    /**
     * Default constructor for deserialization e.g. by Gson.
     */
    protected RuleStatusInfo() {
    }

    /**
     * Constructs a status info.
     *
     * @param status the status (must not be null)
     * @throws IllegalArgumentException if status is null
     */
    public RuleStatusInfo(RuleStatus status) throws IllegalArgumentException {
        this(status, RuleStatusDetail.NONE);
    }

    /**
     * Constructs a status info.
     *
     * @param status       the status (must not be null)
     * @param statusDetail the detail of the status (must not be null)
     * @throws IllegalArgumentException if status or status detail is null
     */
    public RuleStatusInfo(RuleStatus status, RuleStatusDetail statusDetail) throws IllegalArgumentException {
        this(status, statusDetail, null);
    }

    /**
     * Constructs a status info.
     *
     * @param status       the status (must not be null)
     * @param statusDetail the detail of the status (must not be null)
     * @param description  the description of the status
     * @throws IllegalArgumentException if status or status detail is null
     */
    public RuleStatusInfo(RuleStatus status, RuleStatusDetail statusDetail, String description)
            throws IllegalArgumentException {
        if (status == null) {
            throw new IllegalArgumentException("Thing status must not be null");
        }
        if (statusDetail == null) {
            throw new IllegalArgumentException("Thing status detail must not be null");
        }
        this.status = status;
        this.statusDetail = statusDetail;
        this.description = description;
    }

    /**
     * Gets the status itself.
     *
     * @return the status (not null)
     */
    public RuleStatus getStatus() {
        return status;
    }

    /**
     * Gets the detail of the status.
     *
     * @return the status detail (not null)
     */
    public RuleStatusDetail getStatusDetail() {
        return statusDetail;
    }

    /**
     * Gets the description of the status.
     *
     * @return the description
     */
    public String getDescription() {
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
    public boolean equals(Object obj) {
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
