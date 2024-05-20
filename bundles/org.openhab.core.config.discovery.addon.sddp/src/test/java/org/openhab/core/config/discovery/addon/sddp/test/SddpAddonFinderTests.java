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
package org.openhab.core.config.discovery.addon.sddp.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonMatchProperty;
import org.openhab.core.config.discovery.addon.AddonFinderConstants;
import org.openhab.core.config.discovery.addon.sddp.SddpAddonFinder;
import org.openhab.core.config.discovery.sddp.SddpDevice;

/**
 * JUnit tests for the {@link SddpAddonFinder}.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class SddpAddonFinderTests {

    private static final Map<String, String> DEVICE_FIELDS = Map.of(
    // @formatter:off
                "From", "\"192.168.4.237:1902\"",
                "Host", "\"JVC_PROJECTOR-E0DADC152802\"",
                "Max-Age", "1800",
                "Type", "\"JVCKENWOOD:Projector\"",
                "Primary-Proxy", "\"projector\"",
                "Proxies", "\"projector\"",
                "Manufacturer", "\"JVCKENWOOD\"",
                "Model", "\"DLA-RS3100_NZ8\"",
                "Driver", "\"projector_JVCKENWOOD_DLA-RS3100_NZ8.c4i\"");
        // @formatter:on

    private SddpDevice createDevice() {
        return new SddpDevice(DEVICE_FIELDS);
    }

    private List<AddonInfo> createAddonInfos() {
        AddonDiscoveryMethod method = new AddonDiscoveryMethod().setServiceType(AddonFinderConstants.SERVICE_TYPE_SDDP)
                .setMatchProperties(List.of(new AddonMatchProperty("host", "JVC.*")));
        List<AddonInfo> addonInfos = new ArrayList<>();
        addonInfos.add(AddonInfo.builder("jvc", "binding").withName("JVC").withDescription("JVC Kenwood")
                .withDiscoveryMethods(List.of(method)).build());
        return addonInfos;
    }

    @Test
    public void testFinder() {
        SddpDevice device = createDevice();
        List<AddonInfo> addonInfos = createAddonInfos();
        SddpAddonFinder finder = new SddpAddonFinder();

        finder.setAddonCandidates(addonInfos);

        Set<AddonInfo> suggestions;
        AddonInfo info;

        finder.deviceAdded(device);
        suggestions = finder.getSuggestedAddons();
        assertFalse(suggestions.isEmpty());
        info = suggestions.stream().findFirst().orElse(null);
        assertNotNull(info);
        assertEquals("JVC Kenwood", info.getDescription());

        finder.deviceRemoved(device);
        suggestions = finder.getSuggestedAddons();
        assertTrue(suggestions.isEmpty());
    }
}
