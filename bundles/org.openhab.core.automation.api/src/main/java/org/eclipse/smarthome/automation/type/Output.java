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
package org.eclipse.smarthome.automation.type;

import java.util.Collections;
import java.util.Set;

import org.eclipse.smarthome.automation.Module;
import org.eclipse.smarthome.automation.Rule;

/**
 * This class defines meta-information properties, used by the Rule Engine when creating connections between
 * modules.
 * {@link Output}s are the exit point of a {@link Module}. They are used as data source for {@link Input}s of other
 * {@link Module}s.
 * <p>
 * The meta-information contains:
 * <ul>
 * <li>name (mandatory) - unique ID in the scope of the {@link Module}</li>
 * <li>type (mandatory) - the type of the output data. The value of this meta-info property could be any string that
 * makes sense for the user, like a fully qualified name of a Java class, or a general description like "temperature",
 * for example. The type is used to determine which {@link Input} can be connected to this {@link Output}</li>
 * <li>label (optional) - short id (one word) of the Output.</li>
 * <li>description (optional) - user friendly description of the {@link Output}</li>
 * <li>default value (optional) - the string representation of the default value of the {@link Output}.</li>
 * <li>reference (optional) - reference to data source. It defines what part of complex data (i.e. JavaBean,
 * java.lang.Map etc.) has to be used as value of this output.
 * </ul>
 * <p>
 * An {@link Output} can be connected to more than one {@link Input} with a compatible data type.<br>
 *
 * @author Yordan Mihaylov - Initial Contribution
 * @author Ana Dimova - Initial Contribution
 * @author Vasil Ilchev - Initial Contribution
 */
public class Output {

    /**
     * is an unique name of the {@code Output} in scope of the {@link Module}.
     */
    private String name;

    /**
     * This field specifies the type of the output data. The value could be any string that makes sense for the user,
     * like a fully qualified name of a Java class, or a general description like "temperature", for example. The type
     * is used to determine which {@link Input} can be connected to this {@link Output}.
     */
    private String type;

    /**
     * This field is associated with the {@code Output}. The tags add additional restrictions to connections between
     * {@link Input}s and {@link Output}s. The {@link Input}'s tags must be subset of the {@code Output}'s tags to
     * succeed the connection.</br>
     * For example: When we want to connect {@link Input} to {@code Output} and both have same java.lang.double data
     * type. The the output has assign "temperature" and "celsius" tags then the input must have at least one of these
     * output's tags (i.e. "temperature") to connect this input to the selected {@code Output}.
     */
    private Set<String> tags;

    /**
     * This field keeps a single word description of the {@code Output}.
     */
    private String label;

    /**
     * This field keeps the user friendly description of the {@code Output}.
     */
    private String description;

    /**
     * The value of this field refers to the data source. It defines what part of complex data should be used as source
     * of this {@code Output}.
     */
    private String reference;

    /**
     * The string representation of the default value of the {@link Output}. The value of this field is used when there
     * is no runtime value for this {@code Output}. It must be compatible with the type of the {@link Output}.
     */
    private String defaultValue;

    /**
     * Default constructor for deserialization e.g. by Gson.
     */
    protected Output() {
    }

    /**
     * Constructs an {@code Output} instance with the specified name and output data type.
     *
     * @param name a unique name of the {@code Output}.
     * @param type the type of the output data.
     * @throws IllegalArgumentException If one of the name or type parameters is null.
     */
    public Output(String name, String type) {
        this(name, type, null, null, null, null, null);
    }

    /**
     * Constructs an {@code Output} instance with the specified parameters.
     *
     * @param name         a unique name of the {@code Output}.
     * @param type         the type of the output data.
     * @param label        a single word description of the {@code Output}.
     * @param description  is an user friendly description of the {@code Output}.
     * @param tags         are associated with the {@code Output}. The tags add additional restrictions to connections
     *                     between {@link Input}s and {@link Output}s. The {@link Input}'s tags must be subset of the
     *                     {@code Output}'s tags to succeed the connection.<br>
     *                     For example: When we want to connect {@link Input} to
     *                     {@code Output} and both have same java.lang.double data type. The the output has assign
     *                     "temperature" and "celsius" tags then the input must have at least one of these output's tags
     *                     (i.e. "temperature") to connect this input to the selected {@code Output}.
     * @param reference    refers to the data source. It defines what part of complex data should be used as source of
     *                     this {@code Output}.
     * @param defaultValue takes place when there is no runtime value for this {@code Output}. Type of the default value
     *                     must be the type of the {@code Output}.
     * @throws IllegalArgumentException If one of the name or type parameters is null.
     */
    public Output(String name, String type, String label, String description, Set<String> tags, String reference,
            String defaultValue) {
        if (name == null) {
            throw new IllegalArgumentException("The name of the output must not be NULL!");
        }
        if (type == null) {
            throw new IllegalArgumentException("The type of the output must not be NULL!");
        }
        this.name = name;
        this.type = type;
        this.label = label;
        this.description = description;
        this.tags = tags;
        this.reference = reference;
        this.defaultValue = defaultValue;
    }

    /**
     * This method is used for getting the name of {@code Output}. It must be unique in
     * scope of {@link Rule}.
     *
     * @return name is an unique identifier of the {@code Output}.
     */
    public String getName() {
        return name;
    }

    /**
     * This method is used to get the type of the {@code Output}. It can be any string that makes sense for the user,
     * like a fully qualified name of a Java class, or a general description like "temperature", for example.
     *
     * @return the type of the output data.
     */
    public String getType() {
        return type;
    }

    /**
     * This method is used for getting the short description of the {@code Output}.
     * Usually the label should be a single word description.
     *
     * @return label of the Output.
     */
    public String getLabel() {
        return label;
    }

    /**
     * This method is used for getting the long description of the {@code Output}.
     *
     * @return user friendly description of the {@code Output}.
     */
    public String getDescription() {
        return description;
    }

    /**
     * This method is used for getting the reference to data source. It defines what part of complex data (i.e.
     * JavaBean, java.lang.Map etc.) has to be used as a value of this {@code Output}. For example, in the
     * {@code Output} data - java.lang.Map, the reference points to the property that has to be used as an output value.
     *
     * @return a reference to data source.
     */
    public String getReference() {
        return reference;
    }

    /**
     * This method is used for getting the tags of the {@code Output}. The tags add additional restrictions to
     * connections between {@link Input}s and {@code Output}s. The input tags must be subset of the output tags to
     * succeed the connection.<br>
     * For example: When we want to connect {@link Input} to {@code Output} and they both have same data type -
     * java.lang.double and the {@link Output} has assign "temperature" and "celsius" tags, then the {@link Input} must
     * have at least one of these {@code Output}'s tags (i.e. "temperature") to connect this {@link Input} to the
     * selected {@code Output}.
     *
     * @return the tags, associated with this {@link Input}.
     */
    public Set<String> getTags() {
        return tags != null ? tags : Collections.<String> emptySet();
    }

    /**
     * This method is used to get the string representation of the default value of the {@code Output}. It is used when
     * there is no runtime value for this {@code Output}. It must be compatible with the type of the {@link Output}.
     *
     * @return the string representation of the default value of this {@code Output}.
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Output " + name;
    }
}
