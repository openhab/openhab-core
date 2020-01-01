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

import java.util.Map;

/**
 * The {@link NodeAttributes} class contains all attributes for an XML tag.
 * <p>
 * This class <i>DOES NOT</i> support XML tags with attributes <i>AND</i> values, it only supports attributes.
 * <p>
 * This class can be used for an intermediate conversion result of attributes for an XML tag. The conversion can be done
 * by using the according {@link NodeAttributesConverter}.
 * <p>
 * <b>Hint:</b> This class is immutable.
 *
 * @author Michael Grammling - Initial contribution
 */
public class NodeAttributes implements NodeName {

    private String nodeName;
    private Map<String, String> attributes;

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param nodeName the name of the node this object belongs to (must neither be null, nor empty)
     * @param attributes the map of all attributes of the node this object belongs to
     *            by key-value pairs (could be null or empty)
     * @throws IllegalArgumentException if the name of the node is null or empty
     */
    public NodeAttributes(String nodeName, Map<String, String> attributes) throws IllegalArgumentException {
        if ((nodeName == null) || (nodeName.isEmpty())) {
            throw new IllegalArgumentException("The name of the node must neither be null nor empty!");
        }

        this.nodeName = nodeName;
        this.attributes = attributes;
    }

    @Override
    public String getNodeName() {
        return this.nodeName;
    }

    /**
     * Returns the value of the specified attribute.
     *
     * @param name the name of the attribute whose value should be returned (could be null or empty)
     * @return the value of the specified attribute (could be null or empty)
     */
    public String getAttribute(String name) {
        if (this.attributes != null) {
            return this.attributes.get(name);
        }

        return null;
    }

    /**
     * Returns the map of all attributes of a node by key-value pairs.
     *
     * @return the map of all attributes of a node (could be null or empty)
     */
    public Map<String, String> getAttributes() {
        return this.attributes;
    }

    @Override
    public String toString() {
        return "NodeAttributes [nodeName=" + nodeName + ", attributes=" + attributes + "]";
    }

}
