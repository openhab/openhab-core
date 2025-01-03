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
package org.openhab.core.addon;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.addon.internal.xml.AddonInfoListReader;

/**
 * JUnit tests for {@link AddonInfoListReader}.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
class AddonInfoListReaderTest {

    // @formatter:off
    private final String testXml =
            "<addon-info-list><addons>"
            + "  <addon:addon id=\"groovyscripting\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
            + "    xmlns:addon=\"https://openhab.org/schemas/addon/v1.0.0\""
            + "    xsi:schemaLocation=\"https://openhab.org/schemas/addon/v1.0.0 https://openhab.org/schemas/addon-1.0.0.xsd\">"
            + "    <type>automation</type>"
            + "    <name>Groovy Scripting</name>"
            + "    <description>This adds a Groovy script engine.</description>"
            + "    <connection>none</connection>"
            + "    <discovery-methods>"
            + "      <discovery-method>"
            + "        <service-type>mdns</service-type>"
            + "        <discovery-parameters>"
            + "          <discovery-parameter>"
            + "            <name>mdnsServiceType</name>"
            + "            <value>_printer._tcp.local.</value>"
            + "          </discovery-parameter>"
            + "        </discovery-parameters>"
            + "        <match-properties>"
            + "          <match-property>"
            + "            <name>rp</name>"
            + "            <regex>.*</regex>"
            + "          </match-property>"
            + "          <match-property>"
            + "            <name>ty</name>"
            + "            <regex>hp (.*)</regex>"
            + "          </match-property>"
            + "        </match-properties>"
            + "      </discovery-method>"
            + "      <discovery-method>"
            + "        <service-type>upnp</service-type>"
            + "        <match-properties>"
            + "          <match-property>"
            + "            <name>modelName</name>"
            + "            <regex>Philips hue bridge</regex>"
            + "          </match-property>"
            + "        </match-properties>"
            + "      </discovery-method>"
            + "    </discovery-methods>"
            + "  </addon:addon>"
            + "</addons></addon-info-list>";
    // @formatter:on

    @Test
    void testAddonInfoListReader() {
        AddonInfoList addons = null;
        try {
            AddonInfoListReader reader = new AddonInfoListReader();
            addons = reader.readFromXML(testXml);
        } catch (Exception e) {
            fail(e);
        }
        assertNotNull(addons);
        List<AddonInfo> addonsInfos = addons.getAddons();
        assertEquals(1, addonsInfos.size());
        AddonInfo addon = addonsInfos.getFirst();
        assertNotNull(addon);
        List<AddonDiscoveryMethod> discoveryMethods = addon.getDiscoveryMethods();
        assertNotNull(discoveryMethods);
        assertEquals(2, discoveryMethods.size());

        AddonDiscoveryMethod method = discoveryMethods.getFirst();
        assertNotNull(method);
        assertEquals("mdns", method.getServiceType());
        List<AddonParameter> parameters = method.getParameters();
        assertNotNull(parameters);
        assertEquals(1, parameters.size());
        AddonParameter parameter = parameters.getFirst();
        assertNotNull(parameter);
        assertEquals("mdnsServiceType", parameter.getName());
        assertEquals("_printer._tcp.local.", parameter.getValue());
        List<AddonMatchProperty> matchProperties = method.getMatchProperties();
        assertNotNull(matchProperties);
        assertEquals(2, matchProperties.size());
        AddonMatchProperty property = matchProperties.getFirst();
        assertNotNull(property);
        assertEquals("rp", property.getName());
        assertEquals(".*", property.getRegex());
        assertTrue(property.getPattern().matcher("the cat sat on the mat").matches());

        method = discoveryMethods.get(1);
        assertNotNull(method);
        assertEquals("upnp", method.getServiceType());
        parameters = method.getParameters();
        assertNotNull(parameters);
        assertEquals(0, parameters.size());
        matchProperties = method.getMatchProperties();
        assertNotNull(matchProperties);
        assertEquals(1, matchProperties.size());
        property = matchProperties.getFirst();
        assertNotNull(property);
        assertEquals("modelName", property.getName());
        assertEquals("Philips hue bridge", property.getRegex());
        assertTrue(property.getPattern().matcher("Philips hue bridge").matches());
    }
}
