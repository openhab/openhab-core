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
package org.openhab.core.config.xml.util;

import static org.eclipse.jdt.annotation.Checks.requireNonNull;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionBuilder;
import org.openhab.core.config.core.xml.util.XmlDocumentReader;

import com.acme.Product;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.ForbiddenClassException;

/**
 * Tests {@link XmlDocumentReader}.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public class XmlDocumentReaderTest {

    private static final String OHC_PACKAGE_PREFIX = "org.openhab.core.";

    private class ConfigDescriptionReader extends XmlDocumentReader<ConfigDescription> {
        @Override
        protected void registerConverters(XStream xstream) {
        }

        @Override
        protected void registerAliases(XStream xstream) {
        }
    }

    private @Nullable ConfigDescription readXML(String xml) throws IOException {
        Path tempFile = Files.createTempFile(null, null);
        tempFile.toFile().deleteOnExit();
        Files.write(tempFile, xml.getBytes(StandardCharsets.UTF_8));
        return new ConfigDescriptionReader().readFromXML(tempFile.toUri().toURL());
    }

    @Test
    public void defaultSecurityAllowsDeserializingOHCobjects() throws Exception {
        assertThat(ConfigDescription.class.getPackageName(), startsWith(OHC_PACKAGE_PREFIX));

        URI testURI = URI.create("test:uri");
        ConfigDescription configDescription = ConfigDescriptionBuilder.create(testURI).build();

        String xml = new XStream().toXML(configDescription);

        ConfigDescription readConfigDescription = requireNonNull(readXML(xml));

        assertThat(readConfigDescription.getUID(), is(testURI));
    }

    @Test
    public void defaultSecurityDisallowsDeserializingNonOHCobjects() throws Exception {
        assertThat(Product.class.getPackageName(), not(startsWith(OHC_PACKAGE_PREFIX)));

        String xml = new XStream().toXML(new Product());

        assertThrows(ForbiddenClassException.class, () -> readXML(xml));
    }

    /**
     * @see https://x-stream.github.io/CVE-2013-7285.html
     */
    @Test
    public void defaultSecurityProtectsAgainstRemoteCodeExecution() throws Exception {
        String xml = "<contact class='dynamic-proxy'>\n" //
                + "  <interface>org.openhab.core.Contact</interface>\n"
                + "  <handler class='java.beans.EventHandler'>\n" //
                + "    <target class='java.lang.ProcessBuilder'>\n" //
                + "      <command>\n" //
                + "        <string>calc.exe</string>\n" //
                + "      </command>\n" //
                + "    </target>\n" //
                + "    <action>start</action>\n" //
                + "  </handler>\n" //
                + "</contact>";

        assertThrows(ForbiddenClassException.class, () -> readXML(xml));
    }

    /**
     * @see https://x-stream.github.io/CVE-2017-7957.html
     */
    @Test
    public void defaultSecurityProtectsAgainstDenialOfServiceAttacks() throws Exception {
        assertThrows(ForbiddenClassException.class, () -> readXML("<void/>"));
        assertThrows(ForbiddenClassException.class, () -> readXML("<string class='void'>Hello, world!</string>"));
    }
}
