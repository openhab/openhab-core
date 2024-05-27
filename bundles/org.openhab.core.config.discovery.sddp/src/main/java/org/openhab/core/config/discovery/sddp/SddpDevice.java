/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.config.discovery.sddp;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A DTO class containing data from an SDDP device discovery result.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class SddpDevice {

    public final String from;
    public final String host;
    public final String maxAge;
    public final String type;
    public final String primaryProxy;
    public final String proxies;
    public final String manufacturer;
    public final String model;
    public final String driver;
    public final String ipAddress;
    public final String port;
    public final String macAddress;
    public final Instant expireInstant;

    /**
     * Constructor
     * 
     * @param headers a map of parameter name / value pairs.
     */
    public SddpDevice(Map<String, String> headers) {
        from = headers.getOrDefault("From", "").replaceAll("^\"|\"$", "");
        host = headers.getOrDefault("Host", "").replaceAll("^\"|\"$", "");
        maxAge = headers.getOrDefault("Max-Age", "").replaceAll("^\"|\"$", "");
        type = headers.getOrDefault("Type", "").replaceAll("^\"|\"$", "");
        primaryProxy = headers.getOrDefault("Primary-Proxy", "").replaceAll("^\"|\"$", "");
        proxies = headers.getOrDefault("Proxies", "").replaceAll("^\"|\"$", "");
        manufacturer = headers.getOrDefault("Manufacturer", "").replaceAll("^\"|\"$", "");
        model = headers.getOrDefault("Model", "").replaceAll("^\"|\"$", "");
        driver = headers.getOrDefault("Driver", "").replaceAll("^\"|\"$", "");

        String[] fromParts = from.split(":");
        ipAddress = fromParts[0];
        port = fromParts.length > 1 ? fromParts[1] : "";

        String[] hostParts = host.split("-|_");
        macAddress = hostParts.length > 1 ? hostParts[hostParts.length - 1].replaceAll("(..)(?!$)", "$1-") : "";

        expireInstant = Instant.now().plusSeconds(maxAge.isBlank() ? 0 : Integer.parseInt(maxAge));
    }

    /**
     * Set uniqueness is determined by the From field only
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof SddpDevice other) {
            return Objects.equals(from, other.from);
        }
        return false;
    }

    /**
     * Set uniqueness is determined by the From field only
     */
    @Override
    public int hashCode() {
        return Objects.hash(from);
    }

    /**
     * Check if the creation time plus max-age instant is exceeded.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expireInstant);
    }
}
