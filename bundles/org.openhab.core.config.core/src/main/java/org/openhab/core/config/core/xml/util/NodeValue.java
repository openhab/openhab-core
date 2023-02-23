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

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link NodeValue} class contains the node name and its according value for an XML tag.
 * <p>
 * This class can be used for an intermediate conversion result of a single value for an XML tag. The conversion can be
 * done by using the according {@link NodeValueConverter}.
 * <p>
 * <b>Hint:</b> This class is immutable.
 *
 * @author Michael Grammling - Initial contribution
 */
@NonNullByDefault
public class NodeValue implements NodeName {

    private String nodeName;
    private @Nullable Map<String, String> attributes;
    private @Nullable Object value;

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param nodeName the name of the node this object belongs to (must not be empty)
     * @param attributes the attributes of the node this object belongs to
     * @param value the value of the node this object belongs to
     * @throws IllegalArgumentException if the name of the node is empty
     */
    public NodeValue(String nodeName, @Nullable Map<String, String> attributes, @Nullable Object value)
            throws IllegalArgumentException {
        if (nodeName.isEmpty()) {
            throw new IllegalArgumentException("The name of the node must not be empty!");
        }

        this.nodeName = nodeName;
        this.attributes = attributes;
        this.value = formatText(value);
    }

    private @Nullable Object formatText(@Nullable Object object) {
        // fixes a formatting problem with line breaks in text
        if (object instanceof String) {
            return ((String) object).replaceAll("\\n\\s*", " ").trim();
        }

        return object;
    }

    @Override
    public String getNodeName() {
        return this.nodeName;
    }

    /**
     * Returns the attributes of the node.
     *
     * @return the attributes of the node
     */
    public @Nullable Map<String, String> getAttributes() {
        return this.attributes;
    }

    /**
     * Returns the value of the node.
     *
     * @return the value of the node (could be null or empty)
     */
    public @Nullable Object getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return "NodeValue [nodeName=" + nodeName + ", value=" + value + "]";
    }
}
