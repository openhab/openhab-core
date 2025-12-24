/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.addon;

import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for serialization of a suggested addon discovery method.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class AddonDiscoveryMethod {
    private @NonNullByDefault({}) String serviceType;
    private @Nullable List<AddonParameter> parameters;
    private @Nullable List<AddonMatchProperty> matchProperties;

    public String getServiceType() {
        return serviceType.toLowerCase();
    }

    public List<AddonParameter> getParameters() {
        List<AddonParameter> parameters = this.parameters;
        return parameters != null ? parameters : List.of();
    }

    public List<AddonMatchProperty> getMatchProperties() {
        List<AddonMatchProperty> matchProperties = this.matchProperties;
        return matchProperties != null ? matchProperties : List.of();
    }

    public AddonDiscoveryMethod setServiceType(String serviceType) {
        this.serviceType = serviceType.toLowerCase();
        return this;
    }

    public AddonDiscoveryMethod setParameters(@Nullable List<AddonParameter> parameters) {
        this.parameters = parameters;
        return this;
    }

    public AddonDiscoveryMethod setMatchProperties(@Nullable List<AddonMatchProperty> matchProperties) {
        this.matchProperties = matchProperties;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceType, parameters, matchProperties);
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
        AddonDiscoveryMethod other = (AddonDiscoveryMethod) obj;
        return Objects.equals(serviceType, other.serviceType) && Objects.equals(parameters, other.parameters)
                && Objects.equals(matchProperties, other.matchProperties);
    }

    @Override
    public String toString() {
        return "AddonDiscoveryMethod [serviceType=" + serviceType + ", parameters=" + parameters + ", matchProperties="
                + matchProperties + "]";
    }
}
