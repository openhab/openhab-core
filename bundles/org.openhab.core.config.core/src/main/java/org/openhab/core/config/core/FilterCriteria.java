/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

/**
 * The {@link FilterCriteria} specifies a filter for dynamic selection list
 * providers of a {@link ConfigDescriptionParameter}.
 * <p>
 * The {@link FilterCriteria} and its name is related to the context of the containing
 * {@link ConfigDescriptionParameter}.
 *
 * @author Alex Tugarev - Initial contribution
 * @author Markus Rathgeb - Add default constructor for deserialization
 */
public class FilterCriteria {

    private String value;
    private String name;

    /**
     * Default constructor for deserialization e.g. by Gson.
     */
    protected FilterCriteria() {
    }

    public FilterCriteria(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " [name=\"" + name + "\", value=\"" + value + "\"]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FilterCriteria other = (FilterCriteria) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }
}
