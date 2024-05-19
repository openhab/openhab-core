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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A DTO containing data from an SDDP device discovery result.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class SddpInfo {
    public final String from;
    public final String host;
    public final String maxAge;
    public final String type;
    public final String primaryProxy;
    public final String proxies;
    public final String manufacturer;
    public final String model;
    public final String driver;
    public final Instant expireInstant;

    public SddpInfo(Map<String, String> headers) {
        from = headers.getOrDefault("From", "");
        host = headers.getOrDefault("Host", "");
        maxAge = headers.getOrDefault("Max-Age", "");
        type = headers.getOrDefault("Type", "");
        primaryProxy = headers.getOrDefault("Primary-Proxy", "");
        proxies = headers.getOrDefault("Proxies", "");
        manufacturer = headers.getOrDefault("Manufacturer", "");
        model = headers.getOrDefault("Model", "");
        driver = headers.getOrDefault("Driver", "");
        expireInstant = Instant.now().plusSeconds(maxAge.isBlank() ? 0 : Integer.parseInt(maxAge));
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expireInstant);
    }
}
