/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.test;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.model.yaml.YamlDTO;

/**
 * The {@link FirstTypeDTO} is a test type implementing {@link YamlDTO}
 *
 * @author Jan N. Klug - Initial contribution
 */
public class FirstTypeDTO implements YamlDTO {
    public String uid;
    public String description;

    public FirstTypeDTO() {
    }

    public FirstTypeDTO(String uid, String description) {
        this.uid = uid;
        this.description = description;
    }

    @Override
    public @NonNull String getId() {
        return uid;
    }

    @Override
    public boolean isValid() {
        return uid != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        FirstTypeDTO that = (FirstTypeDTO) o;
        return Objects.equals(uid, that.uid) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, description);
    }
}