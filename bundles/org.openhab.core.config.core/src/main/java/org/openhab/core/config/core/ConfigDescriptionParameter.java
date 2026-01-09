/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link ConfigDescriptionParameter} class contains the description of a
 * concrete configuration parameter. Such parameter descriptions are collected
 * within the {@link ConfigDescription} and can be retrieved from the {@link ConfigDescriptionRegistry}.
 *
 * This class defines available configuration parameter types in {@link ConfigDescriptionParameter.Type},
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
 * @author Laurent Garnier - Removed constraint on unit value
 * @author Florian Hotze - Add null annotations
 */
@NonNullByDefault
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
        BOOLEAN

    }

    private @NonNullByDefault({}) String name;
    private @NonNullByDefault({}) Type type;

    private @Nullable String groupName;

    private @Nullable BigDecimal min;
    private @Nullable BigDecimal max;
    private @Nullable BigDecimal step;
    private @Nullable String pattern;
    private boolean required = false;
    private boolean readOnly = false;
    private boolean multiple = false;
    private @Nullable Integer multipleLimit;
    private @Nullable String unit;
    private @Nullable String unitLabel;

    private @Nullable String context;
    @SerializedName("default")
    private @Nullable String defaultValue;
    private @Nullable String label;
    private @Nullable String description;

    private List<ParameterOption> options = new ArrayList<>();
    private List<FilterCriteria> filterCriteria = new ArrayList<>();

    private boolean limitToOptions = true;
    private boolean advanced = false;
    private boolean verify = false;

    private static final Set<String> UNITS = Set.of("A", "cd", "K", "kg", "m", "mol", "s", "g", "rad", "sr", "Hz", "N",
            "Pa", "J", "W", "C", "V", "F", "Ω", "S", "Wb", "T", "H", "Cel", "lm", "lx", "Bq", "Gy", "Sv", "kat", "m/s2",
            "m2v", "m3", "kph", "%", "l", "ms", "min", "h", "d", "week", "y");

    /**
     * Default constructor.
     *
     */
    public ConfigDescriptionParameter() {
    }

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param name the name of the configuration parameter (must not be empty)
     * @param type the data type of the configuration parameter
     * @param minimum the minimal value for numeric types, or the minimal length of
     *            strings, or the minimal number of selected options
     * @param maximum the maximal value for numeric types, or the maximal length of
     *            strings, or the maximal number of selected options
     * @param stepsize the value granularity for a numeric value
     * @param pattern the regular expression for a text type
     * @param required specifies whether the value is required
     * @param readOnly specifies whether the value is read-only
     * @param multiple specifies whether multiple selections of options are allowed
     * @param context the context of the configuration parameter (can be empty)
     * @param defaultValue the default value of the configuration parameter
     * @param label a human-readable label for the configuration parameter (can be empty)
     * @param description a human-readable description for the configuration parameter (can be empty)
     * @param filterCriteria a list of filter criteria for values of a dynamic selection list
     * @param options a list of element definitions of a static selection list
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
     * @param unit specifies the unit of measurements for the configuration parameter
     * @param unitLabel specifies the unit label for the configuration parameter. This attribute can also be used to
     *            provide
     *            natural language units as iterations, runs, etc.
     * @param verify specifies whether the parameter should be considered dangerous
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>if the name is null or empty, or the type is null</li>
     *             <li>if a unit or a unit label is provided for a parameter having type text or boolean</li>
     *             <li>if an invalid unit was given (cp.
     *             https://openhab.org/documentation/development/bindings/xml-reference.html for the list
     *             of valid units)</li>
     *             </ul>
     * @deprecated Use {@link ConfigDescriptionParameterBuilder} instead.
     */
    @Deprecated
    ConfigDescriptionParameter(String name, Type type, @Nullable BigDecimal minimum, @Nullable BigDecimal maximum,
            @Nullable BigDecimal stepsize, @Nullable String pattern, @Nullable Boolean required,
            @Nullable Boolean readOnly, @Nullable Boolean multiple, @Nullable String context,
            @Nullable String defaultValue, @Nullable String label, @Nullable String description,
            @Nullable List<ParameterOption> options, @Nullable List<FilterCriteria> filterCriteria,
            @Nullable String groupName, @Nullable Boolean advanced, @Nullable Boolean limitToOptions,
            @Nullable Integer multipleLimit, @Nullable String unit, @Nullable String unitLabel,
            @Nullable Boolean verify) throws IllegalArgumentException {
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
            this.options = List.of();
        }
        if (filterCriteria != null) {
            this.filterCriteria = Collections.unmodifiableList(filterCriteria);
        } else {
            this.filterCriteria = List.of();
        }
    }

    /**
     * Returns the name of the configuration parameter.
     *
     * @return the name of the configuration parameter (not empty)
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the data type of the configuration parameter.
     *
     * @return the data type of the configuration parameter
     */
    public Type getType() {
        return this.type;
    }

    /**
     * @return the minimal value for numeric types, or the minimal length of
     *         strings, or the minimal number of selected options
     */
    public @Nullable BigDecimal getMinimum() {
        return min;
    }

    /**
     * @return the maximal value for numeric types, or the maximal length of
     *         strings, or the maximal number of selected options
     */
    public @Nullable BigDecimal getMaximum() {
        return max;
    }

    /**
     * Returns the value granularity for a numeric value.
     * <p>
     * By setting the step size to <code>0</code>, any granularity is allowed, i.e. any number of decimals is accepted.
     *
     * @return the value granularity for a numeric value
     */
    public @Nullable BigDecimal getStepSize() {
        return step;
    }

    /**
     * @return the regular expression for a text type
     */
    public @Nullable String getPattern() {
        return pattern;
    }

    /**
     * @return true if the value is required, otherwise false.
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * @return true if multiple selections of options are allowed, otherwise
     *         false.
     */
    public boolean isMultiple() {
        return multiple;
    }

    /**
     * @return the maximum number of options that can be selected from the options list
     */
    public @Nullable Integer getMultipleLimit() {
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
     * <ul>
     * <li><b>network-address</b>: The configuration value represents an IPv4 or IPv6 address or domain name.</li>
     * <li><b>network-interface</b>: The configuration value represents a network interface name, e.g. eth0, wlan0.</li>
     * <li><b>serial-port</b>: The configuration value represents a serial port name, e.g. COM1, /dev/ttyS0.</li>
     * <li><b>password</b>: A alphanumeric password value (a user-interface might obscure the visible value)</li>
     * <li><b>password-create</b>: A alphanumeric passwort generator widget might be shown</li>
     * <li><b>color</b>: This value represents an RGB color value like #000000 - #ffffff or 12,12,12.</li>
     * <li><b>date</b>: A date string in the format "YYYY-MM-DD"</li>
     * <li><b>datetime</b>: A date and time string in the format "YYYY-MM-DD'T'hh:mm:ss", e.g.
     * "2019-12-31T23:59:59"</li>
     * <li><b>cronexpression</b>: A cron expression like "* * * * *". A user interface would probably show a cron
     * expression generator.</li>
     * <li><b>email</b>: The configuration value represents an email address, e.g. username@domain.com</li>
     * <li><b>month</b>: A month of year [1-12]</li>
     * <li><b>week</b>: A week of year [0-52]</li>
     * <li><b>dayOfWeek</b>: A day of week [MON, TUE, WED, THU, FRI, SAT, SUN]</li>
     * <li><b>time</b>: A time string in the format "hh:mm:ss" or as milliseconds since epoch</li>
     * <li><b>telephone</b>: A tel no</li>
     * <li><b>url</b>: A web address</li>
     * <li><b>tag</b>: One tag or multiple tags separated by comma.</li>
     * <li><b>item</b>: A valid item "name". A user-interface would probably show an item selection widget.</li>
     * <li><b>thing</b>: A valid thing UID. A user-interface would probably show a thing selection widget.</li>
     * <li><b>group</b>: A valid group item "name". A user-interface would probably show an item selection widget.</li>
     * <li><b>service</b>: A valid service ID. A user-interface would probably show a service selection widget.</li>
     * <li><b>persistenceService</b>: A valid persistence service ID. A user-interface would probably show a persistence
     * service selection widget.</li>
     * <li><b>channel</b>: A valid channel UID. A user-interface would probably show a channel selection widget.</li>
     * <li><b>channeltype</b>: A valid channel type UID. A user-interface would probably show a channel type selection
     * widget.</li>
     * <li><b>rule</b>: A valid rule uid. A user-interface would probably show a rule selection widget.</li>
     * <li><b>script</b>: The configuration value represents a script (javascript, python etc). A user-interface would
     * probably render a multi line editor.</li>
     * <li><b>page</b>: A valid page UID. A user-interface would probably show a page selection widget.</li>
     * <li><b>widget</b>: A valid widget UID. A user-interface would probably show a widget selection widget.</li>
     * <li><b>location</b>: A latitude,longitude[,altitude] GPS location. A user-interface would probably render a world
     * map for selection.</li>
     * </ul>
     *
     * @return the context of the configuration parameter (could be empty)
     */
    public @Nullable String getContext() {
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
        return required;
    }

    /**
     * Returns the default value of the configuration parameter.
     *
     * @return the default value of the configuration parameter
     */
    public @Nullable String getDefault() {
        return defaultValue;
    }

    /**
     * Returns a human-readable label for the configuration parameter.
     *
     * @return a human-readable label for the configuration parameter (could be empty)
     */
    public @Nullable String getLabel() {
        return label;
    }

    /**
     * Returns the group for this configuration parameter.
     *
     * @return a group for the configuration parameter (could be empty)
     */
    public @Nullable String getGroupName() {
        return groupName;
    }

    /**
     * Returns true is the value for this parameter must be limited to the
     * values in the options list.
     *
     * @return true if the value is limited to the options list
     */
    public boolean getLimitToOptions() {
        return limitToOptions;
    }

    /**
     * Returns true is the parameter is considered an advanced option.
     *
     * @return true if the value is an advanced option
     */
    public boolean isAdvanced() {
        return advanced;
    }

    /**
     * Returns a human-readable description for the configuration parameter.
     *
     * @return a human-readable description for the configuration parameter (could be empty)
     */
    public @Nullable String getDescription() {
        return description;
    }

    /**
     * Returns a static selection list for the value of this parameter.
     *
     * @return static selection list for the value of this parameter
     */
    public List<ParameterOption> getOptions() {
        return options;
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
        return filterCriteria;
    }

    /**
     * Returns the unit of measurements of this parameter.
     *
     * @return the unit of measurements of this parameter
     */
    public @Nullable String getUnit() {
        return unit;
    }

    /**
     * Returns the unit label of this parameter.
     *
     * @return the unit label of this parameter
     */
    public @Nullable String getUnitLabel() {
        return unitLabel;
    }

    /**
     * Returns the verify flag for this parameter. Verify parameters are considered dangerous and the user should be
     * alerted with an "Are you sure" flag in the UI.
     *
     * @return true if the parameter requires verification in the UI
     */
    public boolean isVerifyable() {
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
