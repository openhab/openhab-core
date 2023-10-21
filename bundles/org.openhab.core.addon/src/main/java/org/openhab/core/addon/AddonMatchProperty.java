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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * DTO for serialization of a property match regular expression.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class AddonMatchProperty {
    private @NonNullByDefault({}) String name;
    private @NonNullByDefault({}) String regex;

    public AddonMatchProperty(String name, String regex) {
        this.name = name;
        this.regex = regex;
    }

    public String getName() {
        return name;
    }

    public String getRegex() {
        return regex;
    }
}
