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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link ConverterValueMap} reads all children elements of a node and provides
 * them as key-value pair map.
 * <p>
 * This class should be used for nodes whose children elements <i>only</i> contain simple values (without children) and
 * whose order is unpredictable. There must be only one children with the same name.
 *
 * @author Michael Grammling - Initial contribution
 * @author Alex Tugarev - Extended for options and filter criteria
 */
public class ConverterValueMap {

    private HierarchicalStreamReader reader;
    private Map<String, Object> valueMap;
    private UnmarshallingContext context;

    /**
     * Creates a new instance of this class with the specified parameter.
     *
     * @param reader the reader to be used to read-in all children (must not be null)
     * @param context
     */
    public ConverterValueMap(HierarchicalStreamReader reader, UnmarshallingContext context) {
        this(reader, -1, context);
    }

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param reader the reader to be used to read-in all children (must not be null)
     * @param numberOfValues the number of children to be read-in (< 0 = until end of section)
     * @param context
     * @throws ConversionException if not all children could be read-in
     */
    public ConverterValueMap(HierarchicalStreamReader reader, int numberOfValues, UnmarshallingContext context)
            throws ConversionException {
        this.reader = reader;
        this.context = context;
        this.valueMap = readValueMap(this.reader, numberOfValues >= -1 ? numberOfValues : -1, this.context);
    }

    /**
     * Returns the key-value map containing all read-in children.
     *
     * @return the key-value map containing all read-in children (not null, could be empty)
     */
    public Map<String, Object> getValueMap() {
        return this.valueMap;
    }

    /**
     * Reads-in {@code N} children in a key-value map and returns it.
     *
     * @param reader the reader to be used to read-in the children (must not be null)
     * @param numberOfValues the number of children to be read in (< 0 = until end of section)
     * @param context
     * @return the key-value map containing the read-in children (not null, could be empty)
     * @throws ConversionException if not all children could be read-in
     */
    public static Map<String, Object> readValueMap(HierarchicalStreamReader reader, int numberOfValues,
            UnmarshallingContext context) throws ConversionException {
        Map<String, Object> valueMap = new HashMap<>((numberOfValues >= 0) ? numberOfValues : 10);
        int counter = 0;

        while (reader.hasMoreChildren() && ((counter < numberOfValues) || (numberOfValues == -1))) {
            reader.moveDown();
            if (reader.hasMoreChildren()) {
                List<?> list = (List<?>) context.convertAnother(context, List.class);
                valueMap.put(reader.getNodeName(), list);
            } else {
                valueMap.put(reader.getNodeName(), reader.getValue());
            }
            reader.moveUp();
            counter++;
        }

        if ((counter < numberOfValues) && (numberOfValues > 0)) {
            throw new ConversionException("Not all children could be read-in!");
        }

        return valueMap;
    }

    /**
     * Returns the object associated with the specified name of the child's node.
     *
     * @param nodeName the name of the child's node (must not be null)
     * @return the object associated with the specified name of the child's node (could be null)
     */
    public Object getObject(String nodeName) {
        return this.valueMap.get(nodeName);
    }

    /**
     * Returns the object associated with the specified name of the child's node.
     *
     * @param nodeName the name of the child's node (must not be null)
     * @param defaultValue the value to be returned if the node could not be found (could be null)
     * @return the object associated with the specified name of the child's node (could be null)
     */
    public Object getObject(String nodeName, Object defaultValue) {
        Object value = this.valueMap.get(nodeName);

        if (value != null) {
            return value;
        }

        return defaultValue;
    }

    /**
     * Returns the text associated with the specified name of the child's node.
     *
     * @param nodeName the name of the child's node (must not be null)
     * @return the text associated with the specified name of the child's node (could be null)
     */
    public String getString(String nodeName) {
        return getString(nodeName, null);
    }

    /**
     * Returns the text associated with the specified name of the child's node.
     *
     * @param nodeName the name of the child's node (must not be null)
     * @param defaultValue the text to be returned if the node could not be found (could be null)
     * @return the text associated with the specified name of the child's node (could be null)
     */
    public String getString(String nodeName, String defaultValue) {
        Object value = this.valueMap.get(nodeName);

        if (value instanceof String) {
            // fixes a formatting problem with line breaks in text
            return ((String) value).replaceAll("\\n\\s*", " ").trim();
        }

        return defaultValue;
    }

    /**
     * Returns the boolean associated with the specified name of the child's node.
     *
     * @param nodeName the name of the child's node (must not be null)
     * @return the boolean associated with the specified name of the child's node (could be null)
     */
    public Boolean getBoolean(String nodeName) {
        return getBoolean(nodeName, null);
    }

    /**
     * Returns the boolean associated with the specified name of the child's node.
     *
     * @param nodeName the name of the child's node (must not be null)
     * @param defaultValue the boolean to be returned if the node could not be found (could be null)
     * @return the boolean associated with the specified name of the child's node (could be null)
     */
    public Boolean getBoolean(String nodeName, Boolean defaultValue) {
        Object value = this.valueMap.get(nodeName);

        if (value != null) {
            return Boolean.parseBoolean(value.toString());
        }

        return defaultValue;
    }

    /**
     * Returns the numeric value associated with the specified name of the child's node.
     *
     * @param nodeName the name of the child's node (must not be null)
     * @return the numeric value associated with the specified name of the child's node
     *         (could be null)
     * @throws ConversionException if the value could not be converted to a numeric value
     */
    public Integer getInteger(String nodeName) throws ConversionException {
        return getInteger(nodeName, null);
    }

    /**
     * Returns the numeric value associated with the specified name of the child's node.
     *
     * @param nodeName the name of the child's node (must not be null)
     * @param defaultValue the numeric value to be returned if the node could not be found
     *            (could be null)
     * @return the numeric value associated with the specified name of the child's node
     *         (could be null)
     * @throws ConversionException if the value could not be converted to a numeric value
     */
    public Integer getInteger(String nodeName, Integer defaultValue) throws ConversionException {
        Object value = this.valueMap.get(nodeName);

        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException nfe) {
                throw new ConversionException("The value '" + value + "' cannot be converted to a numeric value!", nfe);
            }
        }

        return defaultValue;
    }

}
