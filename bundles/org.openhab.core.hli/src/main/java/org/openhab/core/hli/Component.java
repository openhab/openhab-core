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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Component represents a piece of UI element that will be passed as part of a @link {@link Card} via the REST API to
 * a client frontend to render; it is kept very simple and delegates the actual rendering and behavior to the frontend.
 *
 * It has a reference to a component's name as defined by the frontend, a map of configuration parameters, and several
 * named "slots", or placeholders, which may contain other sub-components, thus defining a tree.
 *
 * No checks are performed on the actual validity of configuration parameters and their values, the validity of a
 * particular slot for a certain component or the validity of certain types of sub-components within a particular slot:
 * that is the frontend's responsibility.
 *
 * @author Yannick Schaus - Initial contribution
 */
public class Component {
    String component;

    Map<String, Object> config;

    Map<String, List<Component>> slots = null;

    /**
     * Constructs a component by its type name - component names are not arbitrary, they are defined by the target
     * frontend.
     *
     * @param componentName name of the component as known to the frontend
     */
    public Component(String componentName) {
        this.component = componentName;
        this.config = new HashMap<>();
    }

    /**
     * Retrieves the name of the component.
     *
     * @return the component name
     */
    public String getName() {
        return component;
    }

    /**
     * Gets all the configuration parameters of the component
     *
     * @return the map of configuration parameters
     */
    public Map<String, Object> getConfig() {
        return config;
    }

    /**
     * Adds a new configuration parameter to the component
     *
     * @param key the parameter key
     * @param value the parameter value
     */
    public void addConfig(String key, Object value) {
        this.config.put(key, value);
    }

    /**
     * Returns all the slots of the components including their sub-components
     *
     * @return the slots and their sub-components
     */
    public Map<String, List<Component>> getSlots() {
        return slots;
    }

    /**
     * Adds a new empty slot to the component
     *
     * @param slotName the name of the slot
     * @return the empty list of components in the newly created slot
     */
    public List<Component> addSlot(String slotName) {
        if (slots == null) {
            slots = new HashMap<>();
        }
        List<Component> newSlot = new ArrayList<>();
        this.slots.put(slotName, newSlot);

        return newSlot;
    }

    /**
     * Gets the list of sub-components in a slot
     *
     * @param slotName the name of the slot
     * @return the list of sub-components in the slot
     */
    public List<Component> getSlot(String slotName) {
        return this.slots.get(slotName);
    }

    /**
     * Add a new sub-component to the specified slot. Creates the slot if necessary.
     *
     * @param slotName the slot to add the component to
     * @param subComponent the sub-component to add
     */
    public void addComponent(String slotName, Component subComponent) {
        List<Component> slot;
        if (slots == null || !slots.containsKey(slotName)) {
            slot = addSlot(slotName);
        } else {
            slot = getSlot(slotName);
        }

        slot.add(subComponent);
    }
}
