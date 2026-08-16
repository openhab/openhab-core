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
package org.openhab.core.model.yaml.internal.sitemaps;

import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.internal.util.YamlElementUtils;

/**
 * This is a data transfer object that is used to serialize sitemap command mappings.
 *
 * @author Laurent Garnier - Initial contribution
 */
public class YamlMappingDTO {

    public String command;
    public String releaseCommand;
    public String label;
    public String icon;

    public YamlMappingDTO() {
    }

    public boolean isValid(@NonNull List<@NonNull String> errors, @NonNull List<@NonNull String> warnings) {
        boolean ok = true;
        if (command == null) {
            addToList(errors, "\"command\" field missing while mandatory in mappings definition");
            ok = false;
        }
        if (label == null) {
            addToList(errors, "\"label\" field missing while mandatory in mappings definition");
            ok = false;
        }
        if (icon != null && !YamlElementUtils.isValidIcon(icon)) {
            addToList(errors,
                    "invalid value \"%s\" for \"icon\" field in mappings definition; it must contain a maximum of 3 segments separated by a colon, each segment matching pattern [a-zA-Z0-9_][a-zA-Z0-9_-]*"
                            .formatted(icon));
            ok = false;
        }
        return ok;
    }

    private void addToList(@Nullable List<@NonNull String> list, String value) {
        if (list != null) {
            list.add(value);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(command, releaseCommand, label, icon);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        YamlMappingDTO other = (YamlMappingDTO) obj;
        return Objects.equals(command, other.command) && Objects.equals(releaseCommand, other.releaseCommand)
                && Objects.equals(label, other.label) && Objects.equals(icon, other.icon);
    }
}
