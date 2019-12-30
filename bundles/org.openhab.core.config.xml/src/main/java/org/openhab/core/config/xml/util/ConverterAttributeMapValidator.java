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
import java.util.Map;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link ConverterAttributeMapValidator} class reads any attributes of a node, validates
 * if they appear or not, and returns the validated key-value pair map.
 *
 * @author Michael Grammling - Initial contribution
 */
public class ConverterAttributeMapValidator {

    private Map<String, Boolean> validationMaskTemplate;

    /**
     * Creates a new instance of this class with the specified parameter.
     * <p>
     * The validation mask template is a two-dimensional key-required ({@code String, [true|false]}) list, defining all
     * attributes the node could have and which are required and which not. The structure of the array is the following:
     * <br>
     * <code><pre>
     * String[] validationMaskTemplate = new String[][] {
     *         { "uri", "false" },
     *         { "attribute", "true" }};
     * </pre></code>
     * <p>
     * If the validation mask template is {@code null} the validation is skipped. If it's empty, no attributes are
     * allowed for this node.
     *
     * @param validationMaskTemplate the two-dimensional key-required list (could be null or empty)
     */
    public ConverterAttributeMapValidator(String[][] validationMaskTemplate) {
        if (validationMaskTemplate != null) {
            this.validationMaskTemplate = new HashMap<>(validationMaskTemplate.length);

            for (String[] validationProperty : validationMaskTemplate) {
                this.validationMaskTemplate.put(validationProperty[0], Boolean.parseBoolean(validationProperty[1]));
            }
        }
    }

    /**
     * Creates a new instance of this class with the specified parameter.
     * <p>
     * The validation mask template is a key-required ({@code String, [true|false]}) map, defining all attributes the
     * node could have, and which are required and which not.
     * <p>
     * If the validation mask template is {@code null} the validation is skipped. If it's empty, no attributes are
     * allowed for this node.
     *
     * @param validationMaskTemplate the key-required map (could be null or empty)
     */
    public ConverterAttributeMapValidator(Map<String, Boolean> validationMaskTemplate) {
        this.validationMaskTemplate = validationMaskTemplate;
    }

    /**
     * Reads, validates and returns all attributes of a node associated with the specified
     * reader as key-value map.
     *
     * @param reader the reader to be used to read-in all attributes of the node (must not be null)
     * @return the key-value map (not null, could be empty)
     * @throws ConversionException if the validation check fails
     */
    public Map<String, String> readValidatedAttributes(HierarchicalStreamReader reader) throws ConversionException {
        return readValidatedAttributes(reader, this.validationMaskTemplate);
    }

    /**
     * Reads, validates and returns all attributes of a node associated with the specified
     * reader as key-value map.
     * <p>
     * The validation mask template is a key-required ({@code String, [true|false]}) map, defining all attributes the
     * node could have, and which are required and which not.
     *
     * @param reader the reader to be used to read-in all attributes of the node (must not be null)
     * @param validationMaskTemplate the key-required map (could be null or empty)
     * @return the key-value map (not null, could be empty)
     * @throws ConversionException if the validation check fails
     */
    public static Map<String, String> readValidatedAttributes(HierarchicalStreamReader reader,
            Map<String, Boolean> validationMaskTemplate) throws ConversionException {
        Map<String, String> attributeMap = new HashMap<>(reader.getAttributeCount());

        Map<String, Boolean> validationMask = null;
        if (validationMaskTemplate != null) {
            // create a new one, because entries are removed during validation
            validationMask = new HashMap<>(validationMaskTemplate);
        }

        for (int index = 0; index < reader.getAttributeCount(); index++) {
            String attributeName = reader.getAttributeName(index);

            if ((validationMask == null) || validationMask.containsKey(attributeName)) {
                attributeMap.put(attributeName, reader.getAttribute(index));

                if (validationMask != null) {
                    validationMask.remove(attributeName); // no duplicates are allowed
                }
            } else {
                throw new ConversionException("The attribute '" + attributeName + "' of the node '"
                        + reader.getNodeName() + "' is not supported or exists multiple times!");
            }
        }

        // there are still attributes in the validation mask left -> check if they are required
        if (validationMask != null && !validationMask.isEmpty()) {
            for (Map.Entry<String, Boolean> entry : validationMask.entrySet()) {
                String attributeName = entry.getKey();
                boolean attributeRequired = entry.getValue();

                if (attributeRequired) {
                    throw new ConversionException("The attribute '" + attributeName + "' of the node '"
                            + reader.getNodeName() + "' is missing!");
                }
            }
        }

        return attributeMap;
    }

}
