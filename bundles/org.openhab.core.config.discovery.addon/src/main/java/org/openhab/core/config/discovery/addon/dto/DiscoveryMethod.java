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
package org.openhab.core.config.discovery.addon.dto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for serialization of a single addon suggestion candidate discovery method.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class DiscoveryMethod {
    private @Nullable String serviceType;
    private @Nullable List<MatchProperty> matchProperties;
    private @Nullable String mdnsServiceType;

    public ServiceType getServiceType() {
        String serviceType = this.serviceType;
        return ServiceType.valueOf(serviceType != null ? serviceType.toUpperCase() : "");
    }

    public String getMdnsServiceType() {
        String mdnsServiceType = this.mdnsServiceType;
        return mdnsServiceType != null ? mdnsServiceType : "";
    }

    public Map<String, String> getMatchProperties() {
        return matchProperties != null
                ? matchProperties.stream().collect(Collectors.toMap(x -> x.getName(), x -> x.getRegex()))
                : Map.of();
    }

    public DiscoveryMethod setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType.name().toLowerCase();
        return this;
    }

    public DiscoveryMethod setMdnsServiceType(String mdnsServiceType) {
        this.mdnsServiceType = mdnsServiceType;
        return this;
    }

    public DiscoveryMethod setMatchProperties(Map<String, String> matchProperties) {
        this.matchProperties = matchProperties.entrySet().stream().map(e -> new MatchProperty(e.getKey(), e.getValue()))
                .toList();
        return this;
    }
}
