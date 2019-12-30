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
package org.openhab.core.config.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link ConfigDescriptionParameter} class contains the description of a
 * concrete configuration parameter. Such parameter descriptions are collected
 * within the {@link ConfigDescription} and can be retrieved from the {@link ConfigDescriptionRegistry}.
 *
 * This class defines available configuration parameter types in {@link ConfigDescriptionParameter#Type},
 * it defines available unit types in {@link ConfigDescriptionParameter#UNITS} and available
 * contexts (see {@link ConfigDescriptionParameter#getContext()}).
 *
 * @author Michael Grammling - Initial contribution
 * @author Alex Tugarev - Added options, filter criteria, and more parameter
 *         attributes
 * @author Chris Jackson - Added groupId, limitToOptions, advanced,
 *         multipleLimit, verify attributes
 * @author Christoph Knauf - Added default constructor, changed Boolean
 *         getter to return primitive types
 * @author Thomas Höfer - Added unit
 */
public class ConfigDescriptionParameter {

    /**
     * The {@link Type} defines an enumeration of all supported data types a
     * configuration parameter can take.
     *
     * @author Michael Grammling - Initial contribution
     */
    public enum Type {

        /**
         * The data type for a UTF8 text value.
         */
        TEXT,

        /**
         * The data type for a signed integer value in the range of [ {@link Integer#MIN_VALUE},
         * {@link Integer#MAX_VALUE}].
         */
        INTEGER,

        /**
         * The data type for a signed floating point value (IEEE 754) in the
         * range of [{@link Float#MIN_VALUE}, {@link Float#MAX_VALUE}].
         */
        DECIMAL,

        /**
         * The data type for a boolean ({@code true} or {@code false}).
         */
        BOOLEAN;

    }

    private String name;
    private Type type;

    private String groupName;

    private BigDecimal min;
    private BigDecimal max;
    private BigDecimal step;
    private String pattern;
    private boolean required = false;
    private boolean readOnly = false;
    private boolean multiple = false;
    private Integer multipleLimit;
    private String unit;
    private String unitLabel;

    private String context;
    @SerializedName("default")
    private String defaultValue;
    private String label;
    private String description;

    private List<ParameterOption> options = new ArrayList<>();
    private List<FilterCriteria> filterCriteria = new ArrayList<>();

    private boolean limitToOptions = true;
    private boolean advanced = false;
    private boolean verify = false;

    private static final Set<String> UNITS = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("A", "cd", "K", "kg", "m", "mol", "s", "g", "rad", "sr", "Hz",
                    "N", "Pa", "J", "W", "C", "V", "F", "Ω", "S", "Wb", "T", "H", "Cel", "lm", "lx", "Bq", "Gy", "Sv",
                    "kat", "m/s2", "m2v", "m3", "kph", "%", "l", "ms", "min", "h", "d", "week", "y")));

    /**
     * Default constructor.
     *
     */
    public ConfigDescriptionParameter() {
    }

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param name the name of the configuration parameter (must neither be null
     *            nor empty)
     * @param type the data type of the configuration parameter (must not be
     *            null)
     * @throws IllegalArgumentException if the name is null or empty, or the type is null
     */
    public ConfigDescriptionParameter(String name, Type type) throws IllegalArgumentException {
        this(name, type, null, null, null, null, false, false, false, null, null, null, null, null, null, null, false,
                true, null, null, null, false);
    }

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param name the name of the configuration parameter (must neither be null
     *            nor empty)
     * @param type the data type of the configuration parameter (nullable)
     * @param minimum the minimal value for numeric types, or the minimal length of
     *            strings, or the minimal number of selected options (nullable)
     * @param maximum the maximal value for numeric types, or the maximal length of
     *            strings, or the maximal number of selected options (nullable)
     * @param stepsize the value granularity for a numeric value (nullable)
     * @param pattern the regular expression for a text type (nullable)
     * @param required specifies whether the value is required
     * @param readOnly specifies whether the value is read-only
     * @param multiple specifies whether multiple selections of options are allowed
     * @param context the context of the configuration parameter (can be null or
     *            empty)
     * @param defaultValue the default value of the configuration parameter (can be null)
     * @param label a human readable label for the configuration parameter (can be
     *            null or empty)
     * @param description a human readable description for the configuration parameter
     *            (can be null or empty)
     * @param filterCriteria a list of filter criteria for values of a dynamic selection
     *            list (nullable)
     * @param options a list of element definitions of a static selection list
     *            (nullable)
     * @param groupName a string used to group parameters together into logical blocks
     *            so that the UI can display them together
     * @param advanced specifies if this is an advanced parameter. An advanced
     *            parameter can be hidden in the UI to focus the user on
     *            important configuration
     * @param limitToOptions specifies that the users input is limited to the options list.
     *            When set to true without options, this should have no affect.
     *            When set to true with options, the user can only select the
     *            options from the list When set to false with options, the user
     *            can enter values other than those in the list
     * @param multipleLimit specifies the maximum number of options that can be selected
     *            when multiple is true
     * @param unit specifies the unit of measurements for the configuration parameter (nullable)
     * @param unitLabel specifies the unit label for the configuration parameter. This attribute can also be used to
     *            provide
     *            natural language units as iterations, runs, etc.
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>if the name is null or empty, or the type is null</li>
     *             <li>if a unit or a unit label is provided for a parameter having type text or boolean</li>
     *             <li>if an invalid unit was given (cp.
     *             https://openhab.org/documentation/development/bindings/xml-reference.html for the list
     *             of valid units)</li>
     *             </ul>
     */
    ConfigDescriptionParameter(String name, Type type, BigDecimal minimum, BigDecimal maximum, BigDecimal stepsize,
            String pattern, Boolean required, Boolean readOnly, Boolean multiple, String context, String defaultValue,
            String label, String description, List<ParameterOption> options, List<FilterCriteria> filterCriteria,
            String groupName, Boolean advanced, Boolean limitToOptions, Integer multipleLimit, String unit,
            String unitLabel, Boolean verify) throws IllegalArgumentException {
        if ((name == null) || (name.isEmpty())) {
            throw new IllegalArgumentException("The name must neither be null nor empty!");
        }

        if (type == null) {
            throw new IllegalArgumentException("The type must not be null!");
        }

        if ((type == Type.TEXT || type == Type.BOOLEAN) && (unit != null || unitLabel != null)) {
            throw new IllegalArgumentException(
                    "Unit or unit label must only be set for integer or decimal configuration parameters");
        }
        if (unit != null && !UNITS.contains(unit)) {
            throw new IllegalArgumentException("The given unit is invalid.");
        }

        this.name = name;
        this.type = type;
        this.groupName = groupName;
        this.min = minimum;
        this.max = maximum;
        this.step = stepsize;
        this.pattern = pattern;
        this.context = context;
        this.defaultValue = defaultValue;
        this.label = label;
        this.description = description;
        this.multipleLimit = multipleLimit;
        this.unit = unit;
        this.unitLabel = unitLabel;

        if (verify != null) {
            this.verify = verify;
        }
        if (readOnly != null) {
            this.readOnly = readOnly;
        }
        if (multiple != null) {
            this.multiple = multiple;
        }
        if (advanced != null) {
            this.advanced = advanced;
        }
        if (required != null) {
            this.required = required;
        }
        if (limitToOptions != null) {
            this.limitToOptions = limitToOptions;
        }

        if (options != null) {
            this.options = Collections.unmodifiableList(options);
        } else {
            this.options = Collections.unmodifiableList(new LinkedList<>());
        }
        if (filterCriteria != null) {
            this.filterCriteria = Collections.unmodifiableList(filterCriteria);
        } else {
            this.filterCriteria = Collections.unmodifiableList(new LinkedList<>());
        }
    }

    /**
     * Returns the name of the configuration parameter.
     *
     * @return the name of the configuration parameter (neither null, nor empty)
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the data type of the configuration parameter.
     *
     * @return the data type of the configuration parameter (not null)
     */
    public Type getType() {
        return this.type;
    }

    /**
     * @return the minimal value for numeric types, or the minimal length of
     *         strings, or the minimal number of selected options (nullable)
     */
    public BigDecimal getMinimum() {
        return min;
    }

    /**
     * @return the maximal value for numeric types, or the maximal length of
     *         strings, or the maximal number of selected options (nullable)
     */
    public BigDecimal getMaximum() {
        return max;
    }

    /**
     * @return the value granularity for a numeric value (nullable)
     */
    public BigDecimal getStepSize() {
        return step;
    }

    /**
     * @return the regular expression for a text type (nullable)
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * @return true if the value is required, otherwise false.
     */
    public Boolean isReadOnly() {
        return readOnly;
    }

    /**
     * @return true if multiple selections of options are allowed, otherwise
     *         false.
     */
    public Boolean isMultiple() {
        return multiple;
    }

    /**
     * @return the maximum number of options that can be selected from the options list
     */
    public Integer getMultipleLimit() {
        return multipleLimit;
    }

    /**
     * Returns the context of the configuration parameter.
     * <p>
     * A context is a hint for user interfaces and input validators.
     * <p>
     * <p>
     * Any string can be used, but the following have a special meaning:
     * </p>
     *
     * - network-address: The configuration value represents an IPv4 or IPv6 address.
     * - password: A password value (a user-interface might obscure the visible value)
     * - password-create: A passwort generator widget might be shown
     * - color: This value represents an RGB color value like #ffffff or 12,12,12.
     * - date: A date string
     * - time: A time string
     * - cronexpression: A cron expression like "* * * * *". A user interface would probably show a cron expression
     * generator.
     * - datetime: A date and time string
     * - email: The configuration value represents an email address
     * - month: A number [1-12]
     * - week: A week [0-52]
     * - tel: A tel no
     * - url: A web address
     * - script: The configuration value represents a script (javascript, python etc). A user-interface would probably
     * render a multi line editor.
     * - location: A lat,long,alt GPS location. A user-interface would probably render a world map for selection.
     * - tag: One tag or multiple tags separated by comma.
     * - item: A valid item "name". A user-interface would probably show an item selection widget.
     * - thing: A valid thing UID. A user-interface would probably show a thing selection widget.
     * - channel: A valid channel UID.
     * - channeltype: A valid channel type UID. A user-interface would probably show a channel type selection widget.
     * - group: A valid group item "name". A user-interface would probably show an item selection widget.
     * - service: A valid service ID. A user-interface would probably show a service selection widget.
     * - rule: A valid rule uid. A user-interface would probably show a rule selection widget.
     *
     * @return the context of the configuration parameter (could be null or
     *         empty)
     */
    public String getContext() {
        return this.context;
    }

    /**
     * Returns {@code true} if the configuration parameter has to be set,
     * otherwise {@code false}.
     *
     * @return true if the configuration parameter has to be set, otherwise
     *         false
     */
    public boolean isRequired() {
        return this.required;
    }

    /**
     * Returns the default value of the configuration parameter.
     *
     * @return the default value of the configuration parameter (could be null)
     */
    public String getDefault() {
        return this.defaultValue;
    }

    /**
     * Returns a human readable label for the configuration parameter.
     *
     * @return a human readable label for the configuration parameter (could be
     *         null or empty)
     */
    public String getLabel() {
        return this.label;
    }

    /**
     * Returns a the group for this configuration parameter.
     *
     * @return a group for the configuration parameter (could be null or empty)
     */
    public String getGroupName() {
        return this.groupName;
    }

    /**
     * Returns true is the value for this parameter must be limited to the
     * values in the options list.
     *
     * @return true if the value is limited to the options list
     */
    public boolean getLimitToOptions() {
        return this.limitToOptions;
    }

    /**
     * Returns true is the parameter is considered an advanced option.
     *
     * @return true if the value is an advanced option
     */
    public boolean isAdvanced() {
        return this.advanced;
    }

    /**
     * Returns a human readable description for the configuration parameter.
     *
     * @return a human readable description for the configuration parameter
     *         (could be null or empty)
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Returns a static selection list for the value of this parameter.
     *
     * @return static selection list for the value of this parameter
     */
    public List<ParameterOption> getOptions() {
        return this.options;
    }

    /**
     * Returns a list of filter criteria for a dynamically created selection
     * list.
     * <p>
     * The clients should consider the relation between the filter criteria and the parameter's context.
     *
     * @return list of filter criteria for a dynamically created selection list
     */
    public List<FilterCriteria> getFilterCriteria() {
        return this.filterCriteria;
    }

    /**
     * Returns the unit of measurements of this parameter.
     *
     * @return the unit of measurements of this parameter (could be null)
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Returns the unit label of this parameter.
     *
     * @return the unit label of this parameter (could be null)
     */
    public String getUnitLabel() {
        return unitLabel;
    }

    /**
     * Returns the verify flag for this parameter. Verify parameters are considered dangerous and the user should be
     * alerted with an "Are you sure" flag in the UI.
     *
     * @return true if the parameter requires verification in the UI
     */
    public Boolean isVerifyable() {
        return verify;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName());
        sb.append(" [name=");
        sb.append(name);
        sb.append(", ");
        sb.append("type=");
        sb.append(type);
        if (groupName != null) {
            sb.append(", ");
            sb.append("groupName=");
            sb.append(groupName);
        }
        if (min != null) {
            sb.append(", ");
            sb.append("min=");
            sb.append(min);
        }
        if (max != null) {
            sb.append(", ");
            sb.append("max=");
            sb.append(max);
        }
        if (step != null) {
            sb.append(", ");
            sb.append("step=");
            sb.append(step);
        }
        if (pattern != null) {
            sb.append(", ");
            sb.append("pattern=");
            sb.append(pattern);
        }
        sb.append(", ");
        sb.append("readOnly=");
        sb.append(readOnly);

        sb.append(", ");
        sb.append("required=");
        sb.append(required);

        sb.append(", ");
        sb.append("verify=");
        sb.append(verify);

        sb.append(", ");
        sb.append("multiple=");
        sb.append(multiple);
        sb.append(", ");
        sb.append("multipleLimit=");
        sb.append(multipleLimit);
        if (context != null) {
            sb.append(", ");
            sb.append("context=");
            sb.append(context);
        }
        if (label != null) {
            sb.append(", ");
            sb.append("label=");
            sb.append(label);
        }
        if (description != null) {
            sb.append(", ");
            sb.append("description=");
            sb.append(description);
        }
        if (defaultValue != null) {
            sb.append(", ");
            sb.append("defaultValue=");
            sb.append(defaultValue);
        }
        if (unit != null) {
            sb.append(", ");
            sb.append("unit=");
            sb.append(unit);
        }
        if (unitLabel != null) {
            sb.append(", ");
            sb.append("unitLabel=");
            sb.append(unitLabel);
        }
        sb.append("]");
        return sb.toString();
    }
}
