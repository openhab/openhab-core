/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.binding.events;

/**
 * Base BindingEvent data transfer object
 *
 * This can be extended by bindings to add binding specific information, but provides basic information to allow the UI
 * to render a message the user without having to implement any binding specific processor.
 *
 * @author Chris Jackson - Initial Contribution
 *
 */
public class BindingEventDTO {
    public BindingEventType type;
    public String message;

    /**
     * Constructs a binding event with the minimal required information to be sent to the user
     *
     * @param type the {@link BindingEventType} providing general context and severity
     * @param message the message to be provided to the user
     */
    public BindingEventDTO(BindingEventType type, String message) {
        this.type = type;
        this.message = message;
    }
}
