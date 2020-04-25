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
package org.openhab.core.thing.xml.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.xml.util.ConverterAttributeMapValidator;
import org.openhab.core.config.xml.util.GenericUnmarshaller;
import org.openhab.core.config.xml.util.NodeIterator;
import org.openhab.core.thing.type.AbstractDescriptionType;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link AbstractDescriptionTypeConverter} is an abstract implementation of the {@code XStream} {@link Converter}
 * interface used as helper class to convert type
 * definition information within an XML document into a concrete type object.
 * <p>
 * This class should be used for each type definition which inherits from the {@link AbstractDescriptionType} class.
 *
 * @param <T> the result type of the conversion
 *
 * @author Michael Grammling - Initial contribution
 */
public abstract class AbstractDescriptionTypeConverter<T> extends GenericUnmarshaller<T> {

    protected ConverterAttributeMapValidator attributeMapValidator;

    private final String type;

    /**
     * Creates a new instance of this class with the specified parameter.
     *
     * @param clazz the class of the result type (must not be null)
     * @param type the name of the type (e.g. "thing-type", "channel-type")
     */
    public AbstractDescriptionTypeConverter(Class<T> clazz, String type) {
        super(clazz);

        this.type = type;

        this.attributeMapValidator = new ConverterAttributeMapValidator(new String[][] { { "id", "true" } });
    }

    /**
     * Returns the {@code id} attribute of the specific XML type definition.
     *
     * @param attributes the attributes where to extract the ID information (must not be null)
     * @return the ID of the type definition (neither null, nor empty)
     */
    protected String getID(Map<String, String> attributes) {
        return attributes.get("id");
    }

    /**
     * Returns the full extracted UID of the specific XML type definition.
     *
     * @param attributes the attributes where to extract the ID information (must not be null)
     * @param context the context where to extract the binding ID information (must not be null)
     *
     * @return the UID of the type definition (neither null, nor empty)
     */
    protected String getUID(Map<String, String> attributes, UnmarshallingContext context) {
        String bindingId = (String) context.get("thing-descriptions.bindingId");
        String typeId = getID(attributes);

        String uid = String.format("%s:%s", bindingId, typeId);

        return uid;
    }

    /**
     * Returns the value of the {@code label} tag from the specific XML type definition.
     *
     * @param nodeIterator the iterator to be used to extract the information (must not be null)
     * @return the value of the label (neither null, nor empty)
     * @throws ConversionException if the label could not be read
     */
    protected String readLabel(NodeIterator nodeIterator) throws ConversionException {
        return (String) nodeIterator.nextValue("label", true);
    }

    /**
     * Returns the value of the {@code description} tag from the specific XML type definition.
     *
     * @param nodeIterator the iterator to be used to extract the information (must not be null)
     * @return the value of the description (could be null or empty)
     */
    protected String readDescription(NodeIterator nodeIterator) {
        return (String) nodeIterator.nextValue("description", false);
    }

    private URI readConfigDescriptionURI(NodeIterator nodeIterator) throws ConversionException {
        String uriText = nodeIterator.nextAttribute("config-description-ref", "uri", false);

        if (uriText != null) {
            try {
                return new URI(uriText);
            } catch (URISyntaxException ex) {
                throw new ConversionException(
                        "The URI '" + uriText + "' in node " + "'config-description-ref' is invalid!", ex);
            }
        }

        return null;
    }

    private ConfigDescription readConfigDescription(NodeIterator nodeIterator) {
        Object nextNode = nodeIterator.next();

        if (nextNode != null) {
            if (nextNode instanceof ConfigDescription) {
                return (ConfigDescription) nextNode;
            }

            nodeIterator.revert();
        }

        return null;
    }

    /**
     * Returns the value of the {@code config-description-ref} and the {@code config-description} tags from the specific
     * XML type definition.
     *
     * @param nodeIterator the iterator to be used to extract the information (must not be null)
     *
     * @return the URI and configuration object
     *         (contains two elements: URI - could be null, ConfigDescription - could be null)
     */
    protected Object[] getConfigDescriptionObjects(NodeIterator nodeIterator) {
        URI configDescriptionURI = readConfigDescriptionURI(nodeIterator);
        ConfigDescription configDescription = null;
        if (configDescriptionURI == null) {
            configDescription = readConfigDescription(nodeIterator);
            if (configDescription != null) {
                configDescriptionURI = configDescription.getUID();
            }
        }

        return new Object[] { configDescriptionURI, configDescription };
    }

    /**
     * The abstract unmarshal method which must be implemented by the according type converter.
     *
     * @param reader the reader to be used to read XML information from a stream (not null)
     *
     * @param context the context to be used for the XML document conversion (not null)
     *
     * @param attributes the attributes map containing attributes of the type - only UID -
     *            (not null, could be empty)
     *
     * @param nodeIterator the iterator to be used to simply extract information in the right
     *            order and appearance from the XML stream
     *
     * @return the concrete type definition object (could be null)
     *
     * @throws ConversionException if any conversion error occurs
     */
    protected abstract T unmarshalType(HierarchicalStreamReader reader, UnmarshallingContext context,
            Map<String, String> attributes, NodeIterator nodeIterator) throws ConversionException;

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        // read attributes
        Map<String, String> attributes = this.attributeMapValidator.readValidatedAttributes(reader);

        // set automatically extracted URI for a possible 'config-description' section
        context.put("config-description.uri", this.type + ":" + getUID(attributes, context));

        // read values
        List<?> nodes = (List<?>) context.convertAnother(context, List.class);
        NodeIterator nodeIterator = new NodeIterator(nodes);

        // create object
        Object object = unmarshalType(reader, context, attributes, nodeIterator);

        nodeIterator.assertEndOfType();

        return object;
    }
}
