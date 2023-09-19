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
 * DTO for serialization of a single addon suggestion candidate.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class Candidate {
    private @NonNullByDefault({}) String addonUid;
    private @NonNullByDefault({}) String discoveryType;
    private @NonNullByDefault({}) List<PropertyRegex> properties;
    private @Nullable String mdnsServiceType;

    public String getAddonUid() {
        return addonUid;
    }

    public DiscoveryType getDiscoveryType() {
        return DiscoveryType.valueOf(discoveryType.toUpperCase());
    }

    public String getMdnsServiceType() {
        String mdnsServiceType = this.mdnsServiceType;
        return mdnsServiceType != null ? mdnsServiceType : "";
    }

    public Map<String, String> getPropertyRegexMap() {
        return properties.stream().collect(Collectors.toMap(x -> x.getName(), x -> x.getRegex()));
    }

    public Candidate setAddonUid(String addonUid) {
        this.addonUid = addonUid;
        return this;
    }

    public Candidate setDiscoveryType(DiscoveryType discoveryType) {
        this.discoveryType = discoveryType.name().toLowerCase();
        return this;
    }

    public Candidate setMdnsServiceType(String mdnsServiceType) {
        this.mdnsServiceType = mdnsServiceType;
        return this;
    }

    public Candidate setPropertyRegexMap(Map<String, String> propertyRegexMap) {
        properties = propertyRegexMap.entrySet().stream().map(e -> new PropertyRegex(e.getKey(), e.getValue()))
                .toList();
        return this;
    }
}
