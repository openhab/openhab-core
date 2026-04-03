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
package org.openhab.core.voice.text;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link LLMToolParam} class describe each of the parameters of a {@link LLMTool} .
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public final class LLMToolParam {
    private final String name;
    private final LLMToolParamType type;
    private final String description;
    private final List<String> options;
    private final boolean required;

    /**
     *
     */
    public LLMToolParam(String name, LLMToolParamType type, String description, List<String> options,
            boolean required) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.options = options;
        this.required = required;
    }

    public String name() {
        return name;
    }

    public LLMToolParamType type() {
        return type;
    }

    public String description() {
        return description;
    }

    public List<String> options() {
        return options;
    }

    public boolean required() {
        return required;
    }

    @Override
    public String toString() {
        return "LLMToolParam[" + "name=" + name + ", " + "type=" + type + ", " + "description=" + description + ", "
                + "options=" + options + ", " + "required=" + required + ']';
    }
}
