/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.thoughtworks.xstream.converters.ConversionException;

/**
 * The {@link NodeList} class contains the node name and its according list of values for an XML tag.
 * <p>
 * This class can be used for an intermediate conversion result of a list of values for an XML tag. The conversion can
 * be done by using the according {@link NodeListConverter}.
 * <p>
 * <b>Hint:</b> This class is immutable.
 *
 * @author Michael Grammling - Initial contribution
 */
public class NodeList implements NodeName {

    private String nodeName;
    private Map<String, String> attributes;
    private List<?> list;

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param nodeName the name of the node this object belongs to (must neither be null, nor empty)
     * @param attributes all attributes of the node this object belongs to (could be null or empty)
     * @param list the list of the node this object belongs to (could be null or empty)
     * @throws IllegalArgumentException if the name of the node is null or empty
     */
    public NodeList(String nodeName, Map<String, String> attributes, List<?> list) throws IllegalArgumentException {
        if ((nodeName == null) || (nodeName.isEmpty())) {
            throw new IllegalArgumentException("The name of the node must neither be null nor empty!");
        }

        this.attributes = attributes;
        this.nodeName = nodeName;
        this.list = list;
    }

    @Override
    public String getNodeName() {
        return this.nodeName;
    }

    /**
     * Returns the attributes of the node as key-value map
     *
     * @return the attributes of the node as key-value map (could be null or empty).
     */
    public Map<String, String> getAttributes() {
        return this.attributes;
    }

    /**
     * Returns the list of values of the node
     *
     * @return the list of values of the node (could be null or empty).
     */
    public List<?> getList() {
        return this.list;
    }

    /**
     * @see #getAttributes(String, String, String)
     */
    public List<String> getAttributes(String nodeName, String attributeName) throws ConversionException {
        return getAttributes(nodeName, attributeName, null);
    }

    /**
     * Returns the attributes of the specified XML node and attribute name for the whole list.
     * <p>
     * This list <i>MUST ONLY</i> contain {@link NodeAttributes}.
     *
     * @param nodeName the node name to be considered (must neither be null, nor empty)
     * @param attributeName the attribute name to be considered (must neither be null, nor empty)
     * @param formattedText the format for the output text using the placeholder format
     *            of the Java String (could be null or empty)
     * @return the attributes of the specified XML node and attribute name for the whole list
     *         (could be null or empty)
     * @throws ConversionException if the attribute could not be found in the specified node
     */
    @SuppressWarnings("unchecked")
    public List<String> getAttributes(String nodeName, String attributeName, String formattedText)
            throws ConversionException {
        List<String> attributes = null;

        if (this.list != null) {
            attributes = new ArrayList<>(this.list.size());

            String format = formattedText;
            if ((format == null) || (format.isEmpty())) {
                format = "%s";
            }

            for (NodeAttributes node : (List<NodeAttributes>) this.list) {
                if (nodeName.equals(node.getNodeName())) {
                    String attributeValue = node.getAttribute(attributeName);

                    if (attributeValue != null) {
                        attributes.add(String.format(format, attributeValue));
                    } else {
                        throw new ConversionException(
                                "Missing attribute '" + attributeName + "' in '" + nodeName + "'!");
                    }
                } else {
                    throw new ConversionException("Invalid attribute in '" + nodeName + "'!");
                }
            }
        }

        return attributes;
    }

    @Override
    public String toString() {
        return "NodeList [nodeName=" + nodeName + ", attributes=" + attributes + ", list=" + list + "]";
    }

}
