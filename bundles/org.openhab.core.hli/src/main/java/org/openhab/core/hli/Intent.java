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
package org.openhab.core.hli;

import java.util.HashMap;
import java.util.Map;

/**
 * An Intent consists of an identifier of the type of intent, and one or several entities.
 * It is the result of the categorization (for the intent name) and token extraction (for the entities) performed by
 * the OpenNLP interpreter.
 *
 * @author Yannick Schaus - Initial contribution
 */
public class Intent {
    private String name;

    private Map<String, String> entities;

    /**
     * Gets the intent's name (identifier of the type of intent)
     *
     * @return the intent's name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the intent's entities
     *
     * @return the map of entities
     */
    public Map<String, String> getEntities() {
        return entities;
    }

    /**
     * Sets the intent's entities
     *
     * @param entities the map of entities
     */
    public void setEntities(Map<String, String> entities) {
        this.entities = entities;
    }

    @Override
    public String toString() {
        return "Intent [name=" + name + ", entities=" + entities + "]";
    }

    /**
     * Constructs an intent with the specified name
     *
     * @param name the name (ie. type identifier) of the intent
     */
    public Intent(String name) {
        this.name = name;
        this.entities = new HashMap<>();
    }
}
