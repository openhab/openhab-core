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
package org.openhab.core.config.discovery.addon.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for serialization of a property match regular expression.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class MatchProperty {
    private @Nullable String name;
    private @Nullable String regex;

    public MatchProperty(String name, String regex) {
        this.name = name;
        this.regex = regex;
    }

    public String getName() {
        String name = this.name;
        return name != null ? name : "";
    }

    public String getRegex() {
        String regex = this.regex;
        return regex != null ? regex : "";
    }
}
