/**
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
package org.openhab.core.model.yaml.test;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlElementName;

/**
 * The {@link SecondTypeDTO} is a test type implementing {@link YamlElement}
 *
 * @author Jan N. Klug - Initial contribution
 */
@YamlElementName("secondType")
public class SecondTypeDTO implements YamlElement {
    public String id;
    public String label;

    public SecondTypeDTO() {
    }

    public SecondTypeDTO(String id, String label) {
        this.id = id;
        this.label = label;
    }

    @Override
    public @NonNull String getId() {
        return id;
    }

    @Override
    public boolean isValid() {
        return id != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SecondTypeDTO that = (SecondTypeDTO) o;
        return Objects.equals(id, that.id) && Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label);
    }
}
