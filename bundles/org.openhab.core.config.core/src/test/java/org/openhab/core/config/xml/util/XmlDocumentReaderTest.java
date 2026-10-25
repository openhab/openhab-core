/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openhab.core.config.core.xml.util.ConversionException;
import org.openhab.core.config.core.xml.util.NodeList;
import org.openhab.core.config.core.xml.util.NodeListConverter;
import org.openhab.core.config.core.xml.util.NodeValue;
import org.openhab.core.config.core.xml.util.NodeValueConverter;
import org.openhab.core.config.core.xml.util.XmlDocumentException;
import org.openhab.core.config.core.xml.util.XmlDocumentReader;
import org.openhab.core.config.core.xml.util.XmlMapping;

/**
 * Tests {@link XmlDocumentReader}.
 *
 * @author Wouter Born - Initial contribution
 * @author Holger Friedrich - Replace XStream
 */
@NonNullByDefault
public class XmlDocumentReaderTest {

    private static class NodeListReader extends XmlDocumentReader<NodeList> {
        @Override
        protected void registerConverters(XmlMapping mapping) {
            mapping.registerConverter(new NodeListConverter());
            mapping.registerConverter(new NodeValueConverter());
        }

        @Override
        protected void registerAliases(XmlMapping mapping) {
            mapping.alias("values", NodeList.class);
            mapping.alias("value", NodeValue.class);
        }
    }

    private final NodeListReader reader = new NodeListReader();

    @Test
    public void readsAliasedNodes() {
        NodeList nodeList = requireNonNull(reader.readFromXML("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!-- comment -->
                <values name="list">
                  <value>first</value>
                  <!-- comment -->
                  <value type="text"><![CDATA[a < b]]> &amp; c</value>
                  <value/>
                </values>"""));

        assertThat(nodeList.getNodeName(), is("values"));
        assertThat(nodeList.getAttributes(), is(Map.of("name", "list")));

        List<?> values = nodeList.getList();
        assertThat(values.size(), is(3));
        NodeValue second = (NodeValue) values.get(1);
        assertThat(((NodeValue) values.get(0)).getValue(), is("first"));
        assertThat(second.getValue(), is("a < b & c"));
        assertThat(second.getAttributes(), is(Map.of("type", "text")));
        assertThat(((NodeValue) values.get(2)).getValue(), is(""));
    }

    @Test
    public void usesLocalNamesAndIgnoresNamespaceDeclarations() {
        NodeList nodeList = requireNonNull(reader.readFromXML("""
                <ns:values xmlns:ns="https://openhab.org/schemas/test/v1.0.0"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="https://openhab.org/schemas/test/v1.0.0 test.xsd">
                  <value>first</value>
                </ns:values>"""));

        assertThat(nodeList.getNodeName(), is("values"));
        assertThat(nodeList.getAttributes(),
                is(Map.of("schemaLocation", "https://openhab.org/schemas/test/v1.0.0 test.xsd")));
        assertThat(nodeList.getList().size(), is(1));
    }

    @Test
    public void readsFromURL(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("values.xml");
        Files.writeString(file, "<values><value>äöü</value></values>");

        NodeList nodeList = requireNonNull(reader.readFromXML(file.toUri().toURL()));

        assertThat(((NodeValue) nodeList.getList().getFirst()).getValue(), is("äöü"));
    }

    @Test
    public void rejectsUnknownRootNode() {
        XmlDocumentException exception = assertThrows(XmlDocumentException.class, () -> reader.readFromXML("<void/>"));
        assertThat(exception, not(instanceOf(ConversionException.class)));
        assertThat(exception.getMessage(), containsString("'void'"));
    }

    @Test
    public void rejectsUnknownChildNodeAndReportsItsPath() {
        ConversionException exception = assertThrows(ConversionException.class, () -> reader
                .readFromXML("<values><value>first</value><value>second</value><string>third</string></values>"));
        assertThat(exception.getMessage(), containsString("'string'"));
        assertThat(exception.getMessage(), containsString("/values/string"));
    }

    @Test
    public void reportsPathWithSiblingIndexAndLineNumber() {
        ConversionException exception = assertThrows(ConversionException.class, () -> reader.readFromXML("""
                <values>
                  <value>first</value>
                  <values>
                    <value>second</value>
                    <value>third</value>
                    <void/>
                  </values>
                </values>"""));
        assertThat(exception.getMessage(), containsString("(path: /values/values/void, line: 6)"));
    }

    @Test
    public void valueIsTextBeforeFirstChildNode() {
        NodeList nodeList = requireNonNull(reader.readFromXML("<values><value>before<ignored/>after</value></values>"));

        assertThat(((NodeValue) nodeList.getList().getFirst()).getValue(), is("before"));
    }

    @Test
    public void rejectsMalformedDocument() {
        // like XStream: errors within the root element are conversion errors, all other errors are document errors
        assertThrows(ConversionException.class, () -> reader.readFromXML("<values><value>first</values>"));
        for (String xml : List.of("", "no xml", "<values", "<?xml version=\"1.0\"?>")) {
            XmlDocumentException exception = assertThrows(XmlDocumentException.class, () -> reader.readFromXML(xml));
            assertThat(xml, exception, not(instanceOf(ConversionException.class)));
        }
    }

    @Test
    public void rejectsMissingDocument(@TempDir Path tempDir) throws Exception {
        XmlDocumentException exception = assertThrows(XmlDocumentException.class,
                () -> reader.readFromXML(tempDir.resolve("missing.xml").toUri().toURL()));
        assertThat(exception, not(instanceOf(ConversionException.class)));
    }

    /**
     * Payload of XStream CVE-2013-7285: types must not be resolved from the document.
     *
     * @see <a href="https://x-stream.github.io/CVE-2013-7285.html">XStream - CVE-2013-7285</a>
     */
    @Test
    public void ignoresTypesInDocument() {
        String xml = """
                <contact class='dynamic-proxy'>
                  <interface>org.openhab.core.Contact</interface>
                  <handler class='java.beans.EventHandler'>
                    <target class='java.lang.ProcessBuilder'>
                      <command>
                        <string>calc.exe</string>
                      </command>
                    </target>
                    <action>start</action>
                  </handler>
                </contact>""";

        assertThrows(XmlDocumentException.class, () -> reader.readFromXML(xml));
        assertThrows(ConversionException.class,
                () -> reader.readFromXML("<values><value class='java.lang.ProcessBuilder'/><void/></values>"));
    }

    @Test
    public void rejectsEntityExpansion() {
        String xml = """
                <?xml version="1.0"?>
                <!DOCTYPE values [
                  <!ENTITY lol "lol">
                  <!ENTITY lol2 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
                  <!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">
                ]>
                <values><value>&lol3;</value></values>""";

        assertThrows(ConversionException.class, () -> reader.readFromXML(xml));
    }

    @Test
    public void rejectsExternalEntities(@TempDir Path tempDir) throws Exception {
        Path secret = tempDir.resolve("secret.txt");
        Files.writeString(secret, "secret");
        String xml = """
                <?xml version="1.0"?>
                <!DOCTYPE values [ <!ENTITY secret SYSTEM "%s"> ]>
                <values><value>&secret;</value></values>""".formatted(secret.toUri());

        assertThrows(ConversionException.class, () -> reader.readFromXML(xml));
    }
}
