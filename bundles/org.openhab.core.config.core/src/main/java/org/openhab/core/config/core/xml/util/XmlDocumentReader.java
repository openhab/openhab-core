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
package org.openhab.core.config.core.xml.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link XmlDocumentReader} is an abstract class used to read XML documents
 * of a certain type and converts them to its according objects.
 * <p>
 * This class uses {@code StAX} to parse the XML document and the registered {@link XmlConverter}s to convert it.
 * Only XML elements with a registered alias are converted, no types are instantiated via reflection.
 *
 * @author Michael Grammling - Initial contribution
 * @author Wouter Born - Configure XStream security
 * @author Holger Friedrich - Replace XStream
 *
 * @param <T> the result type of the conversion
 */
@NonNullByDefault
public abstract class XmlDocumentReader<@NonNull T> {

    private final XmlMapping mapping = new XmlMapping();

    /**
     * The default constructor of this class initializes the {@link XmlMapping} by calling:
     *
     * <ol>
     * <li>{@link #registerConverters}</li>
     * <li>{@link #registerAliases}</li>
     * </ol>
     */
    public XmlDocumentReader() {
        registerConverters(mapping);
        registerAliases(mapping);
    }

    /**
     * Registers any {@link XmlConverter}s at the {@link XmlMapping}.
     *
     * @param mapping the mapping to be configured
     */
    protected abstract void registerConverters(XmlMapping mapping);

    /**
     * Registers any aliases at the {@link XmlMapping}.
     *
     * @param mapping the mapping to be configured
     */
    protected abstract void registerAliases(XmlMapping mapping);

    /**
     * Reads the XML document containing a specific XML tag from the specified {@link URL} and converts it to the
     * according object.
     *
     * @param xmlURL the URL pointing to the XML document to be read
     * @return the conversion result object (could be null)
     * @throws XmlDocumentException if the document cannot be read or its root element is not supported
     * @throws ConversionException if the root element contains invalid content
     */
    public @Nullable T readFromXML(URL xmlURL) throws XmlDocumentException {
        try (InputStream stream = xmlURL.openStream()) {
            return convert(XmlNodeReader.parse(stream, xmlURL.toExternalForm()));
        } catch (IOException e) {
            throw new XmlDocumentException("The XML document '" + xmlURL + "' could not be read: " + e.getMessage(), e);
        }
    }

    /**
     * Reads the XML document containing a specific XML tag from the specified xml string and converts it to the
     * according object.
     *
     * @param xml a string containing the XML document to be read.
     * @return the conversion result object (could be null).
     * @throws XmlDocumentException if the document cannot be parsed or its root element is not supported
     * @throws ConversionException if the root element contains invalid content
     */
    public @Nullable T readFromXML(String xml) throws XmlDocumentException {
        return convert(XmlNodeReader.parse(new StringReader(xml)));
    }

    @SuppressWarnings("unchecked")
    private @Nullable T convert(XmlNodeReader reader) throws XmlDocumentException {
        Class<?> type = mapping.lookupAlias(reader.getNodeName());
        if (type == null) {
            throw new XmlDocumentException("The root node '" + reader.getNodeName() + "' is not supported!");
        }
        try {
            return (@Nullable T) new XmlUnmarshallingContext(reader, mapping).convertAnother(type);
        } catch (ConversionException e) {
            // add the position of the failing element; the reader is not moved up when an exception occurs
            throw new ConversionException(
                    e.getMessage() + " (path: " + reader.getPath() + ", line: " + reader.getLineNumber() + ")", e);
        }
    }
}
