/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.config.xml.util;

import java.net.URL;

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
 * @author Michael Grammling - Initial Contribution
 *
 * @param <T> the result type of the conversion
 */
public abstract class XmlDocumentReader<T> {

    private XStream xstream;

    /**
     * The default constructor of this class initializes the {@code XStream} object, and calls
     * the abstract methods {@link #registerConverters()} and {@link #registerAliases()}.
     */
    public XmlDocumentReader() {
        StaxDriver driver = new StaxDriver();

        this.xstream = new XStream(driver);

        registerConverters(this.xstream);
        registerAliases(this.xstream);
    }

    /**
     * Sets the classloader for {@link XStream}.
     *
     * @param classLoader the classloader to set (must not be null)
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.xstream.setClassLoader(classLoader);
    }

    /**
     * Registers any {@link Converter}s at the {@link XStream} instance.
     *
     * @param xstream the XStream object to be configured
     */
    public abstract void registerConverters(XStream xstream);

    /**
     * Registers any aliases at the {@link XStream} instance.
     *
     * @param xstream the XStream object to be configured
     */
    public abstract void registerAliases(XStream xstream);

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
    public T readFromXML(URL xmlURL) throws ConversionException {
        if (xmlURL != null) {
            return (T) this.xstream.fromXML(xmlURL);
        }

        return null;
    }

}
