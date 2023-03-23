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
package org.openhab.core.config.core.xml.util;

import java.net.URL;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * The {@link XmlDocumentReader} is an abstract class used to read XML documents
 * of a certain type and converts them to its according objects.
 * <p>
 * This class uses {@code XStream} and {@code StAX} to parse and convert the XML document.
 *
 * @author Michael Grammling - Initial contribution
 * @author Wouter Born - Configure XStream security
 *
 * @param <T> the result type of the conversion
 */
@NonNullByDefault
public abstract class XmlDocumentReader<@NonNull T> {

    protected static final String[] DEFAULT_ALLOWED_TYPES_WILDCARD = new String[] { "org.openhab.core.**" };

    private final XStream xstream = new XStream(new StaxDriver());

    /**
     * The default constructor of this class initializes the {@code XStream} object by calling:
     *
     * <ol>
     * <li>{@link #configureSecurity()}</li>
     * <li>{@link #registerConverters()}</li>
     * <li>{@link #registerAliases()}</li>
     * </ol>
     */
    public XmlDocumentReader() {
        configureSecurity(xstream);
        registerConverters(xstream);
        registerAliases(xstream);
    }

    /**
     * Sets the classloader for {@link XStream}.
     *
     * @param classLoader the classloader to set (must not be null)
     */
    protected void setClassLoader(ClassLoader classLoader) {
        xstream.setClassLoader(classLoader);
    }

    /**
     * Configures the security of the {@link XStream} instance to protect against vulnerabilities.
     *
     * @param xstream the XStream object to be configured
     *
     * @see https://x-stream.github.io/security.html
     */
    protected void configureSecurity(XStream xstream) {
        xstream.allowTypesByWildcard(DEFAULT_ALLOWED_TYPES_WILDCARD);
    }

    /**
     * Registers any {@link Converter}s at the {@link XStream} instance.
     *
     * @param xstream the XStream object to be configured
     */
    protected abstract void registerConverters(XStream xstream);

    /**
     * Registers any aliases at the {@link XStream} instance.
     *
     * @param xstream the XStream object to be configured
     */
    protected abstract void registerAliases(XStream xstream);

    /**
     * Reads the XML document containing a specific XML tag from the specified {@link URL} and converts it to the
     * according object.
     * <p>
     * This method returns {@code null} if the given URL is {@code null}.
     *
     * @param xmlURL the URL pointing to the XML document to be read (could be null)
     * @return the conversion result object (could be null)
     * @throws ConversionException if the specified document contains invalid content
     */
    @SuppressWarnings("unchecked")
    public @Nullable T readFromXML(URL xmlURL) throws ConversionException {
        return (@Nullable T) xstream.fromXML(xmlURL);
    }
}
