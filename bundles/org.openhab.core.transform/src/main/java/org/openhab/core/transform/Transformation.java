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
package org.openhab.core.transform;

import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Identifiable;

/**
 * The {@link Transformation} encapsulates a transformation configuration
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class Transformation implements Identifiable<String> {
    public static final String FUNCTION = "function";

    private final String uid;
    private final String label;
    private final String type;
    private final Map<String, String> configuration;

    /**
     * @param uid the configuration UID. The format is config:&lt;type&gt;:&lt;name&gt;[:&lt;locale&gt;]. For backward
     *            compatibility also filenames are allowed.
     * @param type the type of the configuration (file extension for file-based providers)
     * @param configuration the configuration (containing e.g. the transformation function)
     */
    public Transformation(String uid, String label, String type, Map<String, String> configuration) {
        this.uid = uid;
        this.label = label;
        this.type = type;
        this.configuration = configuration;
    }

    @Override
    public String getUID() {
        return uid;
    }

    public String getLabel() {
        return label;
    }

    public String getType() {
        return type;
    }

    public Map<String, String> getConfiguration() {
        return configuration;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Transformation that = (Transformation) o;
        return uid.equals(that.uid) && label.equals(that.label) && type.equals(that.type)
                && configuration.equals(that.configuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, label, type, configuration);
    }

    @Override
    public String toString() {
        return "TransformationConfiguration{uid='" + uid + "', label='" + label + "', type='" + type
                + "', configuration='" + configuration + "'}";
    }
}
