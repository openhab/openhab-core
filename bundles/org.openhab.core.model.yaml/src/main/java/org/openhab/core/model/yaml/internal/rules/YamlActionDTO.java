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
package org.openhab.core.model.yaml.internal.rules;

import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.converter.RuleSerializer.RuleSerializationOption;

/**
 * The {@link YamlActionDTO} is a data transfer object used to serialize an action in a YAML configuration file.
 *
 * @author Ravi Nadahar - Initial contribution
 */
public class YamlActionDTO extends YamlModuleDTO {

    public Map<@NonNull String, @NonNull String> inputs;

    public YamlActionDTO() {
    }

    public YamlActionDTO(@NonNull Action action) {
        this(action, RuleSerializationOption.NORMAL);
    }

    public YamlActionDTO(@NonNull Action action, RuleSerializationOption option) {
        super(action, option);
        this.inputs = action.getInputs();
        if (option != RuleSerializationOption.INCLUDE_ALL && this.inputs.isEmpty()) {
            this.inputs = null;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(inputs);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof YamlActionDTO)) {
            return false;
        }
        YamlActionDTO other = (YamlActionDTO) obj;
        return Objects.equals(inputs, other.inputs);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());
        builder.append(" [");
        if (id != null) {
            builder.append("id=").append(id);
        }
        if (inputs != null) {
            builder.append(", inputs=").append(inputs);
        }
        if (type != null) {
            builder.append(", type=").append(type);
        }
        if (label != null) {
            builder.append(", label=").append(label);
        }
        if (description != null) {
            builder.append(", description=").append(description);
        }
        if (config != null) {
            builder.append(", config=").append(config);
        }
        builder.append("]");
        return builder.toString();
    }
}
