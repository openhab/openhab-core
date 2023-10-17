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
package org.openhab.core.addon;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for serialization of a suggested addon discovery method.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class AddonDiscoveryMethod {
    private @Nullable String serviceType;
    private @Nullable String mdnsServiceType;
    private @Nullable List<AddonMatchProperty> matchProperties;

    public AddonDiscoveryServiceType getServiceType() {
        String serviceType = this.serviceType;
        return AddonDiscoveryServiceType.valueOf(serviceType != null ? serviceType.toUpperCase() : "");
    }

    public String getMdnsServiceType() {
        String mdnsServiceType = this.mdnsServiceType;
        return mdnsServiceType != null ? mdnsServiceType : "";
    }

    public Map<String, String> getPropertyRegexMap() {
        List<AddonMatchProperty> matchProperties = this.matchProperties;
        return matchProperties != null
                ? matchProperties.stream().collect(Collectors.toMap(m -> m.getName(), m -> m.getRegex()))
                : Map.of();
    }

    public AddonDiscoveryMethod setServiceType(AddonDiscoveryServiceType serviceType) {
        this.serviceType = serviceType.name().toLowerCase();
        return this;
    }

    public AddonDiscoveryMethod setMdnsServiceType(String mdnsServiceType) {
        this.mdnsServiceType = mdnsServiceType;
        return this;
    }

    public AddonDiscoveryMethod setMatchProperties(Map<String, String> matchProperties) {
        this.matchProperties = matchProperties.entrySet().stream()
                .map(e -> new AddonMatchProperty(e.getKey(), e.getValue())).toList();
        return this;
    }
}
