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
package org.openhab.core.config.discovery.addon.candidate;

import java.util.Collections;
import java.util.Map;

import javax.jmdns.ServiceInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This is a {@link MdnsCandidate} mdns version of class for candidates for
 * suggested addons.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class MdnsCandidate extends BaseCandidate {
    private final String mdnsServiceType;

    public MdnsCandidate(String bindingId, Map<String, String> propertyMatchRegex, String serviceType) {
        super(bindingId, propertyMatchRegex);
        this.mdnsServiceType = serviceType;
    }

    /**
     * Check if the data in the provided ServiceInfo matches our own match criteria.
     */
    @Override
    public boolean equals(@Nullable Object object) {
        if (object instanceof ServiceInfo serviceInfo) {
            return propertyMatches("application", serviceInfo.getApplication())
                    && propertyMatches("name", serviceInfo.getName())
                    && Collections.list(serviceInfo.getPropertyNames()).stream().allMatch(
                            propertyName -> propertyMatches(propertyName, serviceInfo.getPropertyString(propertyName)));
        }
        return false;
    }

    public String getMdnsServiceType() {
        return mdnsServiceType;
    }
}
