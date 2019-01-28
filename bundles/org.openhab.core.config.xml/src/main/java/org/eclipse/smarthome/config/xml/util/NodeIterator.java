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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.thoughtworks.xstream.converters.ConversionException;

/**
 * The {@link NodeIterator} is an {@link Iterator} for nodes of an XML document.
 * <p>
 * This iterator offers a simple mechanism iterating through {@code Node}* objects by considering the required or
 * optional occurrences of attributes, values or list of values.
 *
 * @author Michael Grammling - Initial Contribution
 */
public class NodeIterator implements Iterator<Object> {

    private List<?> nodes;
    private int index = 0;

    /**
     * Creates a new instance of this class with the specified argument.
     *
     * @param nodes the list of nodes to be iterated through (could be null or empty)
     */
    public NodeIterator(List<?> nodes) {
        this.nodes = (nodes != null) ? nodes : new ArrayList<>(0);
    }

    @Override
    public boolean hasNext() {
        return (index < this.nodes.size());
    }

    @Override
    public Object next() {
        if (hasNext()) {
            return this.nodes.get(index++);
        }

        return null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Reverts the last {@link #next()} call.
     * <p>
     * After this method returns, the iteration counter is the same as before the last {@link #next()} call. Calling
     * this method multiple times decreases the iteration counter by one until the index of 0 has been reached.
     */
    public void revert() {
        if (index > 0) {
            index--;
        }
    }

    /**
     * Ensures that the end of the node has been reached.
     *
     * @throws ConversionException if the end of the node has not reached yet
     */
    public void assertEndOfType() throws ConversionException {
        if (hasNext()) {
            List<Object> nodes = new ArrayList<>();
            while (hasNext()) {
                nodes.add(next());
            }

            throw new ConversionException("The document is invalid, it contains further" + " unsupported data: "
                    + nodes + "!");
        }
    }

    /**
     * Returns the next object if the specified name of the node fits to the next node,
     * or {@code null} if the node does not exist. In the last case the iterator will
     * <i>not</i> increase its iteration counter.
     *
     * @param nodeName the name of the node to be read next (must neither be null, nor empty)
     * @param required true if the occurrence of the node has to be ensured
     * @return the next object if the specified name of the node fits to the next node,
     *         otherwise null
     * @throws ConversionException if the specified node could not be found in the next node
     *             however it was specified as required
     */
    public Object next(String nodeName, boolean required) throws ConversionException {
        if (hasNext()) {
            Object nextNode = next();

            if (nextNode instanceof NodeName) {
                if (nodeName.equals(((NodeName) nextNode).getNodeName())) {
                    return nextNode;
                }
            }

            this.index--;
        }

        if (required) {
            throw new ConversionException("The node '" + nodeName + "' is missing!");
        }

        return null;
    }

    /**
     * Returns the next attribute if the specified name of the node fits to the next node
     * and the attribute with the specified name could be found, or {@code null} if the
     * node or attribute does not exist. In the last case the iterator will <i>not</i>
     * increase its iteration counter.
     * <p>
     * The next node must be of the type {@link NodeAttributes}.
     *
     * @param nodeName the name of the node to be read next (must neither be null, nor empty)
     * @param attributeName the name of the attribute of the node to be read next
     *            (must neither be null, nor empty)
     * @param required true if the occurrence of the node's attribute has to be ensured
     * @return the next attribute of the specified name of the node and attribute
     *         (could be null or empty)
     * @throws ConversionException if the specified node's attribute could not be found in the
     *             next node however it was specified as required
     */
    public String nextAttribute(String nodeName, String attributeName, boolean required) throws ConversionException {
        if (hasNext()) {
            Object nextNode = next();

            if (nextNode instanceof NodeAttributes) {
                if (nodeName.equals(((NodeName) nextNode).getNodeName())) {
                    return ((NodeAttributes) nextNode).getAttribute(attributeName);
                }
            }

            this.index--;
        }

        if (required) {
            throw new ConversionException("The attribute '" + attributeName + "' in the node '" + nodeName
                    + "' is missing!");
        }

        return null;
    }

    /**
     * Returns the next value if the specified name of the node fits to the next node,
     * or {@code null} if the node does not exist. In the last case the iterator will
     * <i>not</i> increase its iteration counter.
     * <p>
     * The next node must be of the type {@link NodeValue}.
     *
     * @param nodeName the name of the node to be read next (must neither be null, nor empty)
     * @param required true if the occurrence of the node's value has to be ensured
     * @return the next value of the specified name of the node (could be null or empty)
     * @throws ConversionException if the specified node's value could not be found in the
     *             next node however it was specified as required
     */
    public Object nextValue(String nodeName, boolean required) throws ConversionException {
        Object value = next(nodeName, required);

        if (value instanceof NodeValue) {
            return ((NodeValue) value).getValue();
        }

        return null;
    }

    /**
     * Returns the next list of values if the specified name of the node fits to the
     * next node, or {@code null} if the node does not exist. In the last case the
     * iterator will <i>not</i> increase its iteration counter.
     * <p>
     * The next node must be of the type {@link NodeList}.
     *
     * @param nodeName the name of the node to be read next (must neither be null, nor empty)
     * @param required true if the occurrence of the node's list of values has to be ensured
     * @return the next list of values of the specified name of the node (could be null or empty)
     * @throws ConversionException if the specified node's list of values could not be found
     *             in the next node however it was specified as required
     */
    public List<?> nextList(String nodeName, boolean required) throws ConversionException {
        Object list = next(nodeName, required);

        if (list instanceof NodeList) {
            return ((NodeList) list).getList();
        }

        return null;
    }

}
