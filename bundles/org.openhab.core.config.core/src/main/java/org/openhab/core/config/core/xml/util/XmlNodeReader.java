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

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link XmlNodeReader} provides hierarchical access to the elements of a parsed XML document.
 * <p>
 * The reader is positioned at one element at a time. {@link #moveDown()} moves to the next unread child element,
 * {@link #moveUp()} returns to the parent element. Element and attribute names are local names, namespace declarations
 * are not reported as attributes.
 * <p>
 * The document is parsed with the StAX implementation of the JDK. DTDs and external entities are not supported.
 *
 * @author Holger Friedrich - Initial contribution
 */
@NonNullByDefault
public final class XmlNodeReader {

    private final Deque<Position> path = new ArrayDeque<>();

    private XmlNodeReader(Element root) {
        path.push(new Position(root, null, 0));
    }

    /**
     * Parses the XML document from the specified stream.
     *
     * @param stream the stream to read the XML document from
     * @param systemId the system ID of the document used in error messages
     * @return a reader positioned at the root element
     * @throws XmlDocumentException if the root element cannot be parsed
     * @throws ConversionException if the content of the root element cannot be parsed
     */
    static XmlNodeReader parse(InputStream stream, String systemId) throws XmlDocumentException {
        try {
            return parse(createInputFactory().createXMLStreamReader(systemId, stream));
        } catch (XMLStreamException e) {
            throw new XmlDocumentException("The XML document could not be parsed: " + e.getMessage(), e);
        }
    }

    /**
     * Parses the XML document from the specified reader.
     *
     * @param reader the reader to read the XML document from
     * @return a reader positioned at the root element
     * @throws XmlDocumentException if the root element cannot be parsed
     * @throws ConversionException if the content of the root element cannot be parsed
     */
    static XmlNodeReader parse(Reader reader) throws XmlDocumentException {
        try {
            return parse(createInputFactory().createXMLStreamReader(reader));
        } catch (XMLStreamException e) {
            throw new XmlDocumentException("The XML document could not be parsed: " + e.getMessage(), e);
        }
    }

    private static XmlNodeReader parse(XMLStreamReader reader) throws XmlDocumentException {
        Element root = null;
        try {
            Deque<Element> open = new ArrayDeque<>();
            while (reader.hasNext()) {
                switch (reader.next()) {
                    case XMLStreamConstants.START_ELEMENT -> {
                        Element element = new Element(reader.getLocalName(), reader.getLocation().getLineNumber());
                        for (int i = 0; i < reader.getAttributeCount(); i++) {
                            element.attributeNames.add(reader.getAttributeLocalName(i));
                            element.attributeValues.add(reader.getAttributeValue(i));
                        }
                        Element parent = open.peek();
                        if (parent != null) {
                            parent.children.add(element);
                        } else {
                            root = element;
                        }
                        open.push(element);
                    }
                    case XMLStreamConstants.END_ELEMENT -> open.pop();
                    case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA, XMLStreamConstants.SPACE -> {
                        Element current = open.peek();
                        // like XStream, the value of an element is the text before its first child element
                        if (current != null && current.children.isEmpty()) {
                            current.text.append(reader.getText());
                        }
                    }
                    default -> {
                        // comments, processing instructions and the document type declaration are ignored
                    }
                }
            }
            if (root == null) {
                throw new XmlDocumentException("The XML document does not contain a root element");
            }
            return new XmlNodeReader(root);
        } catch (XMLStreamException e) {
            // as before with XStream, only errors within the root element are conversion errors
            String message = "The XML document could not be parsed: " + e.getMessage();
            throw root == null ? new XmlDocumentException(message, e) : new ConversionException(message, e);
        } finally {
            try {
                reader.close();
            } catch (XMLStreamException e) {
                // nothing to do, the document has been read or a parse error is reported already
            }
        }
    }

    private static XMLInputFactory createInputFactory() {
        // always use the JDK implementation, independent of other StAX implementations available at runtime;
        // a new factory is created per document, as factories are not guaranteed to be thread-safe
        XMLInputFactory factory = XMLInputFactory.newDefaultFactory();
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return factory;
    }

    private Element current() {
        return path.getFirst().element;
    }

    /**
     * Returns the local name of the current element.
     *
     * @return the name of the current element
     */
    public String getNodeName() {
        return current().name;
    }

    /**
     * Returns the text content of the current element, without the content of its child elements.
     *
     * @return the text of the current element (could be empty)
     */
    public String getValue() {
        return current().text.toString();
    }

    /**
     * Returns the value of the attribute with the specified local name of the current element.
     *
     * @param name the local name of the attribute
     * @return the value of the attribute or null if the current element has no such attribute
     */
    public @Nullable String getAttribute(String name) {
        Element element = current();
        int index = element.attributeNames.indexOf(name);
        return index >= 0 ? element.attributeValues.get(index) : null;
    }

    /**
     * Returns the value of the attribute with the specified index of the current element.
     *
     * @param index the index of the attribute ({@code 0 <= index < getAttributeCount()})
     * @return the value of the attribute
     */
    public String getAttribute(int index) {
        return current().attributeValues.get(index);
    }

    /**
     * Returns the local name of the attribute with the specified index of the current element.
     *
     * @param index the index of the attribute ({@code 0 <= index < getAttributeCount()})
     * @return the local name of the attribute
     */
    public String getAttributeName(int index) {
        return current().attributeNames.get(index);
    }

    /**
     * Returns the number of attributes of the current element.
     *
     * @return the number of attributes
     */
    public int getAttributeCount() {
        return current().attributeNames.size();
    }

    /**
     * Returns whether the current element has child elements which have not been visited by {@link #moveDown()} yet.
     *
     * @return true if there are unread child elements
     */
    public boolean hasMoreChildren() {
        Position position = path.getFirst();
        return position.nextChild < position.element.children.size();
    }

    /**
     * Moves the reader to the next unread child element of the current element.
     *
     * @throws ConversionException if there are no unread child elements
     */
    public void moveDown() throws ConversionException {
        Position position = path.getFirst();
        if (position.nextChild >= position.element.children.size()) {
            throw new ConversionException("The node '" + getNodeName() + "' has no further child nodes!");
        }
        int index = position.nextChild++;
        path.push(new Position(position.element.children.get(index), position.element, index));
    }

    /**
     * Moves the reader back to the parent of the current element. Unread child elements of the current element are
     * skipped.
     *
     * @throws ConversionException if the reader is positioned at the root element
     */
    public void moveUp() throws ConversionException {
        if (path.size() <= 1) {
            throw new ConversionException("The root node '" + getNodeName() + "' has no parent node!");
        }
        path.pop();
    }

    /**
     * Returns the path of the current element, e.g. {@code /thing-descriptions/thing-type[2]/channels}.
     *
     * @return the path of the current element
     */
    public String getPath() {
        StringBuilder builder = new StringBuilder();
        path.descendingIterator().forEachRemaining(position -> {
            builder.append('/').append(position.element.name);
            Element parent = position.parent;
            if (parent != null) {
                // count the siblings with the same name only when a path is needed, e.g. for an error message
                int sameNameIndex = 0;
                for (int i = 0; i <= position.indexInParent; i++) {
                    if (parent.children.get(i).name.equals(position.element.name)) {
                        sameNameIndex++;
                    }
                }
                if (sameNameIndex > 1) {
                    builder.append('[').append(sameNameIndex).append(']');
                }
            }
        });
        return builder.toString();
    }

    /**
     * Returns the line number of the start tag of the current element in the XML document.
     *
     * @return the line number or -1 if it is not available
     */
    public int getLineNumber() {
        return current().lineNumber;
    }

    private static final class Element {
        private final String name;
        private final int lineNumber;
        private final List<String> attributeNames = new ArrayList<>();
        private final List<String> attributeValues = new ArrayList<>();
        private final StringBuilder text = new StringBuilder();
        private final List<Element> children = new ArrayList<>();

        private Element(String name, int lineNumber) {
            this.name = name;
            this.lineNumber = lineNumber;
        }
    }

    private static final class Position {
        private final Element element;
        private final @Nullable Element parent;
        private final int indexInParent;
        private int nextChild;

        private Position(Element element, @Nullable Element parent, int indexInParent) {
            this.element = element;
            this.parent = parent;
            this.indexInParent = indexInParent;
        }
    }
}
