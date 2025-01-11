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

    /**
     * The network address of the device.
     * For example: 192.168.4.237:1902
     */
    public final String from;

    /**
     * The host address of the device.
     * For example: JVC_PROJECTOR-E0DADC152802 or JVC_PROJECTOR-E0:DA:DC:15:28:02
     * Note: the last 12 resp. 17 characters represent the MAC address of the device.
     */
    public final String host;

    /**
     * The number of seconds after which the device shall no longer considered to be alive on the network.
     * For example: 1800 (a String value).
     */
    public final String maxAge;

    /**
     * The type of the device. Usually a colon delimited combination of a manufacturer id and a device type id.
     * For example: JVCKENWOOD:Projector
     */
    public final String type;

    /**
     * The id of the primary proxy that provides device services.
     * For example: projector
     */
    public final String primaryProxy;

    /**
     * A comma delimited list of proxies.
     * For example: projector,thingy,etc
     * Normally the first entry is the primary proxy.
     */
    public final String proxies;

    /**
     * The device manufacturer.
     * For example: JVCKENWOOD
     */
    public final String manufacturer;

    /**
     * The model number of the device.
     * For example: DLA-RS3100_NZ8
     */
    public final String model;

    /**
     * The driver id.
     * For example: projector_JVCKENWOOD_DLA-RS3100_NZ8.c4i
     */
    public final String driver;

    /**
     * The dotted IP address part of the 'from' field.
     * For example: 192.168.4.237
     */
    public final String ipAddress;

    /**
     * The port part of the 'from' field.
     * For example: 1902 (a String value)
     */
    public final String port;

    /**
     * The MAC address of the device as derived from the last 12 characters of the host field.
     * It is presented in lower-case, dash delimited, format.
     * For example: e0-da-dc-15-28-02
     * Therefore it may be used as a (unique) sub- part of a Thing UID.
     */
    public final String macAddress;

    /**
     * The instant after which the device shall be considered as having left the network.
     */
    public final Instant expireInstant;

    /**
     * Constructor.
     * 
     * @param headers a map of parameter name / value pairs.
     * @param offline indicates if the device is being created from a NOTIFY OFFLINE announcement.
     */
    public SddpDevice(Map<String, String> headers, boolean offline) {
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
        macAddress = hostParts.length <= 1 ? ""
                : hostParts[hostParts.length - 1].replace(":", "").replaceAll("(..)(?!$)", "$1-").toLowerCase();

        expireInstant = offline ? Instant.now().minusMillis(1)
                : Instant.now().plusSeconds(maxAge.isBlank() ? 0 : Integer.parseInt(maxAge));
    }

    /**
     * Set uniqueness is determined by the From field only
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        return (obj instanceof SddpDevice other) && Objects.equals(from, other.from);
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
