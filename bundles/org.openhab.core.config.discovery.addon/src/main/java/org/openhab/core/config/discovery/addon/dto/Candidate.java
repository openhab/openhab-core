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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for serialization of a single addon suggestion discovery candidate.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class Candidate {
    private @Nullable String addonUid;
    private @Nullable List<DiscoveryMethod> discoveryMethods;

    public List<DiscoveryMethod> getDiscoveryMethods() {
        List<DiscoveryMethod> discoveryMethods = this.discoveryMethods;
        return discoveryMethods != null ? discoveryMethods : List.of();
    }

    public String getAddonUid() {
        String addonUid = this.addonUid;
        return addonUid != null ? addonUid : "";
    }

    public Candidate setDiscoveryMethods(List<DiscoveryMethod> discoveryMethods) {
        this.discoveryMethods = discoveryMethods;
        return this;
    }

    public Candidate setAddonUid(String addonUid) {
        this.addonUid = addonUid;
        return this;
    }
}
