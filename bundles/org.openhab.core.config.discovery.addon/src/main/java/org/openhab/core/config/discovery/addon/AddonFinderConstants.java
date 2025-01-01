/**
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
package org.openhab.core.config.discovery.addon;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This {@link AddonFinderConstants} contains constants describing add-on finders available in core.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class AddonFinderConstants {

    public static final String ADDON_SUGGESTION_FINDER = "-addon-suggestion-finder";

    public static final String SERVICE_TYPE_IP = "ip";
    public static final String CFG_FINDER_IP = "suggestionFinderIp";
    public static final String SERVICE_NAME_IP = SERVICE_TYPE_IP + ADDON_SUGGESTION_FINDER;

    public static final String SERVICE_TYPE_MDNS = "mdns";
    public static final String CFG_FINDER_MDNS = "suggestionFinderMdns";
    public static final String SERVICE_NAME_MDNS = SERVICE_TYPE_MDNS + ADDON_SUGGESTION_FINDER;

    public static final String SERVICE_TYPE_SDDP = "sddp";
    public static final String CFG_FINDER_SDDP = "suggestionFinderSddp";
    public static final String SERVICE_NAME_SDDP = SERVICE_TYPE_SDDP + ADDON_SUGGESTION_FINDER;

    public static final String SERVICE_TYPE_UPNP = "upnp";
    public static final String CFG_FINDER_UPNP = "suggestionFinderUpnp";
    public static final String SERVICE_NAME_UPNP = SERVICE_TYPE_UPNP + ADDON_SUGGESTION_FINDER;

    public static final String SERVICE_TYPE_USB = "usb";
    public static final String CFG_FINDER_USB = "suggestionFinderUsb";
    public static final String SERVICE_NAME_USB = SERVICE_TYPE_USB + ADDON_SUGGESTION_FINDER;

    public static final List<String> SUGGESTION_FINDERS = List.of(SERVICE_NAME_IP, SERVICE_NAME_MDNS, SERVICE_NAME_SDDP,
            SERVICE_NAME_UPNP, SERVICE_NAME_USB);

    public static final Map<String, String> SUGGESTION_FINDER_TYPES = Map.of(SERVICE_NAME_IP, SERVICE_TYPE_IP,
            SERVICE_NAME_MDNS, SERVICE_TYPE_MDNS, SERVICE_NAME_SDDP, SERVICE_TYPE_SDDP, SERVICE_NAME_UPNP,
            SERVICE_TYPE_UPNP, SERVICE_NAME_USB, SERVICE_TYPE_USB);

    public static final Map<String, String> SUGGESTION_FINDER_CONFIGS = Map.of(SERVICE_NAME_IP, CFG_FINDER_IP,
            SERVICE_NAME_MDNS, CFG_FINDER_MDNS, SERVICE_NAME_SDDP, CFG_FINDER_SDDP, SERVICE_NAME_UPNP, CFG_FINDER_UPNP,
            SERVICE_NAME_USB, CFG_FINDER_USB);
}
