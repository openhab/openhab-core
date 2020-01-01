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
package org.openhab.core.automation.type;

import java.util.Collections;
import java.util.Set;

import org.openhab.core.automation.Module;
import org.openhab.core.automation.Rule;

/**
 * This class defines meta-information properties, used by the Rule Engine when creating connections between
 * modules.
 * The {@link Input}s of a {@link Module} are the entry points for data coming from other {@link Module}s. An
 * {@link Input} can be connected to a single {@link Output} of another {@link Module}, which produces data of an
 * acceptable type.
 * <p>
 * The meta-information contains:
 * <ul>
 * <li>name (mandatory) - unique ID in the scope of the {@link Module}</li>
 * <li>type (mandatory) - the acceptable data type for this {@link Input}. The value of this meta-info property could be
 * any string that makes sense for the user, like a fully qualified name of a Java class, or a general description like
 * "temperature", for example. The value "*" means that all possible types are acceptable. The type is used to determine
 * which {@link Output} can be connected to this {@link Input}.</li>
 * <li>label (optional) - short description (one word) of the {@link Input}</li>
 * <li>description (optional) - long user friendly description of the {@link Input}</li>
 * <li>default value (optional) - the string representation of the default value of the {@link Input}. It must be
 * compatible with the type of the {@link Input}</li>
 * <li>required (optional) - defines if the {@link Input} is required or optional. The default value is false.</li>
 * </ul>
 *
 * @author Yordan Mihaylov - Initial contribution
 */
public class Input {

    /**
     * Specifies a unique name of the {@code Input} in scope of the {@link Module}.
     */
    private String name;

    /**
     * Specifies the acceptable data type for this {@link Input}. The value could be any string that makes sense for the
     * user, like a fully qualified name of a Java class, or a general description like "temperature", for example. The
     * value "*" means that all possible types are acceptable. The type is used to determine which {@link Output} can be
     * connected to this {@link Input} instance.
     */
    private String type;

    /**
     * Keeps a single word description of the {@code Input}.
     */
    private String label;

    /**
     * Keeps the user friendly description of the {@code Input}.
     */
    private String description;

    /**
     * Determines if the {@code Input} is required or optional.
     */
    private boolean required = false;
    private Set<String> tags;
    private String reference;
    private String defaultValue;

    /**
     * Default constructor for deserialization e.g. by Gson.
     */
    protected Input() {
    }

    /**
     * Creates an {@code Input} instance base on type of accepted data and input name. These are required information
     * and can't be null.
     *
     * @param name the unique name of the {@link Input} in scope of the module.
     * @param type the acceptable data type for this {@link Input}.
     * @throws IllegalArgumentException If one of the name or type parameters is null.
     */
    public Input(String name, String type) {
        this(name, type, null, null, null, false, null, null);
    }

    /**
     * Creates an {@code Input} instance base on the specified parameters. Type of accepted data and input name are
     * required information and can't be null.
     *
     * @param name unique name of the {@code Input} in scope of the module.
     * @param type the acceptable data type for this {@link Input}.
     * @param label a single word description of the {@code Input}.
     * @param description user friendly description of the {@code Input}.
     * @param tags are associated with the {@code Input}. The tags adds additional restrictions to connections
     *            between {@code Input}s and {@link Output}s. The input tags must be subset of the output tags
     *            to succeed the connection.
     *            For example: When we want to connect input to output and both have same java.lang.double data
     *            type. The the output has assign "temperature" and "celsius" tags then the input must have at
     *            least one of these output's tags (i.e. "temperature") to connect this {@code Input} to the
     *            selected output.
     * @param required determines if the {@code Input} is required or optional.
     * @param reference refers to the input of parent module type or null. If this input is part of the system module
     *            the reference should be null.
     * @param defaultValue default value takes place when there is no value for this Input. Type of the default value
     *            must be the type the Input.
     * @throws IllegalArgumentException If one of the name or type parameters is null.
     */
    public Input(String name, String type, String label, String description, Set<String> tags, boolean required,
            String reference, String defaultValue) {
        if (name == null) {
            throw new IllegalArgumentException("The name of the input must not be NULL!");
        }
        if (type == null) {
            throw new IllegalArgumentException("The type of the input must not be NULL!");
        }
        this.name = name;
        this.type = type;
        this.label = label;
        this.description = description;
        this.tags = tags;
        this.required = required;
        this.reference = reference;
        this.defaultValue = defaultValue;
    }

    /**
     * Gets the name of Input. It must be unique in scope of the {@link Rule}.
     *
     * @return name is an unique identifier of the Input.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the short description of the Input. Usually the label should be a single word description.
     *
     * @return label of the Input.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Gets the long description of the Input.
     *
     * @return user friendly description of the Input.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Determines if the Input is required or optional.
     *
     * @return true when required, false otherwise.
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Gets the type of the {@link Input}. The returned value can be any string that makes sense for the user, like a
     * fully qualified name of a Java class, or a general description like "temperature", for example. The value "*"
     * means that all possible types are acceptable.
     *
     * @return the type of the {@link Input}.
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the tags of the Input. The tags add additional restrictions to connections between {@link Input}s and
     * {@link Output}s. The input tags must be subset of the output tags to succeed the connection.<br>
     * For example: When we want to connect input to output and they both have same java.lang.double data type, and the
     * output has assign "temperature" and "celsius" tags then the input must have at least one of these output's tags
     * (i.e. "temperature") to connect this input to the selected output.
     *
     * @return tags associated with this Input.
     */
    public Set<String> getTags() {
        return tags != null ? tags : Collections.<String> emptySet();
    }

    /**
     * Gets the reference to data source. It is used to link custom inputs (inputs of custom module type) to system
     * input (defined by the system module type. The system module type uses only system inputs).
     *
     * @return reference to data source.
     */
    public String getReference() {
        return reference;
    }

    /**
     * Gets the string representation of the default value of the {@link Input}. It is used when there is no runtime
     * value for this {@link Input}. It must be compatible with the type of the {@link Input}.
     *
     * @return the string representation of the default value of this {@link Input}.
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String toString() {
        return "Input " + name;
    }
}
