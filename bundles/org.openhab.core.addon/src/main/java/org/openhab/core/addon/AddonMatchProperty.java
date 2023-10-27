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
package org.openhab.core.addon;

import java.util.Objects;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for serialization of a property match regular expression.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class AddonMatchProperty {
    private @NonNullByDefault({}) String name;
    private @NonNullByDefault({}) String regex;
    private transient @NonNullByDefault({}) Pattern pattern;

    public AddonMatchProperty(String name, String regex) {
        this.name = name;
        this.regex = regex;
        this.pattern = Pattern.compile(regex);
    }

    public String getName() {
        return name;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public String getRegex() {
        return regex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, regex);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AddonMatchProperty other = (AddonMatchProperty) obj;
        return Objects.equals(name, other.name) && Objects.equals(regex, other.regex);
    }
}
